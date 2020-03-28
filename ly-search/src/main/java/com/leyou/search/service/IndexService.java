package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.utils.NumberUtils;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-26 15:58
 **/
@Service
public class IndexService {

    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu) {

        // 查询商品分类
        List<String> names = categoryClient.queryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3())).stream()
                .map(Category::getName).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryById(spu.getBrandId());
        // 搜索过滤的字段
        String all = spu.getTitle() + "/" +
                StringUtils.join(names, "/") + "/" + brand.getName();

        // 查询sku
        List<Sku> skuList = goodsClient.querySkuBySpuId(spu.getId());
        // sku的价格的集合
        Set<Long> prices = new TreeSet<>();
        // 设置sku集合的JSON格式
        List<Map<String, Object>> skus = new ArrayList<>();

        for (Sku sku : skuList) {
            prices.add(sku.getPrice());
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("image",
                    StringUtils.isBlank(sku.getImages())
                            ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            map.put("price", sku.getPrice());
            map.put("title", sku.getTitle());
            skus.add(map);
        }

        // 查询规格参数的key
        List<SpecParam> specParams = specClient.queryParams(null, spu.getCid3(), null, true);
        // 查询spuDetail
        SpuDetail detail = goodsClient.queryDetailBySpuId(spu.getId());
        // 通用规格参数值
        Map<Long, Object> genericSpec = JsonUtils.parseMap(detail.getGenericSpec(), Long.class, Object.class);
        // 特有规格参数值
        Map<Long, List<Object>> specialSpec = JsonUtils.nativeRead(detail.getSpecialSpec(), new TypeReference<Map<Long, List<Object>>>() {
        });

        // 可搜索的规格参数键值对
        Map<String, Object> specs = new HashMap<>();
        for (SpecParam param : specParams) {
            Long id = param.getId();
            String name = param.getName();
            Object value = null;
            if (param.getGeneric()) {
                // 通用参数
                value = genericSpec.get(id);
                // 判断是否为数值类型
                if (param.getNumeric()) {
                    value = chooseSegment(value.toString(), param);
                }
            } else {
                // 特有参数
                value = specialSpec.get(id);
            }
            if (value == null) {
                value = "其它";
            }
            specs.put(name, value);
        }

        Goods goods = new Goods();
        goods.setId(spu.getId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setSubTitle(spu.getSubTitle());
        goods.setSkus(JsonUtils.serialize(skus));
        goods.setPrice(new ArrayList<>(prices));
        goods.setSpecs(specs);
        goods.setAll(all);

        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest request) {
        String key = request.getKey();
        if (StringUtils.isBlank(key)) {
            return null;
        }
        // 构建本地查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 1、分页
        Integer page = request.getPage() - 1;
        Integer size = request.getSize();
        queryBuilder.withPageable(PageRequest.of(page, size));
        // 2、搜索条件过滤
        QueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);
        // 对搜索结果字段过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));

        // 3、聚合
        String categoryAggName = "categoryAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        String brandAggName = "brandAgg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        // 4、搜索
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        // 5、解析结果
        // 5.1、解析分页结果
        long total = result.getTotalElements();//总数据
        long totalPage = (total + size - 1) / size;
        List<Goods> list = result.getContent();

        // 5.2、解析聚合结果
        Aggregations aggs = result.getAggregations();
        // 处理分类聚合
        List<Category> categories = handleCategoryAgg(aggs.get(categoryAggName));
        // 处理品牌的聚合
        List<Brand> brands = handleBrandAgg(aggs.get(brandAggName));

        // 规格参数结果
        List<Map<String, Object>> specs = null;

        if (categories.size() == 1) {
            specs = buildSpecs(categories.get(0).getId(), basicQuery);
        }
        return new SearchResult(total, totalPage, list, categories, brands, specs);
    }

    // 构建基本查询和过滤
    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 创建布尔查询，包含搜索和过滤条件
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 搜索条件
        queryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).minimumShouldMatch("75%"));
        // 过滤条件
        Map<String, String> filter = request.getFilter();
        // 创建布尔查询，用来封装多个过滤条件
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            if (!"cid3".equals(key) && !"brandId".equals(key)) {
                key = "specs." + key + ".keyword";
            }
            String value = entry.getValue();
            filterQuery.must(QueryBuilders.termQuery(key, value));
        }

        queryBuilder.filter(filterQuery);
        return queryBuilder;
    }

    // 构建规格参数结果
    private List<Map<String, Object>> buildSpecs(Long cid, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 1、根据分类查询所有可搜索的规格参数
        List<SpecParam> params = specClient.queryParams(null, cid, null, true);
        // 2、对规格参数进行聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 基本搜索条件
        queryBuilder.withQuery(basicQuery);
        // 设置为最小搜索结果
        queryBuilder.withPageable(PageRequest.of(0, 1));

        // 聚合
        for (SpecParam param : params) {
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }

        // 搜索
        Aggregations aggs = template.queryForPage(queryBuilder.build(), Goods.class).getAggregations();
        // 3、解析聚合结果
        for (SpecParam param : params) {
            String name = param.getName();
            StringTerms terms = aggs.get(name);
            List<String> options = terms.getBuckets().stream()
                    .map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
            Map<String, Object> map = new HashMap<>();
            map.put("k", name);
            map.put("options", options);
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> handleBrandAgg(LongTerms terms) {
        // 从桶中取出id
        List<Long> ids = terms.getBuckets().stream()
                .map(bucket -> bucket.getKeyAsNumber().longValue())
                .collect(Collectors.toList());
        // 根据id查询品牌的集合
        return brandClient.queryByIds(ids);
    }

    private List<Category> handleCategoryAgg(LongTerms terms) {
        // 从桶中取出id
        List<Long> ids = terms.getBuckets().stream()
                .map(bucket -> bucket.getKeyAsNumber().longValue())
                .collect(Collectors.toList());
        // 根据id查询品牌的集合
        return categoryClient.queryByIds(ids);
    }

    public void insertOrUpdate(Long id) {
        Spu spu = goodsClient.querySpuById(id);
        Goods goods = buildGoods(spu);
        goodsRepository.save(goods);
    }

    public void delete(Long id) {
        goodsRepository.deleteById(id);
    }
}

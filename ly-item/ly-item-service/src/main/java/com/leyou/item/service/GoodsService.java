package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-22 18:12
 **/
@Service
public class GoodsService {
    private static final Logger logger = LoggerFactory.getLogger(GoodsService.class);

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    // TODO 未来希望改造成全文检索实现
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, String key, Boolean saleable) {
        // 分页
        PageHelper.startPage(Math.max(1, page), Math.max(Math.min(rows, 100), 5));
        // 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();

        // 1、过滤key
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        // 2、过滤saleable
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        // 3、过滤是否有效
        criteria.andEqualTo("valid", true);
        // 查询
        List<Spu> list = spuMapper.selectByExample(example);
        // 处理分类和品牌的名称
        handleCategoryAndBrandNames(list);
        // 封装结果，返回
        PageInfo<Spu> info = new PageInfo<>(list);
        return new PageResult<>(info.getTotal(), list);
    }

    private void handleCategoryAndBrandNames(List<Spu> list) {
        for (Spu spu : list) {
            List<String> names = categoryService.queryCategoryByIds(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3())).stream()
                    .map(c -> c.getName()).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names, "/"));

            Brand brand = brandService.queryById(spu.getBrandId());
            spu.setBname(brand.getName());
        }
    }

    @Transactional
    public void saveGoods(Spu spu) {
        try {
            // 新增spu
            spu.setId(null);
            spu.setSaleable(true);
            spu.setValid(true);
            spu.setCreateTime(new Date());
            spu.setLastUpdateTime(spu.getCreateTime());
            int count = spuMapper.insert(spu);
            if (count <= 0) {
                logger.error("新增spu失败，spu:{}", JsonUtils.serialize(spu));
                throw new RuntimeException("新增spu失败");
            }

            // 新增SpuDetail
            SpuDetail spuDetail = spu.getSpuDetail();
            spuDetail.setSpuId(spu.getId());
            count = detailMapper.insert(spuDetail);
            if (count <= 0) {
                logger.error("新增spuDetail失败，spuDetail:{}", JsonUtils.serialize(spuDetail));
                throw new RuntimeException("新增spuDetail失败");
            }
            // 新增库存和sku
            insertSkuAndStock(spu);

            // 发送消息
            amqpTemplate.convertAndSend("item.insert", spu.getId());
        } catch (Exception e) {
            logger.error("新增商品失败", e);
            throw new RuntimeException("新增商品失败", e);
        }
    }



    public SpuDetail queryDetailBySpuId(Long spuId) {
        return detailMapper.selectByPrimaryKey(spuId);
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku t = new Sku();
        t.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(t);
        List<Long> ids = skus.stream().map(sku -> sku.getId()).collect(Collectors.toList());
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        Map<Long, Integer> map = new HashMap<>();
        for (Stock stock : stocks) {
            map.put(stock.getSkuId(), stock.getStock());
        }
        for (Sku sku : skus) {
            sku.setStock(map.get(sku.getId()));
        }
        return skus;
    }

    @Transactional
    public void updateGoods(Spu spu) {
        try {
            // 查询以前的sku
            Sku t = new Sku();
            t.setSpuId(spu.getId());
            List<Sku> skus = skuMapper.select(t);
            if(CollectionUtils.isNotEmpty(skus)) {
                List<Long> ids = skus.stream().map(Sku::getId).collect(Collectors.toList());
                // 删除stock
                stockMapper.deleteByIdList(ids);
                // 删除sku
                skuMapper.delete(t);
            }
            // 新增sku和stock
            insertSkuAndStock(spu);

            // 修改spu
            spu.setLastUpdateTime(new Date());
            spu.setValid(null);
            spu.setCreateTime(null);
            spu.setSaleable(null);
            spuMapper.updateByPrimaryKeySelective(spu);

            // 修改spuDetail
            detailMapper.updateByPrimaryKey(spu.getSpuDetail());

            // 发送消息
            amqpTemplate.convertAndSend("item.update", spu.getId());
        }catch (Exception e){
            logger.error("修改商品异常。", e);
            throw new RuntimeException(e);
        }
    }

    private void insertSkuAndStock(Spu spu) {
        int count;// 新增sku
        List<Sku> skus = spu.getSkus();
        List<Stock> stocks = new ArrayList<>();
        for (Sku sku : skus) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            count = skuMapper.insert(sku);
            if (count != 1) {
                logger.error("新增sku失败，sku:{}", JsonUtils.serialize(sku));
                throw new RuntimeException("新增sku失败");
            }

            // 生成库存对象
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stocks.add(stock);
        }

        count = stockMapper.insertList(stocks);
        if (count < stocks.size()) {
            logger.error("新增stocks失败，stocks:{}", JsonUtils.serialize(stocks));
            throw new RuntimeException("新增stocks失败");
        }
    }

    public Spu querySpuById(Long id) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        // 查询spu下的sku集合
        spu.setSkus(querySkuBySpuId(id));
        // 查询detail
        spu.setSpuDetail(queryDetailBySpuId(id));
        return spu;
    }
}

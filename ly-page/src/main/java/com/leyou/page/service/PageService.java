package com.leyou.page.service;

import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.Spu;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-31 16:58
 **/
@Service
public class PageService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private TemplateEngine templateEngine;

    private static final ExecutorService es = Executors.newFixedThreadPool(20);

    @Value("${ly.page.filePath}")
    private String destPath;

    public Map<String, Object> loadModel(Long spuId) {
        Map<String, Object> map = new HashMap<>();
        try {
            // 1、查询spu
            Spu spu = goodsClient.querySpuById(spuId);
            map.put("spu", spu);
            // 2、查询sku
            map.put("skus", spu.getSkus());
            // 3、查询detail
            map.put("detail", spu.getSpuDetail());
            // 4、查询三级分类
            List<Category> categories = categoryClient.queryByIds(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            map.put("categories", categories);
            // 5、查询品牌
            Brand brand = brandClient.queryById(spu.getBrandId());
            map.put("brand", brand);
            // 6、查询规格参数
            List<SpecGroup> specs = specClient.querySpecsByCid(spu.getCid3());
            map.put("specs", specs);
            return map;
        }catch (Exception e){
            throw new RuntimeException("加载数据失败！", e);
        }
    }

    public void createHtml(Long id){
        File dest = getFilePath(id);
        File bak = new File(id + "_bak.html");
        // 准备文件关联的输出流
        try (PrintWriter writer = new PrintWriter(dest, "UTF-8")){
            // 准备数据
            Map<String, Object> model = loadModel(id);
            // 准备Context
            Context context = new Context();
            context.setVariables(model);

            if(dest.exists()){
                dest.renameTo(bak);
            }

            // 生成文件
            templateEngine.process("item", context, writer);
        }catch (Exception e){
            bak.renameTo(dest);
            throw new RuntimeException(e);
        } finally {
            bak.deleteOnExit();
        }
    }

    private File getFilePath(Long id) {
        File dir = new File(destPath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        return new File(dir, id + ".html");
    }

    public void syncCreateHtml(Long id){
        es.submit(() -> createHtml(id));
    }

    public void deleteHtml(Long id) {
        File file = getFilePath(id);
        file.deleteOnExit();
    }
}

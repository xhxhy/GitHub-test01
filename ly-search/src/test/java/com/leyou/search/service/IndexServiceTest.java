package com.leyou.search.service;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Spu;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class IndexServiceTest {

    @Autowired
    private IndexService indexService;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private GoodsClient goodsClient;

    @Test
    public void buildGoods() {
//        template.createIndex(Goods.class);
//        template.putMapping(Goods.class);

        int page = 1;
        int rows = 100;
        int size = 0;
        do {

            // 批量查询spu
            PageResult<Spu> result = goodsClient.querySpuByPage(page, rows, null, true);

            List<Spu> spus = result.getItems();

            // goods的集合
            List<Goods> list = new ArrayList<>();
            for (Spu spu : spus) {
                Goods goods = indexService.buildGoods(spu);
                list.add(goods);
            }
            goodsRepository.saveAll(list);
            page++;
            size = spus.size();
        } while (size == 100);
    }
}
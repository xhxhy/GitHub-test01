package com.leyou.search.repository;

import com.leyou.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-26 16:51
 **/
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long>{
}

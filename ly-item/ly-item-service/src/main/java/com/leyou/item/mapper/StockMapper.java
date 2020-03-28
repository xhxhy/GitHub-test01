package com.leyou.item.mapper;

import com.leyou.item.pojo.Stock;
import tk.mybatis.mapper.additional.idlist.IdListMapper;
import tk.mybatis.mapper.additional.insert.InsertListMapper;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-23 16:41
 **/
public interface StockMapper extends Mapper<Stock>, InsertListMapper<Stock>, IdListMapper<Stock, Long> {
}

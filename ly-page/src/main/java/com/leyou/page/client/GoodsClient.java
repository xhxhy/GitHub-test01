package com.leyou.page.client;

import com.leyou.item.api.GoodsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-26 15:56
 **/
@FeignClient("item-service")
public interface GoodsClient extends GoodsApi {
}

package com.leyou.page.client;

import com.leyou.item.api.CategoryApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-26 15:36
 **/
@FeignClient(value = "item-service")
public interface CategoryClient extends CategoryApi{
}
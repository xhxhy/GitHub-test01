package com.leyou.search.client;

import com.leyou.item.api.CategoryApi;
import com.leyou.item.pojo.Category;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-26 15:36
 **/
@FeignClient(value = "item-service")
public interface CategoryClient extends CategoryApi{
}
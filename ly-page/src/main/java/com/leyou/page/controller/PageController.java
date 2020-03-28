package com.leyou.page.controller;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-07-31 16:10
 **/
@Controller
public class PageController {

    @Autowired
    private PageService pageService;

    @GetMapping("item/{spuId}.html")
    public String toGoodsPage(@PathVariable("spuId") Long spuId, Model model){
        // 准备模型数据
        Map<String,Object> data = pageService.loadModel(spuId);
        model.addAllAttributes(data);

        // 创建html
        pageService.syncCreateHtml(spuId);

        // 返回视图名
        return "item";
    }
}

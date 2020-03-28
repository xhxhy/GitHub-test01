package com.leyou.search.mq;

import com.leyou.search.service.IndexService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-02 14:53
 **/
@Component
public class ItemListener {

    @Autowired
    private IndexService indexService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ly.search.insert.queue", durable = "true"),
            exchange = @Exchange(
                    name = "ly.item.exchange", type = ExchangeTypes.TOPIC,
                    ignoreDeclarationExceptions = "true"),
            key = {"item.insert", "item.update"}
    ))
    public void listenInsert(Long id){
        if(id != null){
            // 新增或修改
            indexService.insertOrUpdate(id);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ly.search.delete.queue", durable = "true"),
            exchange = @Exchange(
                    name = "ly.item.exchange", type = ExchangeTypes.TOPIC,
                    ignoreDeclarationExceptions = "true"),
            key = "item.delete"
    ))
    public void listenDelete(Long id){
        if(id != null){
            // 删除
            indexService.delete(id);
        }
    }
}

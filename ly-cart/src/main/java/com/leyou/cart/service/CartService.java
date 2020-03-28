package com.leyou.cart.service;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-05 18:30
 **/
@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "ly:cart:user:";

    public void addCart(Cart cart) {
        // 获取当前用户
        UserInfo user = UserInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();

        // 获取商品id
        String hashKey = cart.getSkuId().toString();
        // 获取数量
        int num = cart.getNum();

        // 获取hash操作的对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        // 判断要添加的商品是否存在
        if(hashOps.hasKey(hashKey)){
            // 存在，修改数量
            cart = JsonUtils.parse(hashOps.get(hashKey).toString(), Cart.class);
            cart.setNum(num + cart.getNum());
        }else{
            // 不存在，直接新增
            cart.setUserId(user.getId());
        }
        // 写入redis
        hashOps.put(hashKey, JsonUtils.serialize(cart));
    }

    public List<Cart> queryCartList() {
        // 获取当前用户
        UserInfo user = UserInterceptor.getUser();
        String key = KEY_PREFIX + user.getId();
        // 从redis中获取
        if(!redisTemplate.hasKey(key)){
            return null;
        }
        // 存在，则获取
        return redisTemplate.boundHashOps(key).values().stream()
                .map(o -> JsonUtils.parse(o.toString(), Cart.class))
                .collect(Collectors.toList());
    }
}

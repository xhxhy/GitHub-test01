package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-02 16:47
 **/
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Value("${ly.sms.exchangeName}")
    private String exchangeName;

    @Value("${ly.sms.routingKey}")
    private String routingKey;

    private static final String KEY_PREFIX = "user:verify:phone:";

    public Boolean checkData(String data, Integer type) {
        User user = new User();
        switch (type) {
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                return null;
        }
        return userMapper.selectCount(user) <= 0;
    }

    public void sendCode(String phone) {
        String key = KEY_PREFIX + phone;
        // 1、生成验证码
        String code = NumberUtils.generateCode(6);
        // 2、保存验证码到redis
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        // 3、发送短信
        Map<String, String> map = new HashMap<>();
        map.put("phone", phone);
        map.put("code", code);
        amqpTemplate.convertAndSend(exchangeName, routingKey, map);
    }

    public boolean register(User user, String code) {
        // 1、校验code
        String key = KEY_PREFIX + user.getPhone();
        String cacheCode = redisTemplate.opsForValue().get(key);
        if (!StringUtils.equals(cacheCode, code)) {
            return false;
        }
        // 2、对密码加密
        // 生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        // 密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        // 3、保存
        user.setId(null);
        user.setCreated(new Date());
        int count = userMapper.insert(user);
        return count == 1;
    }

    public User queryByUsernameAndPassword(String username, String password) {
        // 查询
        User t = new User();
        t.setUsername(username);
        User user = userMapper.selectOne(t);
        // 校验用户名
        if (user == null) {
            return null;
        }
        // 校验密码
        String dbPassword = CodecUtils.md5Hex(password, user.getSalt());
        if (!StringUtils.equals(dbPassword, user.getPassword())) {
            return null;
        }
        return user;
    }
}

package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-04 17:38
 **/
@Service
@Slf4j
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private UserClient userClient;

    public String accredit(String username, String password) {
        try {
            User user = userClient.queryByUsernameAndPassword(username, password);
            if (user == null) {
                // 用户名或密码错误
                return null;
            }
            // 生成token
            UserInfo userInfo = new UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            String token = JwtUtils.generateToken(userInfo, prop.getPrivateKey(), prop.getExpire());
            return token;
        }catch (Exception e){
            log.error("用户登录异常", e);
            return null;
        }
    }
}

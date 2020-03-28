package com.leyou.cart.interceptor;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-05 18:00
 **/

public class UserInterceptor implements HandlerInterceptor {

    private JwtProperties jwtProp;

    public UserInterceptor(JwtProperties jwtProp) {
        this.jwtProp = jwtProp;
    }

    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取token
        String token = CookieUtils2.getCookieValue(request, jwtProp.getCookieName());
        // 解析token
        try {
            UserInfo user = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey());
            // 保存用户
            tl.set(user);
            return true;
        } catch (Exception e) {
            // token无效
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清空ThreadLocal中的数据
        tl.remove();
    }

    public static UserInfo getUser(){
        return tl.get();
    }
}

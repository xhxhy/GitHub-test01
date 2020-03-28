package com.leyou.zuul.filter;

import com.google.common.util.concurrent.RateLimiter;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.zuul.config.FilterProperties;
import com.leyou.zuul.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-05 15:00
 **/
@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter{

    @Autowired
    private FilterProperties filterProp;
    @Autowired
    private JwtProperties jwtProp;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        // 获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = ctx.getRequest();
        // 获取请求路径
        String requestPath = request.getRequestURI();
        // 返回true：要拦截，返回false：不拦截
        return !isAllowPath(requestPath);
    }

    private boolean isAllowPath(String requestPath) {
        for (String path : filterProp.getAllowPaths()) {
            if(StringUtils.startsWith(requestPath, path)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Object run() throws ZuulException {

        // 获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = ctx.getRequest();
        // 获取token
        String token = CookieUtils.getCookieValue(request, jwtProp.getCookieName());
        // 解析token，得到用户数据
        try {
            JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey());
        } catch (Exception e) {
            // 无效的token，返回401，未授权
            ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
            ctx.setSendZuulResponse(false);
            return null;
        }
        // TODO 查询用户权限
        // TODO 判断是否有权访问路径
        return null;
    }
}

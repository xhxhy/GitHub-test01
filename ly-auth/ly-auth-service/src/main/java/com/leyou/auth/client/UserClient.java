package com.leyou.auth.client;

import com.leyou.user.api.UserApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-04 17:42
 **/
@FeignClient("user-service")
public interface UserClient extends UserApi{
}

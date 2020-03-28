package com.leyou.user.controller;

import com.leyou.user.pojo.User;
import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-02 16:47
 **/
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 校验数据是否可用
     *
     * @param data
     * @param type
     * @return
     */
    @GetMapping("/check/{data}/{type}")
    public ResponseEntity<Boolean> checkData(
            @PathVariable("data") String data,
            @PathVariable(value = "type", required = false) Integer type
    ) {
        if (type == null) {
            type = Integer.valueOf(1);
        }
        Boolean boo = userService.checkData(data, type);
        if (boo == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(boo);
    }

    /**
     * 发送短信验证码
     *
     * @param phone
     * @return
     */
    @PostMapping("/code")
    public ResponseEntity<Void> sendCode(@RequestParam("phone") String phone) {
        if (!phone.matches("^1[35678]\\d{9}$")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        userService.sendCode(phone);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * 注册
     *
     * @param user
     * @param code
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid User user, @RequestParam("code") String code) {
        boolean boo = userService.register(user, code);
        if (!boo) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/query")
    public ResponseEntity<User> queryByUsernameAndPassword(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        User user = userService.queryByUsernameAndPassword(username, password);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(user);
    }
}

package com.zxl.problems.migratespringboot.controller;

import javax.annotation.Resource;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zxl.problems.migratespringboot.service.LoginService;

/**
 * LoginController
 *
 * @author zhanghan
 * @date 2018/12/5
 * @since 1.0
 */
@Controller
public class LoginController {

    private static final String LOGIN_SUCCESS_KEY = "login_success_count";

    @Resource
    private LoginService loginService;

    @Resource
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return "index";
    }

    @RequestMapping(value = "login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<String> login(@RequestParam @NotBlank String username, @RequestParam @NotEmpty char[] token) {
        if (loginService.login(username, token)) {
            long successTimes = redisTemplate.opsForValue().increment(LOGIN_SUCCESS_KEY, 1L);
            return new ResponseEntity<>("第" + successTimes + "次登录", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
    }
}

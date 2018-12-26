package com.zxl.problems.migratespringboot.interceptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * SimpleRequestLimitInterceptor
 *
 * @author zhanghan
 * @date 2018/12/5
 * @since 1.0
 */
public class SimpleRequestLimitInterceptor extends HandlerInterceptorAdapter {

    private static final long REQUEST_LIMIT = 200000;
    private final Cache<String, AtomicLong> cache = CacheBuilder.newBuilder().concurrencyLevel(20)
            .expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2000).build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String key = request.getRemoteAddr();
        AtomicLong requestTimes = cache.get(key, () -> new AtomicLong(0));
        if (requestTimes.incrementAndGet() > REQUEST_LIMIT) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "too many requests");
            return false;
        }
        return true;
    }
}

package com.aurora.aspect;

import com.aurora.annotation.AccessLimit;
import com.aurora.common.RedisConstants;
import com.aurora.exception.BusinessException;
import com.aurora.utils.IpUtils;
import com.aurora.starter.redis.core.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @author: quequnlong
 * @date: 2024/12/28
 * @description: 限流切面处理
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AccessLimitAspect {

    private final RedisRateLimiter redisRateLimiter;

    @Before("@annotation(accessLimit)")
    public void doBefore(JoinPoint joinPoint, AccessLimit accessLimit) {
        int time = accessLimit.time();
        int count = accessLimit.count();

        HttpServletRequest request = IpUtils.getRequest();
        // 拼接redis key = IP + Api限流
        String key = RedisConstants.RATE_LIMIT_KEY + IpUtils.getIp() + request.getRequestURI();

        // 尝试获取限流许可；失败表示请求过于频繁
        if (!redisRateLimiter.tryRateLimit(key, count, time, () -> {})) {
            log.info("API请求限流拦截启动,{} 请求过于频繁", key);
            throw new BusinessException("请求过于频繁,稍后重试");
        }
    }
}

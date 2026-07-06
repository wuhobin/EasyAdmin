package com.aurora.aspect;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.aurora.annotation.OperationLogger;
import com.aurora.common.Constants;
import com.aurora.dto.user.LoginUserInfo;
import com.aurora.entity.SysOperateLog;
import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.mapper.SysOperateLogMapper;
import com.aurora.utils.AspectUtils;
import com.aurora.utils.DateUtils;
import com.aurora.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;

/**
 * 日志切面
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLoggerAspect.class);

    private final SysOperateLogMapper operateLogMapper;

    /**
     * 开始时间
     */
    Date startTime;

    @Pointcut(value = "@annotation(operationLogger)")
    public void pointcut(OperationLogger operationLogger) {

    }

    @Around(value = "pointcut(operationLogger)")
    public Object doAround(ProceedingJoinPoint joinPoint, OperationLogger operationLogger) throws Throwable {
        HttpServletRequest request = IpUtils.getRequest();
        StpUtil.checkLogin();
        //因给了演示账号所有权限以供用户观看，所以执行业务前需判断是否是管理员操作
        if  (!StpUtil.hasRole(Constants.ADMIN)) {
            throw new NotPermissionException("无权限");
        }
        startTime = DateUtils.getNowDate();

        //先执行业务
        Object result = joinPoint.proceed();
        try {
            // 日志收集
            handle(joinPoint, request);

        } catch (Exception e) {
            logger.error("日志记录出错!", e);
        }

        return result;
    }

    /**
     * 管理员日志收集
     *
     * @param point
     * @throws Exception
     */
    private void handle(ProceedingJoinPoint point, HttpServletRequest request) throws Exception {

        Method currentMethod = AspectUtils.INSTANCE.getMethod(point);

        //获取操作名称
        OperationLogger annotation = currentMethod.getAnnotation(OperationLogger.class);

        boolean save = annotation.save();

        String operationName = AspectUtils.INSTANCE.parseParams(point.getArgs(), annotation.value());
        if (!save) {
            return;
        }
        // 获取参数名称字符串
        String paramsJson = getParamsJson(point);

        // 当前操作用户
        String userJson = JsonUtil.toJson(StpUtil.getSession().get(Constants.CURRENT_USER));
        LoginUserInfo user = JsonUtil.parse(userJson, LoginUserInfo.class);
        String type = request.getMethod();
        String ip = IpUtils.getIp();
        String url = request.getRequestURI();

        // 存储日志
        Date endTime = new Date();
        Long spendTime = endTime.getTime() - startTime.getTime();

        SysOperateLog operateLog = SysOperateLog.builder()
                .ip(ip)
                .source(IpUtils.getIp2region(ip))
                .type(type)
                .username(user.getUsername())
                .paramsJson(paramsJson)
                .requestUrl(url)
                .spendTime(spendTime)
                .methodName(point.getSignature().getName())
                .classPath(point.getTarget().getClass().getName())
                .operationName(operationName).build();

        operateLogMapper.insert(operateLog);
    }

    private String getParamsJson(ProceedingJoinPoint joinPoint) throws ClassNotFoundException, NoSuchMethodException {
        // 参数值
        Object[] args = joinPoint.getArgs();
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        String[] parameterNames = methodSignature.getParameterNames();

        // 通过map封装参数和参数值
        HashMap<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < parameterNames.length; i++) {
            paramMap.put(parameterNames[i], args[i]);
        }

        boolean isContains = paramMap.containsKey("request");
        if (isContains) paramMap.remove("request");
        return JSONUtil.toJsonStr(paramMap);
    }
}

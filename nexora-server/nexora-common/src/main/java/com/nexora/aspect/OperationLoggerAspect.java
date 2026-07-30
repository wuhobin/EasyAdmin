package com.nexora.aspect;

import com.nexora.annotation.OperationLogger;
import com.nexora.constants.CommonConstants;
import com.nexora.domain.vo.auth.LoginUserInfoVo;
import com.nexora.entity.SysOperateLog;
import com.nexora.mapper.SysOperateLogMapper;
import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.utils.ServletUtils;
import com.nexora.utils.IpRegionUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Records operations after the business method succeeds.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLoggerAspect.class);
    private static final Pattern ARGUMENT_PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    private final SysOperateLogMapper operateLogMapper;

    @Pointcut(value = "@annotation(operationLogger)")
    public void pointcut(OperationLogger operationLogger) {
    }

    @Around(value = "pointcut(operationLogger)")
    public Object doAround(ProceedingJoinPoint joinPoint, OperationLogger operationLogger) throws Throwable {
        HttpServletRequest request = ServletUtils.getRequest();
        SecurityUtils.checkLogin();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        try {
            if (operationLogger.save()) {
                saveOperationLog(joinPoint, operationLogger, request, startTime);
            }
        } catch (Exception e) {
            logger.error("操作日志记录失败", e);
        }
        return result;
    }

    private void saveOperationLog(ProceedingJoinPoint point, OperationLogger annotation,
                                  HttpServletRequest request, long startTime) {
        String operationName = formatOperationName(annotation.value(), point.getArgs());
        MethodSignature signature = (MethodSignature) point.getSignature();
        String paramsJson = serializeParameters(signature.getParameterNames(), point.getArgs());

        String userJson = JsonUtil.toJson(SecurityUtils.getSessionAttribute(CommonConstants.CURRENT_USER));
        LoginUserInfoVo user = JsonUtil.parse(userJson, LoginUserInfoVo.class);
        String ip = ServletUtils.getClientIp(request);

        SysOperateLog operateLog = SysOperateLog.builder()
                .ip(ip)
                .source(IpRegionUtils.resolve(ip))
                .type(request == null ? "" : request.getMethod())
                .userId(user == null ? null : user.getId())
                .paramsJson(paramsJson)
                .requestUrl(request == null ? "" : request.getRequestURI())
                .spendTime(System.currentTimeMillis() - startTime)
                .methodName(point.getSignature().getName())
                .classPath(point.getTarget().getClass().getName())
                .operationName(operationName)
                .build();

        operateLogMapper.insert(operateLog);
    }

    static String formatOperationName(String template, Object[] args) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Object[] safeArgs = args == null ? new Object[0] : args;
        Matcher matcher = ARGUMENT_PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) - 1;
            String replacement = index >= 0 && index < safeArgs.length
                    ? JsonUtil.toJson(safeArgs[index])
                    : matcher.group();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static String serializeParameters(String[] parameterNames, Object[] args) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (parameterNames == null || args == null) {
            return JsonUtil.toJson(parameters);
        }
        int length = Math.min(parameterNames.length, args.length);
        for (int i = 0; i < length; i++) {
            if (!isUnloggableArgument(args[i])) {
                parameters.put(parameterNames[i], args[i]);
            }
        }
        return JsonUtil.toJson(parameters);
    }

    private static boolean isUnloggableArgument(Object argument) {
        return argument instanceof ServletRequest
                || argument instanceof ServletResponse
                || argument instanceof MultipartFile
                || argument instanceof InputStream
                || argument instanceof OutputStream;
    }
}

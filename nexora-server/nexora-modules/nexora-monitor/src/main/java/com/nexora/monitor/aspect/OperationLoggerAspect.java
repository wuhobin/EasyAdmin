package com.nexora.monitor.aspect;

import com.nexora.annotation.OperationLogger;
import com.nexora.monitor.entity.SysOperateLog;
import com.nexora.monitor.mapper.SysOperateLogMapper;
import com.aurora.starter.common.utils.JsonUtil;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.utils.ServletUtils;
import com.nexora.monitor.infrastructure.IpRegionUtils;
import com.nexora.monitor.infrastructure.OperationLogContext;
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
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            SecurityUtils.checkLogin();
            int currentUserId = SecurityUtils.getLoginIdAsInt();
            RequestAuditSnapshot requestSnapshot = new RequestAuditSnapshot(
                    currentUserId > 0 ? currentUserId : null,
                    request == null ? "" : ServletUtils.getClientIp(request),
                    request == null ? "" : request.getRequestURI(),
                    request == null ? "" : request.getMethod(),
                    System.currentTimeMillis());

            Object result = joinPoint.proceed();
            try {
                if (operationLogger.save()) {
                    saveOperationLog(joinPoint, operationLogger, requestSnapshot);
                }
            } catch (Exception e) {
                logger.error("操作日志记录失败", e);
            }
            return result;
        } finally {
            OperationLogContext.clear();
        }
    }

    private void saveOperationLog(ProceedingJoinPoint point, OperationLogger annotation,
                                  RequestAuditSnapshot requestSnapshot) {
        String operationName = formatOperationName(annotation.value(), point.getArgs());
        MethodSignature signature = (MethodSignature) point.getSignature();
        Map<String, Object> parameters = collectParameters(
                signature.getParameterNames(), point.getArgs());
        parameters.putAll(OperationLogContext.parameters());
        String paramsJson = JsonUtil.toJson(parameters);

        SysOperateLog operateLog = SysOperateLog.builder()
                .ip(requestSnapshot.ip())
                .source(IpRegionUtils.resolve(requestSnapshot.ip()))
                .type(requestSnapshot.method())
                .userId(requestSnapshot.operatorUserId())
                .paramsJson(paramsJson)
                .requestUrl(requestSnapshot.requestUrl())
                .spendTime(System.currentTimeMillis() - requestSnapshot.startTime())
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
        return JsonUtil.toJson(collectParameters(parameterNames, args));
    }

    private static Map<String, Object> collectParameters(
            String[] parameterNames,
            Object[] args) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (parameterNames == null || args == null) {
            return parameters;
        }
        int length = Math.min(parameterNames.length, args.length);
        for (int i = 0; i < length; i++) {
            if (!isUnloggableArgument(args[i])) {
                parameters.put(parameterNames[i], args[i]);
            }
        }
        return parameters;
    }

    private static boolean isUnloggableArgument(Object argument) {
        return argument instanceof ServletRequest
                || argument instanceof ServletResponse
                || argument instanceof MultipartFile
                || argument instanceof InputStream
                || argument instanceof OutputStream;
    }

    private record RequestAuditSnapshot(
            Integer operatorUserId,
            String ip,
            String requestUrl,
            String method,
            long startTime) {
    }
}

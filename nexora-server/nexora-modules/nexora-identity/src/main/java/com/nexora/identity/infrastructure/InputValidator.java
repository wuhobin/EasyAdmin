package com.nexora.identity.infrastructure;

import com.aurora.starter.webmvc.exception.BizException;

/**
 * 常用输入校验工具方法。
 */
public final class InputValidator {

    private InputValidator() {
        // utility class
    }

    /**
     * 校验非空字符串，返回 trim 后的值。
     */
    public static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(message);
        }
        return value.trim();
    }
}

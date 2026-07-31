package com.nexora.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum SysUserStatusEnum {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常"),
    PENDING(2, "待审核");

    private static final Set<Integer> CODES = Arrays.stream(values())
            .map(SysUserStatusEnum::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final int code;
    private final String description;

    SysUserStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean supports(Integer code) {
        return code != null && CODES.contains(code);
    }
}

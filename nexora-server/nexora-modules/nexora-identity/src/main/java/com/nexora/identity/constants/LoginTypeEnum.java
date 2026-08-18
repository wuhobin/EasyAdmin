package com.nexora.identity.constants;

import lombok.Getter;

@Getter
public enum LoginTypeEnum {
    EMAIL(1),
    WECHAT_MP(2);

    private final int code;

    LoginTypeEnum(int code) {
        this.code = code;
    }
}

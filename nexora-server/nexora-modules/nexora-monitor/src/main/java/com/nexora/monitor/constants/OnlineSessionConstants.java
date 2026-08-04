package com.nexora.monitor.constants;

public final class OnlineSessionConstants {

    public static final String SESSION_ID_PATTERN =
            "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-"
                    + "[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    public static final String INVALID_SESSION_ID_MESSAGE =
            "会话编号必须是规范 UUID v4";

    private OnlineSessionConstants() {
    }
}

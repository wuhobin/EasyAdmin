package com.aurora.common;

import com.aurora.starter.webmvc.exception.BizCode;

public enum ResultCode implements BizCode {

    SUCCESS( 200, "成功" ),

    FAILURE( 400, "失败" ),

    ERROR(-1, "操作异常"),

    ERROR_DEFAULT(500,"系统繁忙，请稍后重试"),

    NOT_LOGIN(401, "当前会话已过期，请重新登录"),

    NO_PERMISSION(-7,"无权限"),

    ERROR_PASSWORD(-8,"用户帐号或者密码错误!"),

    DISABLE_ACCOUNT(-12,"该账号已被管理员禁止登录!"),

    ERROR_USER_NOT_EXIST(10009, "用户不存在"),

    ERROR_EXCEPTION_MOBILE_CODE(10003,"验证码不正确或已过期，请重新输入"),

    PARAMS_ILLEGAL(10018,"参数不合法!!");

    public final int code;

    public final String desc;

    ResultCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return desc;
    }

}

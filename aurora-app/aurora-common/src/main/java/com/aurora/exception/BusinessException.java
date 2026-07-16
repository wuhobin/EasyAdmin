package com.aurora.exception;

import com.aurora.common.ResultCode;
import com.aurora.starter.webmvc.exception.BizException;

public class BusinessException extends BizException {

    public BusinessException(String msg,Throwable e) {
        super(ResultCode.ERROR_DEFAULT.code, msg, e);
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode);
    }

    public BusinessException(String msg) {
        super(ResultCode.ERROR_DEFAULT.code, msg);
    }

    public BusinessException(Integer code, String msg) {
        super(code, msg);
    }

    public BusinessException(Integer code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

    public BusinessException(Throwable cause) {
        super(ResultCode.ERROR.code, cause.getMessage(), cause);
    }
}

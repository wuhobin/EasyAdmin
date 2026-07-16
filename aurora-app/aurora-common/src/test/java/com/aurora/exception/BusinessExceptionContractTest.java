package com.aurora.exception;

import com.aurora.common.ResultCode;
import com.aurora.starter.webmvc.exception.BizCode;
import com.aurora.starter.webmvc.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionContractTest {

    @Test
    void usesPlatformBusinessExceptionContract() {
        BusinessException exception = new BusinessException(ResultCode.ERROR_PASSWORD);

        assertThat(exception).isInstanceOf(BizException.class);
        assertThat(exception.getCode()).isEqualTo(ResultCode.ERROR_PASSWORD.code);
        assertThat(exception.getMessage()).isEqualTo(ResultCode.ERROR_PASSWORD.desc);
        assertThat(ResultCode.class).isAssignableTo(BizCode.class);
    }
}

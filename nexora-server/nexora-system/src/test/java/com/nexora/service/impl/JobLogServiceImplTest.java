package com.nexora.service.impl;

import com.aurora.starter.quartz.mapper.QuartzJobLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobLogServiceImplTest {

    @Mock
    private QuartzJobLogMapper mapper;

    private JobLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JobLogServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void cleansJobLogsWithAnExplicitCondition() {
        service.cleanJobLog();

        verify(mapper).delete(argThat(wrapper -> wrapper != null
                && wrapper.getSqlSegment().contains("log_id IS NOT NULL")));
    }
}

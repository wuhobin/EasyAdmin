package com.nexora.config;

import com.nexora.biz.system.SysConfigGroupBizService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SysConfigGroupStartupValidator implements ApplicationRunner {

    private final SysConfigGroupBizService configGroupBizService;

    @Override
    public void run(ApplicationArguments args) {
        configGroupBizService.validateDatabase();
    }
}

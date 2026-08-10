package com.nexora;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author whb
 */
@SpringBootApplication
@MapperScan({"com.nexora.identity.mapper", "com.nexora.file.mapper", "com.nexora.mail.mapper", "com.nexora.monitor.mapper", "com.nexora.system.mapper", "com.nexora.message.mapper"})
public class NexoraBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexoraBootApplication.class, args);
    }
}

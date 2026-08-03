package com.nexora;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author whb
 */
@SpringBootApplication
@EnableFileStorage
@MapperScan({"com.nexora.mapper", "com.nexora.identity.mapper", "com.nexora.file.mapper"})
public class NexoraBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexoraBootApplication.class, args);
    }
}

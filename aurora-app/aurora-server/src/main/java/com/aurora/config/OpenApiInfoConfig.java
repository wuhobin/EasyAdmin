package com.aurora.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Springdoc OpenAPI 信息元数据装配。
 *
 * <p>springdoc 2.8.9 在没有 {@code OpenAPI} Bean 时硬编码默认 Info（{@code DEFAULT_TITLE} / {@code DEFAULT_VERSION}），
 * 不读取 {@code springdoc.info.*} 配置；本类构造默认 Info Bean，把 {@code springdoc.info.*} 中的字段填回去。</p>
 *
 * <p>未声明的字段（{@code contact.email} 等）会从所给字符串 trim 后为空串，整体跳过挂载；与 platform-boot-starter 之前
 * Knife4jExtProperties 行为一致。</p>
 *
 * <p>激活条件：显式设置 {@code platform.openapi.info.enabled=true} 时本 Bean 才装载；
 * 业务需要整体覆盖（{@code SecurityScheme}、{@code Server} 列表等）时可直接将 enabled 置为 false 并注入自己的
 * {@code OpenAPI} Bean。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "platform.openapi.info", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiInfoConfig {

    @Value("${springdoc.info.title:Aurora API}")
    private String title;

    @Value("${springdoc.info.version:1.0.0}")
    private String version;

    @Value("${springdoc.info.description:}")
    private String description;

    @Value("${springdoc.info.terms-of-service:}")
    private String termsOfService;

    @Value("${springdoc.info.contact.name:}")
    private String contactName;

    @Value("${springdoc.info.contact.url:}")
    private String contactUrl;

    @Value("${springdoc.info.contact.email:}")
    private String contactEmail;

    @Value("${springdoc.info.license.name:}")
    private String licenseName;

    @Value("${springdoc.info.license.url:}")
    private String licenseUrl;

    @Bean
    public OpenAPI auroraOpenApi() {
        Info info = new Info()
                .title(title)
                .version(version);

        if (StringUtils.hasText(description)) {
            info.description(description);
        }
        if (StringUtils.hasText(termsOfService)) {
            info.termsOfService(termsOfService);
        }
        if (StringUtils.hasText(contactName) || StringUtils.hasText(contactUrl) || StringUtils.hasText(contactEmail)) {
            Contact c = new Contact();
            if (StringUtils.hasText(contactName)) c.name(contactName);
            if (StringUtils.hasText(contactUrl)) c.url(contactUrl);
            if (StringUtils.hasText(contactEmail)) c.email(contactEmail);
            info.contact(c);
        }
        if (StringUtils.hasText(licenseName) || StringUtils.hasText(licenseUrl)) {
            License l = new License();
            if (StringUtils.hasText(licenseName)) l.name(licenseName);
            if (StringUtils.hasText(licenseUrl)) l.url(licenseUrl);
            info.license(l);
        }
        return new OpenAPI().info(info);
    }
}

package com.nexora.monitor.domain.form;

import com.aurora.starter.mybatisplus.model.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Setter;

@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "在线会话查询表单")
public class OnlineSessionQueryForm extends PageParam {

    private String keyword;

    private String ip;

    @Size(max = 100)
    @Schema(description = "邮箱或昵称关键字")
    public String getKeyword() {
        return normalize(keyword);
    }

    @Size(max = 45)
    @Schema(description = "IP 关键字")
    public String getIp() {
        return normalize(ip);
    }

    @Override
    @Min(1)
    @Schema(description = "页码", defaultValue = "1")
    public Integer getPageNum() {
        return super.getPageNum();
    }

    @Override
    @Min(1)
    @Max(100)
    @Schema(description = "每页条数", defaultValue = "10", maximum = "100")
    public Integer getPageSize() {
        return super.getPageSize();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

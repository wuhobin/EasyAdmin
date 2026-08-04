package com.nexora.monitor.domain.form;

import com.aurora.starter.mybatisplus.model.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(description = "在线会话查询表单")
public class OnlineSessionQueryForm extends PageParam {

    @Size(max = 100)
    @Schema(description = "邮箱或昵称关键字")
    private String keyword;

    @Size(max = 45)
    @Schema(description = "IP 关键字")
    private String ip;

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
}

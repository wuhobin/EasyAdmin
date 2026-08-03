package com.nexora.identity.domain.vo.role;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data
@Schema(description = "角色视图对象")
public class SysRoleVo {
    @Schema(description = "角色ID")
    private Integer id;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色备注")
    private String remarks;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Schema(description = "扩展参数")
    private Map<String, Object> params;
}

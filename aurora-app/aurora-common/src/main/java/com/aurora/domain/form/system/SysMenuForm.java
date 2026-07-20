package com.aurora.domain.form.system;

import com.aurora.constants.MenuTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;
import java.util.Map;
import java.util.List;

@Data
@Schema(description = "菜单表单")
public class SysMenuForm {
    @Schema(description = "菜单ID")
    private Integer id;

    @Schema(description = "父级菜单ID")
    private Integer parentId;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "菜单类型")
    private MenuTypeEnum type;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "是否隐藏")
    private Integer hidden;

    @Schema(description = "是否外链")
    private Integer isExternal;

    @Schema(description = "权限标识")
    private String perm;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "扩展参数")
    private Map<String, Object> params;

    @Schema(description = "子菜单列表")
    private List<SysMenuForm> children;
}

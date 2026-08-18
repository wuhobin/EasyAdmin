package com.nexora.identity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 当前用户可见的工作台统计。
 */
@Data
@Builder
@Schema(description = "工作台统计")
public class WorkbenchSummaryVo {

    /**
     * 是否为超级管理员视图。
     */
    @Schema(description = "是否为超级管理员视图")
    private Boolean administrator;

    /**
     * 系统用户总数，仅管理员可见。
     */
    @Schema(description = "系统用户总数，仅管理员可见")
    private Long userCount;

    /**
     * 角色数量；管理员为系统总数，普通用户为个人角色数。
     */
    @Schema(description = "角色数量；管理员为系统总数，普通用户为个人角色数")
    private Long roleCount;

    /**
     * 非按钮菜单总数，仅管理员可见。
     */
    @Schema(description = "非按钮菜单总数，仅管理员可见")
    private Long menuCount;

    /**
     * 权限数量；管理员为系统总数，普通用户为个人权限数。
     */
    @Schema(description = "权限数量；管理员为系统总数，普通用户为个人权限数")
    private Long permissionCount;

    /**
     * 当前用户可访问的功能数量，仅普通用户可见。
     */
    @Schema(description = "当前用户可访问的功能数量，仅普通用户可见")
    private Long accessibleFeatureCount;
}

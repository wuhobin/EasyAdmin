package com.aurora.entity;

import com.aurora.enums.MenuTypeEnum;
import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("sys_menu")
@Schema(description = "菜单信息")
public class SysMenu extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @Schema(description = "父菜单ID，一级菜单为0")
    private Integer parentId;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "菜单名称")
    private String title;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "菜单类型（CATALOG目录 MENU菜单 BUTTON按钮）")
    private MenuTypeEnum type;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "路由的name")
    private String name;

    @Schema(description = "是否隐藏路由，当设置 true 的时候该路由在侧边栏隐藏")
    private Integer hidden;

    @Schema(description = "是否外链 0:否  1:是")
    private Integer isExternal;

    @Schema(description = "权限标识")
    private String perm;

    @TableField(exist = false)
    private List<SysMenu> children;
}
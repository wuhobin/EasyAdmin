package com.nexora.identity.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "系统路由视图对象")
public class SysRouterVo {
    @Schema(description = "菜单ID")
    private Integer id;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "重定向地址")
    private String redirect;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "路由元信息")
    private MetaVo meta;

    @Schema(description = "子路由列表")
    private List<SysRouterVo> children;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "路由元信息")
    public static class MetaVo {
        @Schema(description = "路由标题")
        private String title;

        @Schema(description = "路由图标")
        private String icon;

        @Schema(description = "是否隐藏")
        private Boolean hidden;

        @Schema(description = "是否外链")
        private Boolean isExternal;
    }
}

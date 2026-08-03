package com.nexora.system.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "系统基础配置")
public class SystemSettings {

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 100, message = "站点名称不能超过100个字符")
    private String siteName;

    @NotBlank(message = "站点短标题不能为空")
    @Size(max = 100, message = "站点短标题不能超过100个字符")
    private String shortTitle;

    @NotNull(message = "站点描述不能为空")
    @Size(max = 500, message = "站点描述不能超过500个字符")
    private String siteDescription;

    @NotNull(message = "站点 Logo 不能为空")
    @Size(max = 1024, message = "站点 Logo 地址不能超过1024个字符")
    private String siteLogo;

    @NotNull(message = "版权信息不能为空")
    @Size(max = 255, message = "版权信息不能超过255个字符")
    private String copyright;

    @NotNull(message = "ICP备案信息不能为空")
    @Size(max = 100, message = "ICP备案信息不能超过100个字符")
    private String icp;

    @NotNull(message = "水印开关不能为空")
    private Boolean watermarkEnabled;

    @NotBlank(message = "水印类型不能为空")
    @Pattern(regexp = "username|username_time|sitename|custom", message = "水印类型不受支持")
    private String watermarkType;

    @NotNull(message = "自定义水印文本不能为空")
    @Size(max = 100, message = "自定义水印文本不能超过100个字符")
    private String watermarkCustomText;

    @NotNull(message = "水印透明度不能为空")
    @DecimalMin(value = "0.01", message = "水印透明度不能小于0.01")
    @DecimalMax(value = "0.5", message = "水印透明度不能大于0.5")
    private Double watermarkOpacity;

    public void setSiteName(String siteName) {
        this.siteName = strip(siteName);
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = strip(shortTitle);
    }

    public void setSiteDescription(String siteDescription) {
        this.siteDescription = strip(siteDescription);
    }

    public void setSiteLogo(String siteLogo) {
        this.siteLogo = strip(siteLogo);
    }

    public void setCopyright(String copyright) {
        this.copyright = strip(copyright);
    }

    public void setIcp(String icp) {
        this.icp = strip(icp);
    }

    public void setWatermarkType(String watermarkType) {
        this.watermarkType = strip(watermarkType);
    }

    public void setWatermarkCustomText(String watermarkCustomText) {
        this.watermarkCustomText = strip(watermarkCustomText);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}

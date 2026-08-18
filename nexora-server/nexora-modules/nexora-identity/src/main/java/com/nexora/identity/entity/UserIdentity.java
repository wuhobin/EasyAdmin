package com.nexora.identity.entity;

import com.aurora.starter.mybatisplus.model.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_identity")
@Schema(description = "用户第三方身份")
public class UserIdentity extends BaseEntity implements Serializable {

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    /** 关联的系统用户ID。 */
    @Schema(description = "关联的系统用户ID")
    private Integer userId;

    /** 身份提供方。 */
    @Schema(description = "身份提供方")
    private String provider;

    /** 身份提供方应用ID。 */
    @Schema(description = "身份提供方应用ID")
    private String providerAppId;

    /** 身份提供方用户ID，微信公众号场景为 OpenID。 */
    @Schema(description = "身份提供方用户ID")
    private String providerUserId;
}

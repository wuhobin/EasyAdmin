package com.aurora.domain.vo.user;

import com.aurora.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author: quequnlong
 * @date: 2025/1/3
 * @description:
 */
@Data
public class OnlineUserVo extends SysUser {

    @Schema(name = "token")
    private String tokenValue;

}

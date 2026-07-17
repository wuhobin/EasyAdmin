package com.aurora.domain.model.user;

import com.aurora.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineUserData extends SysUser {
    private String tokenValue;
}

package com.aurora.domain.model.user;

import com.aurora.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SysUserProfileData {
    private SysUser sysUser;
    private List<String> roles;
}

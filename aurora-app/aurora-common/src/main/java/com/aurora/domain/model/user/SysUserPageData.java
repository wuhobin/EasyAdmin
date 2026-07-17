package com.aurora.domain.model.user;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class SysUserPageData {
    private Integer id;
    private String username;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String nickname;
    private String avatar;
    private String ip;
    private String ipLocation;
    private String mobile;
    private String email;
    private Integer sex;
    private List<Integer> roleIds;
    private Date createTime;
}

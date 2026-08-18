package com.nexora.identity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String contentFormat;
    private Integer noticeType;
    private Integer targetType;
    private String targetIds;
    private Integer status;
    private Integer createBy;
    private String createName;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
}

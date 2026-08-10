package com.nexora.message.domain.form;

import lombok.Data;

import java.util.List;

@Data
public class NoticeForm {
    private Long id;
    private String title;
    private String content;
    private String contentFormat;
    private Integer noticeType;
    private Integer targetType;
    private List<Integer> targetUserIds;
}

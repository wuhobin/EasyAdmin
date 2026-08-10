package com.nexora.message.domain.form;

import lombok.Data;

@Data
public class NoticeQueryForm {
    private String title;
    private Integer noticeType;
    private Integer status;
}

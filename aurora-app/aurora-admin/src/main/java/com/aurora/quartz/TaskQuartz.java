package com.aurora.quartz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("task")
@RequiredArgsConstructor
public class TaskQuartz {

    public void neatMultipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        // 执行多参方法示例
    }

    public void neatParams(String params) {
        System.out.println("执行有参方法：" + params);
    }

    public void neatNoParams() {
        System.out.println("执行无参方法");
    }
}

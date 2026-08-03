package com.nexora.identity.infrastructure;

import cn.idev.excel.FastExcel;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author: quequnlong
 * @date: 2025/1/4
 * @description: excel工具类
 */
public class FastExcelUtils {

    public static <T> void exportExcel(List<T> data, Class<T> c, String fileName, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition",
                "attachment;filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
        // 写入数据
        FastExcel.write(response.getOutputStream(), c)
                .sheet("模板")
                .doWrite(data);
    }
}

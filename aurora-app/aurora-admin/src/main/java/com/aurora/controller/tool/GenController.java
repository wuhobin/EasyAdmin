package com.aurora.controller.tool;

import com.aurora.common.Result;
import com.aurora.entity.GenTable;
import com.aurora.service.GenTableService;
import com.aurora.starter.mybatisplus.model.PageParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/tool/gen")
@RequiredArgsConstructor
@Tag(name = "代码生成业务")
public class GenController {

    private final GenTableService genTableService;

    @GetMapping("/list")
    @Operation(description = "获取代码生成列表")
    public Result<Map<String, Object>> list(GenTable genTable) {
        return Result.success(genTableService.selectGenTableList(genTable));
    }

    @GetMapping("/preview/{tableId}")
    @Operation(description = "预览代码")
    public Result<Map<String, String>> preview(@PathVariable Long tableId) {
        return Result.success(genTableService.previewCode(tableId));
    }

    @DeleteMapping("/{tableIds}")
    @Operation(description = "删除代码生成")
    public Result<Integer> remove(@PathVariable Long[] tableIds) {
        return Result.success(genTableService.deleteGenTableByIds(tableIds));
    }

    @GetMapping("/code/{tables}")
    @Operation(description = "下载代码")
    public Result<Void> genCode(@PathVariable String tables) {
        genTableService.generatorCode(tables);
        return Result.success();
    }

    @GetMapping("/sync/{tableName}")
    @Operation(description = "同步数据库")
    public Result<String> synchDb(@PathVariable String tableName) {
        return Result.success(genTableService.synchDb(tableName));
    }

    @GetMapping("/db/list")
    @Operation(description = "查询数据库列表")
    public Result<Map<String, Object>> dbList(GenTable genTable, PageParam pageParam) {
        Map<String, Object> list = genTableService.selectDbTableList(genTable, pageParam);
        return Result.success(list);
    }

    @PostMapping("/importTable")
    @Operation(description = "导入表结构")
    public Result<Void> importTable(@RequestBody String[] tables) {
        genTableService.importGenTable(tables);
        return Result.success();
    }

    @GetMapping("/download/{tables}")
    @Operation(description = "下载代码")
    public void download(@PathVariable("tables") String tables, HttpServletResponse response) {
        try {
            byte[] data = genTableService.downloadCode(tables.split(","));

            response.reset();
            response.setHeader("Content-Disposition", "attachment; filename=\"code.zip\"");
            response.addHeader("Content-Length", "" + data.length);
            response.setContentType("application/octet-stream; charset=UTF-8");

            IOUtils.write(data, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("下载代码失败", e);
        }
    }
}

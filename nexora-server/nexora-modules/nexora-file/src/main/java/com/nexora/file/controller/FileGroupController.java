package com.nexora.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.file.biz.FileGroupBizService;
import com.nexora.file.domain.form.FileGroupForm;
import com.nexora.file.domain.vo.FileGroupListVo;
import com.nexora.file.domain.vo.SysOssFileGroupVo;
import com.aurora.starter.webmvc.domain.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "文件分组管理")
@RestController
@RequestMapping("/file/groups")
@RequiredArgsConstructor
public class FileGroupController {

    private final FileGroupBizService fileGroupBizService;

    @Operation(summary = "查询文件分组")
    @GetMapping
    @SaCheckPermission("sys:file:list")
    public Result<FileGroupListVo> list(@RequestParam(required = false) Long ownerId) {
        return Result.data(fileGroupBizService.list(ownerId));
    }

    @Operation(summary = "创建文件分组")
    @PostMapping
    @SaCheckPermission("sys:file:upload")
    public Result<SysOssFileGroupVo> create(@RequestBody FileGroupForm form) {
        return Result.data(fileGroupBizService.create(form));
    }

    @Operation(summary = "重命名文件分组")
    @PutMapping("/{id}")
    @SaCheckPermission("sys:file:upload")
    public Result<SysOssFileGroupVo> rename(@PathVariable Long id, @RequestBody FileGroupForm form) {
        return Result.data(fileGroupBizService.rename(id, form));
    }

    @Operation(summary = "删除文件分组")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:file:delete")
    public Result<Void> delete(@PathVariable Long id, @RequestParam(required = false) Long ownerId) {
        fileGroupBizService.delete(id, ownerId);
        return Result.success();
    }
}

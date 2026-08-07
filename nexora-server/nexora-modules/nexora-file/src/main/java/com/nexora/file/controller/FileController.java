package com.nexora.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.file.biz.FileBizService;
import com.nexora.file.domain.form.OssFileQueryForm;
import com.nexora.file.domain.form.FileBatchForm;
import com.nexora.file.domain.form.FileMoveForm;
import com.nexora.file.domain.form.FileRenameForm;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.nexora.file.domain.vo.SysOssFileVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileBizService fileBizService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    @SaCheckPermission("sys:file:upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) Long groupId) {
        return Result.data(groupId == null ? fileBizService.upload(file) : fileBizService.upload(file, groupId));
    }

    public Result<String> upload(MultipartFile file) {
        return Result.data(fileBizService.upload(file));
    }

    @Operation(summary = "查询文件列表")
    @GetMapping("/list")
    @SaCheckPermission("sys:file:list")
    public Result<IPage<SysOssFileVo>> list(OssFileQueryForm form, PageParam pageParam) {
        return Result.data(fileBizService.list(form, pageParam));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/{id}/download")
    @SaCheckPermission("sys:file:download")
    public void downloadById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        fileBizService.download(id, response);
    }

    @Operation(summary = "预览文件")
    @GetMapping("/{id}/preview")
    @SaCheckPermission("sys:file:list")
    public void previewById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        fileBizService.preview(id, response);
    }

    @Operation(summary = "预览文本文件")
    @GetMapping("/{id}/text")
    @SaCheckPermission("sys:file:list")
    public Result<String> previewTextById(@PathVariable Long id) throws IOException {
        return Result.data(fileBizService.textPreview(id));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:file:delete")
    public Result<Void> deleteById(@PathVariable Long id) {
        fileBizService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "批量删除文件")
    @PostMapping("/batch-delete")
    @SaCheckPermission("sys:file:delete")
    public Result<Void> deleteBatch(@RequestBody FileBatchForm form) {
        fileBizService.deleteByIds(form);
        return Result.success();
    }

    @Operation(summary = "批量移动文件")
    @PutMapping("/batch-move")
    @SaCheckPermission("sys:file:upload")
    public Result<Void> moveBatch(@RequestBody FileMoveForm form) {
        fileBizService.move(form);
        return Result.success();
    }

    @Operation(summary = "重命名文件")
    @PutMapping("/{id}/rename")
    @SaCheckPermission("sys:file:upload")
    public Result<Void> rename(@PathVariable Long id, @RequestBody FileRenameForm form) {
        fileBizService.rename(id, form);
        return Result.success();
    }

}

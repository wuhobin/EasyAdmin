package com.aurora.controller.file;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.aurora.dto.file.OssFileQuery;
import com.aurora.entity.SysOssFile;
import com.aurora.service.FileService;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.vo.file.SysOssFileVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    private final SysOssFileService ossFileService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    @SaCheckPermission("sys:file:upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        OssUploadResult result = fileService.upload(file);
        return Result.data(result == null ? null : result.getUrl());
    }

    @Operation(summary = "查询文件列表")
    @GetMapping("/list")
    @SaCheckPermission("sys:file:list")
    public Result<IPage<SysOssFileVo>> list(OssFileQuery query, PageParam pageParam) {
        return Result.data(ossFileService.listFiles(query, pageParam));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/{id}/download")
    @SaCheckPermission("sys:file:download")
    public void downloadById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysOssFile file = ossFileService.getDownloadFile(id);
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = file.getFileName();
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        response.setContentType(resolveMediaType(file.getContentType()).toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (file.getFileSize() != null && file.getFileSize() > 0) {
            response.setContentLengthLong(file.getFileSize());
        }
        ossFileService.download(file, response.getOutputStream());
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:file:delete")
    public Result<Void> deleteById(@PathVariable Long id) {
        ossFileService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "按 URL 删除文件")
    @GetMapping("/delete")
    @SaCheckPermission("sys:file:delete")
    public Result<Boolean> delete(@RequestParam("url") String url) {
        return Result.data(ossFileService.deleteByUrl(url));
    }

    private static MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

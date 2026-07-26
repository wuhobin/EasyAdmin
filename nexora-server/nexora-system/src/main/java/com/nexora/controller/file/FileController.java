package com.nexora.controller.file;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.biz.FileBizService;
import com.nexora.domain.form.query.file.OssFileQueryForm;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.domain.response.Result;
import com.nexora.domain.vo.file.SysOssFileVo;
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
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
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

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:file:delete")
    public Result<Void> deleteById(@PathVariable Long id) {
        fileBizService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "按 URL 删除文件")
    @GetMapping("/delete")
    @SaCheckPermission("sys:file:delete")
    public Result<Boolean> delete(@RequestParam("url") String url) {
        return Result.data(fileBizService.deleteByUrl(url));
    }
}

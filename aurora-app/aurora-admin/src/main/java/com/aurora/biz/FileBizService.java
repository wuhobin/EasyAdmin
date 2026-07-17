package com.aurora.biz;

import com.aurora.domain.convert.OssFileConvert;
import com.aurora.domain.form.query.file.OssFileQueryForm;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.aurora.entity.SysOssFile;
import com.aurora.service.FileService;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

@Service
@RequiredArgsConstructor
public class FileBizService {

    private final FileService fileService;
    private final SysOssFileService ossFileService;

    public String upload(MultipartFile file) {
        OssUploadResult result = fileService.upload(file);
        return result == null ? null : result.getUrl();
    }

    public IPage<SysOssFileVo> list(OssFileQueryForm form, PageParam pageParam) {
        IPage<SysOssFile> page = ossFileService.listFiles(OssFileConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(OssFileConvert.INSTANCE::toVo);
    }

    public SysOssFileVo getDownloadFile(Long id) {
        return OssFileConvert.INSTANCE.toVo(ossFileService.getDownloadFile(id));
    }

    public void download(SysOssFileVo file, OutputStream outputStream) {
        ossFileService.download(OssFileConvert.INSTANCE.toEntity(file), outputStream);
    }

    public void deleteById(Long id) {
        ossFileService.deleteById(id);
    }

    public boolean deleteByUrl(String url) {
        return ossFileService.deleteByUrl(url);
    }
}

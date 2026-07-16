package com.aurora.controller.file;

import com.aurora.entity.SysOssFile;
import com.aurora.service.FileService;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileControllerTest {

    @Test
    void delegatesListAndDeleteOperationsToTheRecordService() {
        FileService fileService = mock(FileService.class);
        SysOssFileService ossFileService = mock(SysOssFileService.class);
        FileController controller = new FileController(fileService, ossFileService);
        SysOssFile query = new SysOssFile();
        PageParam pageParam = new PageParam(1, 10);
        @SuppressWarnings("unchecked")
        IPage<SysOssFile> page = mock(IPage.class);
        when(ossFileService.listFiles(query, pageParam)).thenReturn(page);

        controller.list(query, pageParam);
        controller.deleteById(1L);
        controller.delete("https://oss.example.com/file.png");

        verify(ossFileService).listFiles(query, pageParam);
        verify(ossFileService).deleteById(1L);
        verify(ossFileService).deleteByUrl("https://oss.example.com/file.png");
    }
}

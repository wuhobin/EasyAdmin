package com.nexora.file.controller;

import com.nexora.file.biz.FileBizService;
import com.nexora.file.domain.form.OssFileQueryForm;
import com.nexora.file.domain.vo.SysOssFileVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileControllerTest {

    @Test
    void delegatesListAndDeleteOperationsToTheBizService() {
        FileBizService fileBizService = mock(FileBizService.class);
        FileController controller = new FileController(fileBizService);
        OssFileQueryForm form = new OssFileQueryForm();
        PageParam pageParam = new PageParam(1, 10);
        @SuppressWarnings("unchecked")
        IPage<SysOssFileVo> page = mock(IPage.class);
        when(fileBizService.list(form, pageParam)).thenReturn(page);

        controller.list(form, pageParam);
        controller.deleteById(1L);

        verify(fileBizService).list(form, pageParam);
        verify(fileBizService).deleteById(1L);
    }

    @Test
    void delegatesUploadToTheBizService() {
        FileBizService fileBizService = mock(FileBizService.class);
        FileController controller = new FileController(fileBizService);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", new byte[]{1, 2, 3});

        controller.upload(file);

        verify(fileBizService).upload(file);
    }

    @Test
    void delegatesDownloadToTheBizService() throws Exception {
        FileBizService fileBizService = mock(FileBizService.class);
        FileController controller = new FileController(fileBizService);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.downloadById(1L, response);

        verify(fileBizService).download(1L, response);
    }
}

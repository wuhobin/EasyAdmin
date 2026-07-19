package com.aurora.controller.file;

import com.aurora.biz.FileBizService;
import com.aurora.domain.form.query.file.OssFileQueryForm;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

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
        controller.delete("https://oss.example.com/file.png");

        verify(fileBizService).list(form, pageParam);
        verify(fileBizService).deleteById(1L);
        verify(fileBizService).deleteByUrl("https://oss.example.com/file.png");
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

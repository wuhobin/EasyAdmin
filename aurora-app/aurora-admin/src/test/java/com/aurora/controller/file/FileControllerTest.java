package com.aurora.controller.file;

import com.aurora.biz.FileBizService;
import com.aurora.domain.form.query.file.OssFileQueryForm;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
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
    void downloadsWithStoredMimeTypeAndOriginalFilename() throws Exception {
        FileBizService fileBizService = mock(FileBizService.class);
        FileController controller = new FileController(fileBizService);
        SysOssFileVo file = SysOssFileVo.builder()
                .id(1L)
                .originalFilename("中文图片.png")
                .fileName("stored.png")
                .contentType(MediaType.IMAGE_PNG_VALUE)
                .fileSize(128L)
                .build();
        when(fileBizService.getDownloadFile(1L)).thenReturn(file);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadById(1L, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(response.getContentLengthLong()).isEqualTo(128L);
        assertThat(ContentDisposition.parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).getFilename())
                .isEqualTo("中文图片.png");
        verify(fileBizService).download(file, response.getOutputStream());
    }

    @Test
    void fallsBackToStoredFilenameAndBinaryContentType() throws Exception {
        FileBizService fileBizService = mock(FileBizService.class);
        FileController controller = new FileController(fileBizService);
        SysOssFileVo file = SysOssFileVo.builder()
                .id(1L)
                .fileName("stored.bin")
                .contentType("not a mime type")
                .fileSize(0L)
                .build();
        when(fileBizService.getDownloadFile(1L)).thenReturn(file);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.downloadById(1L, response);

        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("stored.bin");
        assertThat(response.getHeader(HttpHeaders.CONTENT_LENGTH)).isNull();
    }
}

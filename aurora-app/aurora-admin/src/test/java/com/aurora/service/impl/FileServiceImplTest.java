package com.aurora.service.impl;

import com.aurora.entity.SysOssFile;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.task.OssFileRecordRetryTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private OssTemplate ossTemplate;

    @Mock
    private SysOssFileService ossFileService;

    @Mock
    private OssFileRecordRetryTask retryTask;

    @Test
    void preservesTheNativeFileIdAndDoesNotEnqueueAfterSuccessfulInsert() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult("native-id");
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileServiceImpl service = new FileServiceImpl(ossTemplate, ossFileService, retryTask);

        OssUploadResult result = service.upload(file);

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isEqualTo("native-id");
        assertThat(result.getId()).isEqualTo("native-id");
        verify(retryTask, never()).submit(any());
    }

    @Test
    void generatesAFileIdWhenTheNativeResultHasNone() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult(null);
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileServiceImpl service = new FileServiceImpl(ossTemplate, ossFileService, retryTask);

        OssUploadResult result = service.upload(file);

        assertThat(result.getId()).isNotBlank();
    }

    @Test
    void enqueuesTheRecordWhenTheInitialInsertThrows() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenThrow(new IllegalStateException("database unavailable"));
        FileServiceImpl service = new FileServiceImpl(ossTemplate, ossFileService, retryTask);

        OssUploadResult result = service.upload(file);

        assertThat(result.getUrl()).isEqualTo("https://oss.example.com/file.png");
        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void doesNotRetryTheProducerCallWhenQueueSubmissionFails() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(retryTask).submit(any(SysOssFile.class));
        FileServiceImpl service = new FileServiceImpl(ossTemplate, ossFileService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis unavailable");

        verify(retryTask).submit(any(SysOssFile.class));
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
    }

    private static OssUploadResult uploadResult(String id) {
        return OssUploadResult.builder()
                .id(id)
                .url("https://oss.example.com/file.png")
                .filename("file.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .size(3L)
                .platform("qiniu-kodo-1")
                .build();
    }
}

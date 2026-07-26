package com.nexora.biz;

import com.nexora.constants.Constants;
import com.nexora.entity.SysOssFile;
import com.nexora.service.SysOssFileService;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.nexora.task.OssFileRecordRetryTask;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileBizServiceTest {

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
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        String url = service.upload(file);

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isEqualTo("native-id");
        assertThat(url).isEqualTo(uploadResult.getUrl());
        verify(retryTask, never()).submit(any());
    }

    @Test
    void generatesAFileIdWhenTheNativeResultHasNone() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult(null);
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        service.upload(file);

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isNotBlank();
    }

    @Test
    void enqueuesTheRecordWhenTheInitialInsertThrows() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenThrow(new IllegalStateException("database unavailable"));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        String url = service.upload(file);

        assertThat(url).isEqualTo("https://oss.example.com/file.png");
        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void doesNotRetryTheProducerCallWhenQueueSubmissionFails() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(false);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(retryTask).submit(any(SysOssFile.class));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis unavailable");

        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void allowsUserToDeleteOwnFile() {
        SysOssFile file = storedFile(1L, 10L);
        when(ossFileService.getById(1L)).thenReturn(file);
        when(ossTemplate.delete(any(FileInfo.class))).thenReturn(true);
        when(ossFileService.removeById(1L)).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(Constants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.deleteById(1L);
        }

        verify(ossTemplate).delete(any(FileInfo.class));
        verify(ossFileService).removeById(1L);
    }

    @Test
    void rejectsDeletingAnotherUsersFile() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(Constants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.deleteById(1L))
                    .hasMessage("只能删除自己上传的文件");
        }

        verify(ossTemplate, never()).delete(any(FileInfo.class));
        verify(ossFileService, never()).removeById(any());
    }

    @Test
    void allowsAdminToDeleteAnotherUsersFile() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        when(ossTemplate.delete(any(FileInfo.class))).thenReturn(true);
        when(ossFileService.removeById(1L)).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(Constants.ADMIN)).thenReturn(true);

            service.deleteById(1L);
        }

        verify(ossTemplate).delete(any(FileInfo.class));
        verify(ossFileService).removeById(1L);
    }

    @Test
    void rejectsUrlDeletionWhenAnyRecordBelongsToAnotherUser() {
        String url = "https://oss.example.com/file.png";
        when(ossFileService.listByUrl(url)).thenReturn(List.of(
                storedFile(1L, 10L),
                storedFile(2L, 20L)
        ));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(Constants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.deleteByUrl(url))
                    .hasMessage("只能删除自己上传的文件");
        }

        verify(ossTemplate, never()).delete(any(FileInfo.class));
        verify(ossFileService, never()).removeBatchByIds(any());
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

    private static SysOssFile storedFile(Long id, Long uploaderId) {
        return SysOssFile.builder()
                .id(id)
                .fileId("file-" + id)
                .fileUrl("https://oss.example.com/file.png")
                .fileName("file.png")
                .platform("qiniu-kodo-1")
                .uploaderId(uploaderId)
                .build();
    }
}

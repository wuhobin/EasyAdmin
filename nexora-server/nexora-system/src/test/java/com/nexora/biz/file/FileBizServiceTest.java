package com.nexora.biz.file;

import com.nexora.constants.CommonConstants;
import com.nexora.domain.form.query.file.OssFileQueryForm;
import com.nexora.domain.query.OssFileQuery;
import com.nexora.entity.SysOssFile;
import com.nexora.service.SysOssFileService;
import com.nexora.service.SysUserService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.oss.model.OssUploadResult;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.security.context.SecurityUtils;
import com.nexora.task.OssFileRecordRetryTask;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
    private SysUserService sysUserService;

    @Mock
    private OssFileRecordRetryTask retryTask;

    @Test
    void recordsTheAuthenticatedUploaderAndPreservesTheNativeFileId() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult("native-id");
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        String url;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            url = service.upload(file);
        }

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isEqualTo("native-id");
        assertThat(captor.getValue().getUploaderId()).isEqualTo(10L);
        assertThat(url).isEqualTo(uploadResult.getUrl());
        verify(retryTask, never()).submit(any());
    }

    @Test
    void generatesAFileIdWhenTheNativeResultHasNone() {
        MockMultipartFile file = file();
        OssUploadResult uploadResult = uploadResult(null);
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult);
        when(ossFileService.saveIfAbsent(any())).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            service.upload(file);
        }

        ArgumentCaptor<SysOssFile> captor = ArgumentCaptor.forClass(SysOssFile.class);
        verify(ossFileService).saveIfAbsent(captor.capture());
        assertThat(captor.getValue().getFileId()).isNotBlank();
    }

    @Test
    void rejectsUploadBeforeCallingOssWhenTheCurrentUserIsMissing() {
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(0);

            assertThatThrownBy(() -> service.upload(file()))
                    .hasMessage(CommonConstants.FILE_CURRENT_USER_REQUIRED_MESSAGE);
        }

        verify(ossTemplate, never()).upload(any(MockMultipartFile.class), anyString());
        verify(ossFileService, never()).saveIfAbsent(any());
    }

    @Test
    void enqueuesTheRecordWhenTheInitialInsertThrows() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenThrow(new IllegalStateException("database unavailable"));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        String url;
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            url = service.upload(file);
        }

        assertThat(url).isEqualTo("https://oss.example.com/file.png");
        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void doesNotRetryTheProducerCallWhenQueueSubmissionFails() {
        MockMultipartFile file = file();
        when(ossTemplate.upload(any(MockMultipartFile.class), anyString())).thenReturn(uploadResult("file-123"));
        when(ossFileService.saveIfAbsent(any())).thenReturn(false);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(retryTask).submit(any(SysOssFile.class));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.upload(file))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("redis unavailable");
        }

        verify(retryTask).submit(any(SysOssFile.class));
    }

    @Test
    void forcesTheCurrentUploaderOnNonAdminListQueries() {
        OssFileQueryForm form = new OssFileQueryForm();
        form.setUploaderId(99L);
        Page<SysOssFile> page = new Page<>(1, 10);
        when(ossFileService.listFiles(any(), any())).thenReturn(page);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);
            service.list(form, new PageParam(1, 10));
        }

        ArgumentCaptor<OssFileQuery> queryCaptor = ArgumentCaptor.forClass(OssFileQuery.class);
        verify(ossFileService).listFiles(queryCaptor.capture(), any(PageParam.class));
        assertThat(queryCaptor.getValue().getUploaderId()).isEqualTo(10L);
    }

    @Test
    void preservesTheOptionalUploaderFilterOnAdminListQueries() {
        OssFileQueryForm form = new OssFileQueryForm();
        form.setUploaderId(99L);
        Page<SysOssFile> page = new Page<>(1, 10);
        when(ossFileService.listFiles(any(), any())).thenReturn(page);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(true);
            service.list(form, new PageParam(1, 10));
        }

        ArgumentCaptor<OssFileQuery> queryCaptor = ArgumentCaptor.forClass(OssFileQuery.class);
        verify(ossFileService).listFiles(queryCaptor.capture(), any(PageParam.class));
        assertThat(queryCaptor.getValue().getUploaderId()).isEqualTo(99L);
    }

    @Test
    void allowsUserToDeleteOwnFile() {
        SysOssFile file = storedFile(1L, 10L);
        when(ossFileService.getById(1L)).thenReturn(file);
        when(ossTemplate.delete(any(FileInfo.class))).thenReturn(true);
        when(ossFileService.removeById(1L)).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.deleteById(1L);
        }

        verify(sysUserService).existsByAvatar(file.getFileUrl());
        verify(ossTemplate).delete(any(FileInfo.class));
        verify(ossFileService).removeById(1L);
    }

    @Test
    void rejectsDeletingAFileUsedAsAnAvatarEvenForAdmin() {
        SysOssFile file = storedFile(1L, 20L);
        when(ossFileService.getById(1L)).thenReturn(file);
        when(sysUserService.existsByAvatar(file.getFileUrl())).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteById(1L))
                    .hasMessage(CommonConstants.FILE_AVATAR_IN_USE_MESSAGE);
        }

        verify(sysUserService).existsByAvatar(file.getFileUrl());
        verify(ossTemplate, never()).delete(any(FileInfo.class));
        verify(ossFileService, never()).removeById(any());
    }

    @Test
    void rejectsDeletingAnotherUsersFile() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.deleteById(1L))
                    .hasMessage(CommonConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }

        verify(sysUserService, never()).existsByAvatar(anyString());
        verify(ossTemplate, never()).delete(any(FileInfo.class));
        verify(ossFileService, never()).removeById(any());
    }

    @Test
    void reportsTheSameMessageWhenDeletingAMissingFile() {
        when(ossFileService.getById(1L)).thenReturn(null);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        assertThatThrownBy(() -> service.deleteById(1L))
                .hasMessage(CommonConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);

        verify(sysUserService, never()).existsByAvatar(anyString());
        verify(ossTemplate, never()).delete(any(FileInfo.class));
    }

    @Test
    void allowsAdminToDeleteAnotherUsersFile() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        when(ossTemplate.delete(any(FileInfo.class))).thenReturn(true);
        when(ossFileService.removeById(1L)).thenReturn(true);
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(true);

            service.deleteById(1L);
        }

        verify(ossTemplate).delete(any(FileInfo.class));
        verify(ossFileService).removeById(1L);
    }

    @Test
    void rejectsDownloadingAnotherUsersFileWithTheUnifiedMessage() {
        when(ossFileService.getById(1L)).thenReturn(storedFile(1L, 20L));
        FileBizService service = new FileBizService(ossTemplate, ossFileService, sysUserService, retryTask);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(CommonConstants.ADMIN)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.download(1L, new MockHttpServletResponse()))
                    .hasMessage(CommonConstants.FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE);
        }

        verify(ossTemplate, never()).getFileStorageService();
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

package com.aurora.service.impl;

import com.aurora.dto.file.OssFileRecordRetryData;
import com.aurora.entity.SysOssFile;
import com.aurora.mapper.SysOssFileMapper;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.webmvc.exception.BizException;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysOssFileServiceImplTest {

    @Mock
    private SysOssFileMapper mapper;

    @Mock
    private OssTemplate ossTemplate;

    @Mock
    private FileStorageService fileStorageService;

    private SysOssFileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysOssFileServiceImpl(ossTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void treatsAnExistingFileIdAsAnIdempotentSuccess() {
        OssFileRecordRetryData data = retryData();
        when(mapper.exists(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        assertThat(service.saveIfAbsent(data)).isTrue();

        verify(mapper, never()).insert(org.mockito.ArgumentMatchers.any(SysOssFile.class));
    }

    @Test
    void keepsTheDatabaseRecordWhenOssDeletionFails() {
        SysOssFile file = file();
        when(mapper.selectById(file.getId())).thenReturn(file);
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(file.getId()))
                .isInstanceOf(BizException.class);

        verify(mapper, never()).deleteById(file.getId());
    }

    @Test
    void physicallyDeletesTheRecordAfterOssDeletionSucceeds() {
        SysOssFile file = file();
        when(mapper.selectById(file.getId())).thenReturn(file);
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);
        when(mapper.deleteById(file.getId())).thenReturn(1);

        service.deleteById(file.getId());

        verify(mapper).deleteById(file.getId());
    }

    @Test
    void passesStoredPlatformAndObjectKeyToOssDeletion() {
        SysOssFile file = file();
        when(mapper.selectById(file.getId())).thenReturn(file);
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);
        when(mapper.deleteById(file.getId())).thenReturn(1);

        service.deleteById(file.getId());

        verify(ossTemplate).delete(org.mockito.ArgumentMatchers.argThat((FileInfo fileInfo) ->
                file.getFileUrl().equals(fileInfo.getUrl())
                        && file.getPlatform().equals(fileInfo.getPlatform())
                        && "base/20260717/file.png".equals(fileInfo.getFilename())));
    }

    @Test
    void identifiesTheResidualRecordWhenDatabaseDeletionFails() {
        SysOssFile file = file();
        when(mapper.selectById(file.getId())).thenReturn(file);
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);
        when(mapper.deleteById(file.getId())).thenReturn(0);

        assertThatThrownBy(() -> service.deleteById(file.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("id=" + file.getId())
                .hasMessageContaining("fileId=" + file.getFileId());
    }

    @Test
    void legacyUrlDeletionRemovesAllRecordsForTheDeletedObject() {
        SysOssFile first = file();
        SysOssFile second = SysOssFile.builder()
                .id(2L)
                .fileId("file-456")
                .fileUrl(first.getFileUrl())
                .build();
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(first, second));
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);
        when(mapper.delete(org.mockito.ArgumentMatchers.any())).thenReturn(2);

        assertThat(service.deleteByUrl(first.getFileUrl())).isTrue();

        verify(mapper).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void legacyUrlDeletionUsesStoredPlatformAndObjectKey() {
        SysOssFile first = file();
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(first));
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);
        when(mapper.delete(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        assertThat(service.deleteByUrl(first.getFileUrl())).isTrue();

        verify(ossTemplate).delete(org.mockito.ArgumentMatchers.argThat((FileInfo fileInfo) ->
                first.getFileUrl().equals(fileInfo.getUrl())
                        && first.getPlatform().equals(fileInfo.getPlatform())
                        && "base/20260717/file.png".equals(fileInfo.getFilename())));
    }

    @Test
    void legacyUrlDeletionWithoutRecordUsesDefaultPlatform() {
        String url = "https://oss.example.com/base/20260717/file.png";
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(ossTemplate.getFileStorageService()).thenReturn(fileStorageService);
        when(fileStorageService.getDefaultPlatform()).thenReturn("qiniu-kodo-1");
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);

        assertThat(service.deleteByUrl(url)).isTrue();

        verify(ossTemplate).delete(org.mockito.ArgumentMatchers.argThat((FileInfo fileInfo) ->
                url.equals(fileInfo.getUrl())
                        && "qiniu-kodo-1".equals(fileInfo.getPlatform())
                        && "base/20260717/file.png".equals(fileInfo.getFilename())));
    }

    @Test
    void identifiesResidualRecordsWhenLegacyUrlDatabaseDeletionFails() {
        SysOssFile first = file();
        SysOssFile second = SysOssFile.builder()
                .id(2L)
                .fileId("file-456")
                .fileUrl(first.getFileUrl())
                .build();
        when(mapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(first, second));
        when(ossTemplate.delete(org.mockito.ArgumentMatchers.any(FileInfo.class))).thenReturn(true);
        when(mapper.delete(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        assertThatThrownBy(() -> service.deleteByUrl(first.getFileUrl()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("ids=[1, 2]")
                .hasMessageContaining("fileIds=[file-123, file-456]");
    }

    private static OssFileRecordRetryData retryData() {
        return OssFileRecordRetryData.builder()
                .fileId("file-123")
                .fileUrl("https://oss.example.com/file.png")
                .fileName("file.png")
                .originalFilename("avatar.png")
                .contentType("image/png")
                .fileSize(128L)
                .platform("qiniu-kodo-1")
                .uploaderId(1L)
                .uploaderName("admin")
                .build();
    }

    private static SysOssFile file() {
        return SysOssFile.builder()
                .id(1L)
                .fileId("file-123")
                .fileUrl("https://oss.example.com/base/20260717/file.png")
                .fileName("file.png")
                .platform("qiniu-kodo-1")
                .build();
    }
}

package com.aurora.service.impl;

import com.aurora.dto.file.OssFileRecordRetryData;
import com.aurora.entity.SysOssFile;
import com.aurora.mapper.SysOssFileMapper;
import com.aurora.starter.oss.template.OssTemplate;
import com.aurora.starter.webmvc.exception.BizException;
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
        when(ossTemplate.delete(file.getFileUrl())).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(file.getId()))
                .isInstanceOf(BizException.class);

        verify(mapper, never()).deleteById(file.getId());
    }

    @Test
    void physicallyDeletesTheRecordAfterOssDeletionSucceeds() {
        SysOssFile file = file();
        when(mapper.selectById(file.getId())).thenReturn(file);
        when(ossTemplate.delete(file.getFileUrl())).thenReturn(true);
        when(mapper.deleteById(file.getId())).thenReturn(1);

        service.deleteById(file.getId());

        verify(mapper).deleteById(file.getId());
    }

    @Test
    void identifiesTheResidualRecordWhenDatabaseDeletionFails() {
        SysOssFile file = file();
        when(mapper.selectById(file.getId())).thenReturn(file);
        when(ossTemplate.delete(file.getFileUrl())).thenReturn(true);
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
        when(ossTemplate.delete(first.getFileUrl())).thenReturn(true);
        when(mapper.delete(org.mockito.ArgumentMatchers.any())).thenReturn(2);

        assertThat(service.deleteByUrl(first.getFileUrl())).isTrue();

        verify(mapper).delete(org.mockito.ArgumentMatchers.any());
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
        when(ossTemplate.delete(first.getFileUrl())).thenReturn(true);
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
                .fileUrl("https://oss.example.com/file.png")
                .build();
    }
}

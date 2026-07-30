package com.nexora.service.impl;

import com.nexora.domain.query.OssFileQuery;
import com.nexora.entity.SysOssFile;
import com.nexora.mapper.SysOssFileMapper;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysOssFileServiceImplTest {

    @Mock
    private SysOssFileMapper mapper;

    private SysOssFileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysOssFileServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void treatsAnExistingFileIdAsAnIdempotentSuccess() {
        SysOssFile file = file();
        when(mapper.exists(any())).thenReturn(true);

        assertThat(service.saveIfAbsent(file)).isTrue();

        verify(mapper, never()).insert(any(SysOssFile.class));
    }

    @Test
    void treatsConcurrentDuplicateInsertAsAnIdempotentSuccess() {
        SysOssFile file = file();
        when(mapper.exists(any())).thenReturn(false);
        when(mapper.insert(file)).thenThrow(new DuplicateKeyException("duplicate"));

        assertThat(service.saveIfAbsent(file)).isTrue();
    }

    @Test
    void returnsEntitiesForTheBizConversionBoundary() {
        SysOssFile file = file();
        Page<SysOssFile> entityPage = new Page<>(1, 10);
        entityPage.setRecords(List.of(file));
        entityPage.setTotal(1L);
        when(mapper.selectPage(any(), any())).thenReturn(entityPage);

        IPage<SysOssFile> result = service.listFiles(new OssFileQuery(), new PageParam(1, 10));

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).containsExactly(file);
    }

    private static SysOssFile file() {
        return SysOssFile.builder()
                .id(1L)
                .fileId("file-123")
                .fileUrl("https://oss.example.com/file.png")
                .fileName("file.png")
                .build();
    }
}

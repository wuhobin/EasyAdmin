package com.nexora.file.biz;

import com.nexora.constants.SecurityConstants;
import com.nexora.file.constants.FileConstants;
import com.nexora.file.domain.form.FileGroupForm;
import com.nexora.file.entity.SysOssFileGroup;
import com.nexora.file.service.SysOssFileGroupService;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.security.context.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class FileGroupBizServiceTest {

    @Mock
    private SysOssFileGroupService groupService;

    @Mock
    private SysOssFileService fileService;

    @Test
    void createsAGroupForTheAuthenticatedUser() {
        when(groupService.save(any(SysOssFileGroup.class))).thenReturn(true);
        FileGroupForm form = new FileGroupForm();
        form.setOwnerId(99L);
        form.setName("Documents");
        FileGroupBizService service = new FileGroupBizService(groupService, fileService);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            service.create(form);
        }

        ArgumentCaptor<SysOssFileGroup> captor = ArgumentCaptor.forClass(SysOssFileGroup.class);
        verify(groupService).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(10L);
        assertThat(captor.getValue().getName()).isEqualTo("Documents");
    }

    @Test
    void reportsWhenGroupCreationDoesNotInsertARow() {
        when(groupService.save(any(SysOssFileGroup.class))).thenReturn(false);
        FileGroupForm form = new FileGroupForm();
        form.setName("Documents");
        FileGroupBizService service = new FileGroupBizService(groupService, fileService);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.create(form))
                    .hasMessage(FileConstants.FILE_GROUP_CREATE_FAILED_MESSAGE);
        }
    }

    @Test
    void reportsWhenGroupRenameDoesNotUpdateARow() {
        SysOssFileGroup group = SysOssFileGroup.builder().id(1L).ownerId(10L).name("Old").build();
        when(groupService.getById(1L)).thenReturn(group);
        when(groupService.updateById(group)).thenReturn(false);
        FileGroupForm form = new FileGroupForm();
        form.setName("New");
        FileGroupBizService service = new FileGroupBizService(groupService, fileService);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(() -> SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE)).thenReturn(false);
            securityUtils.when(SecurityUtils::getLoginIdAsInt).thenReturn(10);

            assertThatThrownBy(() -> service.rename(1L, form))
                    .hasMessage(FileConstants.FILE_GROUP_UPDATE_FAILED_MESSAGE);
        }
    }
}

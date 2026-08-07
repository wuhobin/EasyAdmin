package com.nexora.file.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nexora.constants.SecurityConstants;
import com.nexora.file.constants.FileConstants;
import com.nexora.file.domain.form.FileGroupForm;
import com.nexora.file.domain.vo.FileGroupListVo;
import com.nexora.file.domain.vo.SysOssFileGroupVo;
import com.nexora.file.entity.SysOssFile;
import com.nexora.file.entity.SysOssFileGroup;
import com.nexora.file.service.SysOssFileGroupService;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.webmvc.exception.BizException;
import com.aurora.starter.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileGroupBizService {

    private final SysOssFileGroupService groupService;
    private final SysOssFileService fileService;

    public FileGroupListVo list(Long requestedOwnerId) {
        if (isAdmin() && requestedOwnerId == null) {
            return FileGroupListVo.builder()
                    .groups(List.of())
                    .ungroupedCount(0L)
                    .scopeRequired(true)
                    .build();
        }
        Long ownerId = resolveOwnerId(requestedOwnerId);
        List<SysOssFileGroupVo> groups = groupService.listByOwnerId(ownerId).stream()
                .map(SysOssFileGroupVo::from)
                .toList();
        Long ungroupedCount = groupService.countUngrouped(ownerId);
        return FileGroupListVo.builder()
                .groups(groups)
                .ungroupedCount(ungroupedCount == null ? 0L : ungroupedCount)
                .scopeRequired(false)
                .build();
    }

    @Transactional
    public SysOssFileGroupVo create(FileGroupForm form) {
        Long ownerId = resolveOwnerId(form == null ? null : form.getOwnerId());
        String name = normalizeName(form == null ? null : form.getName());
        SysOssFileGroup group = SysOssFileGroup.builder().ownerId(ownerId).name(name).build();
        try {
            if (!groupService.save(group)) {
                throw new BizException(FileConstants.FILE_GROUP_CREATE_FAILED_MESSAGE);
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(FileConstants.FILE_GROUP_NAME_EXISTS_MESSAGE, exception);
        }
        return SysOssFileGroupVo.from(group);
    }

    @Transactional
    public SysOssFileGroupVo rename(Long id, FileGroupForm form) {
        Long ownerId = resolveOwnerId(form == null ? null : form.getOwnerId());
        SysOssFileGroup group = requireOwnedGroup(id, ownerId);
        group.setName(normalizeName(form == null ? null : form.getName()));
        try {
            if (!groupService.updateById(group)) {
                throw new BizException(FileConstants.FILE_GROUP_UPDATE_FAILED_MESSAGE);
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(FileConstants.FILE_GROUP_NAME_EXISTS_MESSAGE, exception);
        }
        return SysOssFileGroupVo.from(group);
    }

    @Transactional
    public void delete(Long id, Long requestedOwnerId) {
        Long ownerId = resolveOwnerId(requestedOwnerId);
        SysOssFileGroup group = requireOwnedGroup(id, ownerId);
        LambdaUpdateWrapper<SysOssFile> update = new LambdaUpdateWrapper<SysOssFile>()
                .set(SysOssFile::getGroupId, null)
                .eq(SysOssFile::getGroupId, group.getId())
                .eq(SysOssFile::getUploaderId, ownerId);
        fileService.update(update);
        if (!groupService.removeById(group.getId())) {
            throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE);
        }
    }

    public SysOssFileGroup validateGroupForUploader(Long groupId, Long uploaderId) {
        if (groupId == null) {
            return null;
        }
        if (uploaderId == null) {
            throw new BizException(FileConstants.FILE_CURRENT_USER_REQUIRED_MESSAGE);
        }
        return requireOwnedGroup(groupId, uploaderId);
    }

    public Map<Long, String> namesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return groupService.listByIds(ids).stream()
                .collect(Collectors.toMap(SysOssFileGroup::getId, SysOssFileGroup::getName, (left, right) -> left));
    }

    public SysOssFileGroup requireOwnedGroup(Long id, Long ownerId) {
        if (id == null || ownerId == null) {
            throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE);
        }
        SysOssFileGroup group = groupService.getById(id);
        if (group == null || !ownerId.equals(group.getOwnerId())) {
            throw new BizException(FileConstants.FILE_GROUP_NOT_FOUND_MESSAGE);
        }
        return group;
    }

    private Long resolveOwnerId(Long requestedOwnerId) {
        if (isAdmin()) {
            if (requestedOwnerId == null || requestedOwnerId <= 0) {
                throw new BizException(FileConstants.FILE_GROUP_REQUIRED_MESSAGE);
            }
            return requestedOwnerId;
        }
        int currentUserId = SecurityUtils.getLoginIdAsInt();
        if (currentUserId <= 0) {
            throw new BizException(FileConstants.FILE_CURRENT_USER_REQUIRED_MESSAGE);
        }
        return (long) currentUserId;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? null : name.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BizException(FileConstants.FILE_GROUP_NAME_REQUIRED_MESSAGE);
        }
        if (normalized.length() > 50) {
            throw new BizException(FileConstants.FILE_GROUP_NAME_TOO_LONG_MESSAGE);
        }
        return normalized;
    }

    private static boolean isAdmin() {
        return SecurityUtils.hasRole(SecurityConstants.ADMIN_ROLE_CODE);
    }
}

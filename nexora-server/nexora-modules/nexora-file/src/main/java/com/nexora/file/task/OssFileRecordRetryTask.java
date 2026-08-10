package com.nexora.file.task;

import com.nexora.file.entity.SysOssFile;
import com.nexora.file.entity.SysOssFileGroup;
import com.nexora.file.service.SysOssFileGroupService;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.redis.core.task.DelayedRetryTask;
import com.aurora.starter.redis.model.DelayRetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class OssFileRecordRetryTask extends DelayedRetryTask<SysOssFile> {

    static final String TASK_GROUP = "oss:file:record:retry";
    static final int MAX_COUNT = 9;
    static final long INTERVAL_SECONDS = 15L;

    private final SysOssFileService ossFileService;
    private final SysOssFileGroupService groupService;

    @Autowired
    public OssFileRecordRetryTask(SysOssFileService ossFileService, SysOssFileGroupService groupService) {
        this.ossFileService = ossFileService;
        this.groupService = groupService;
    }

    public OssFileRecordRetryTask(SysOssFileService ossFileService) {
        this(ossFileService, null);
    }

    public void submit(SysOssFile data) {
        DelayRetry<SysOssFile> retry = new DelayRetry<SysOssFile>()
                .setData(data)
                .setMaxCount(MAX_COUNT)
                .setInterval(INTERVAL_SECONDS)
                .setUseSameInterval(false);
        producer(retry, INTERVAL_SECONDS);
    }

    @Override
    public String getTaskGroup() {
        return TASK_GROUP;
    }

    @Override
    protected boolean execute(SysOssFile data) {
        normalizeMissingGroup(data);
        if (!ossFileService.saveIfAbsent(data)) {
            throw new IllegalStateException("OSS file record insert returned false: " + data.getFileId());
        }
        return true;
    }

    private void normalizeMissingGroup(SysOssFile data) {
        if (data.getGroupId() == null || groupService == null) {
            return;
        }
        SysOssFileGroup group = groupService.getById(data.getGroupId());
        if (group == null || !Objects.equals(group.getOwnerId(), data.getUploaderId())) {
            log.info("File group disappeared before record retry, storing file as ungrouped, fileId={}, groupId={}",
                    data.getFileId(), data.getGroupId());
            data.setGroupId(null);
        }
    }

    @Override
    protected void handleException(DelayRetry<SysOssFile> task, Exception exception) {
        SysOssFile data = task.getData();
        if (task.getCount() >= task.getMaxCount() - 1) {
            log.error("OSS file record retry exhausted, fileId={}, url={}",
                    data.getFileId(), data.getFileUrl(), exception);
            throw new IllegalStateException("OSS file record retry exhausted: " + data.getFileId(), exception);
        }
        log.warn("OSS file record retry failed, fileId={}, count={}", data.getFileId(), task.getCount());
    }
}

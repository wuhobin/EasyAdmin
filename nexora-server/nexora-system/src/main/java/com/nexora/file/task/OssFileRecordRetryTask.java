package com.nexora.file.task;

import com.nexora.file.entity.SysOssFile;
import com.nexora.file.service.SysOssFileService;
import com.aurora.starter.redis.core.task.DelayedRetryTask;
import com.aurora.starter.redis.model.DelayRetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OssFileRecordRetryTask extends DelayedRetryTask<SysOssFile> {

    static final String TASK_GROUP = "oss:file:record:retry";
    static final int MAX_COUNT = 9;
    static final long INTERVAL_SECONDS = 15L;

    private final SysOssFileService ossFileService;

    public OssFileRecordRetryTask(SysOssFileService ossFileService) {
        this.ossFileService = ossFileService;
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
        if (!ossFileService.saveIfAbsent(data)) {
            throw new IllegalStateException("OSS file record insert returned false: " + data.getFileId());
        }
        return true;
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

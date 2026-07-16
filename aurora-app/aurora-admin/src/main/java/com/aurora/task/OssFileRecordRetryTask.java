package com.aurora.task;

import com.aurora.dto.file.OssFileRecordRetryData;
import com.aurora.service.SysOssFileService;
import com.aurora.starter.redis.core.task.DelayedRetryTask;
import com.aurora.starter.redis.model.DelayRetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OssFileRecordRetryTask extends DelayedRetryTask<OssFileRecordRetryData> {

    static final String TASK_GROUP = "oss:file:record:retry";
    static final int MAX_COUNT = 9;
    static final long INTERVAL_SECONDS = 15L;

    private final SysOssFileService ossFileService;

    public OssFileRecordRetryTask(SysOssFileService ossFileService) {
        this.ossFileService = ossFileService;
    }

    public void submit(OssFileRecordRetryData data) {
        DelayRetry<OssFileRecordRetryData> retry = new DelayRetry<OssFileRecordRetryData>()
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
    protected boolean execute(OssFileRecordRetryData data) {
        if (!ossFileService.saveIfAbsent(data)) {
            throw new IllegalStateException("OSS file record insert returned false: " + data.getFileId());
        }
        return true;
    }

    @Override
    protected void handleException(DelayRetry<OssFileRecordRetryData> task, Exception exception) {
        OssFileRecordRetryData data = task.getData();
        if (task.getCount() >= task.getMaxCount() - 1) {
            log.error("OSS file record retry exhausted, fileId={}, url={}",
                    data.getFileId(), data.getFileUrl(), exception);
            throw new IllegalStateException("OSS file record retry exhausted: " + data.getFileId(), exception);
        }
        log.warn("OSS file record retry failed, fileId={}, count={}", data.getFileId(), task.getCount());
    }
}

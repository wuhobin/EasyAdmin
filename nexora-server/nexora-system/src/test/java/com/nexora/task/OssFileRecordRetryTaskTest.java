package com.nexora.task;

import com.nexora.entity.SysOssFile;
import com.nexora.service.SysOssFileService;
import com.aurora.starter.redis.model.DelayRetry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class OssFileRecordRetryTaskTest {

    @Test
    void submitsFiveAttemptRetryConfigurationWithFifteenSecondInitialDelay() {
        CapturingRetryTask task = new CapturingRetryTask(mock(SysOssFileService.class));
        SysOssFile data = data();

        task.submit(data);

        assertThat(task.delaySeconds).isEqualTo(15L);
        assertThat(task.retry.getData()).isSameAs(data);
        assertThat(task.retry.getMaxCount()).isEqualTo(9);
        assertThat(task.retry.getInterval()).isEqualTo(15L);
        assertThat(task.retry.isUseSameInterval()).isFalse();
        assertThat(task.getTaskGroup()).isEqualTo("oss:file:record:retry");
    }

    @Test
    void delegatesRetryExecutionToTheRecordService() {
        SysOssFileService service = mock(SysOssFileService.class);
        SysOssFile data = data();
        when(service.saveIfAbsent(data)).thenReturn(true);
        OssFileRecordRetryTask task = new OssFileRecordRetryTask(service);

        assertThat(task.execute(data)).isTrue();

        verify(service).saveIfAbsent(data);
    }

    @Test
    void currentStarterCounterProducesFiveAsyncExecutions() {
        SysOssFileService service = mock(SysOssFileService.class);
        SysOssFile data = data();
        when(service.saveIfAbsent(data)).thenThrow(new IllegalStateException("database unavailable"));
        CapturingRetryTask task = new CapturingRetryTask(service);
        task.submit(data);

        for (int attempt = 0; attempt < 4; attempt++) {
            task.consumer(task.retry);
        }
        assertThatThrownBy(() -> task.consumer(task.retry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retry exhausted");

        verify(service, times(5)).saveIfAbsent(data);
        assertThat(task.delays).containsExactly(15L, 30L, 60L, 90L, 120L);
    }

    private static SysOssFile data() {
        return SysOssFile.builder()
                .fileId("file-123")
                .fileUrl("https://oss.example.com/file.png")
                .build();
    }

    private static class CapturingRetryTask extends OssFileRecordRetryTask {

        private DelayRetry<SysOssFile> retry;
        private long delaySeconds;
        private final List<Long> delays = new ArrayList<>();

        private CapturingRetryTask(SysOssFileService service) {
            super(service);
        }

        @Override
        public void producer(DelayRetry<SysOssFile> data, long delay) {
            this.retry = data;
            this.delaySeconds = delay;
            this.delays.add(delay);
        }
    }
}

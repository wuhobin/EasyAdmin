package com.nexora.contract;

/**
 * 检查已存储文件是否仍被业务数据引用。
 */
public interface StoredFileUsageChecker {

    /**
     * @param fileUrl 文件访问地址
     * @return 文件仍被引用时返回 {@code true}
     */
    boolean isInUse(String fileUrl);
}

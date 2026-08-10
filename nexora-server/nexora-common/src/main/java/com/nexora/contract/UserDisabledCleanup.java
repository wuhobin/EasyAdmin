package com.nexora.contract;

/**
 * 用户被禁用后的跨领域清理扩展点。
 */
public interface UserDisabledCleanup {

    /**
     * 清理指定用户仍在运行的临时资源。
     *
     * @param userId 被禁用的用户 ID
     */
    void cleanup(Integer userId);
}

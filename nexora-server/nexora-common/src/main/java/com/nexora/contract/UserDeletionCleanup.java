package com.nexora.contract;

import java.util.List;

/**
 * 用户删除前清理关联数据的扩展点。
 */
public interface UserDeletionCleanup {

    /**
     * 清理指定用户关联的数据。
     *
     * @param userIds 待删除用户 ID
     */
    void cleanup(List<Integer> userIds);
}

package com.nexora.system.api;

import java.util.List;
import java.util.Optional;

/**
 * 系统字典的只读门面。
 */
public interface SystemDictionaryReader {

    /**
     * 查询启用的字典及其启用条目。
     *
     * @param type 字典类型
     * @return 字典不存在或未启用时为空，否则返回按系统顺序排列的条目
     */
    Optional<List<DictionaryEntry>> findEnabledEntries(String type);
}

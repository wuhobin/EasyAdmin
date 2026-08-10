package com.nexora.system.config;

import com.aurora.starter.mybatisplus.mybatis.DynamicCondition;
import com.nexora.system.api.DictionaryEntry;
import com.nexora.system.api.SystemDictionaryReader;
import com.nexora.system.domain.query.SysDictDataQuery;
import com.nexora.system.domain.query.SysDictQuery;
import com.nexora.system.entity.SysDict;
import com.nexora.system.service.SysDictDataService;
import com.nexora.system.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SysDictionaryReader implements SystemDictionaryReader {

    private static final int ENABLED_STATUS = 1;
    private static final String DEFAULT_ENTRY_VALUE = "1";

    private final SysDictService dictService;
    private final SysDictDataService dictDataService;

    @Override
    public Optional<List<DictionaryEntry>> findEnabledEntries(String type) {
        if (type == null) {
            return Optional.empty();
        }
        SysDictQuery dictQuery = new SysDictQuery();
        dictQuery.setType(type);
        dictQuery.setStatus(ENABLED_STATUS);
        SysDict dict = dictService.getOne(DynamicCondition.toWrapper(dictQuery), false);
        if (dict == null) {
            return Optional.empty();
        }
        SysDictDataQuery dataQuery = new SysDictDataQuery();
        dataQuery.setDictId(dict.getId());
        dataQuery.setStatus(ENABLED_STATUS);
        List<DictionaryEntry> entries = dictDataService.listOrdered(dataQuery)
                .stream()
                .map(item -> new DictionaryEntry(
                        item.getLabel(),
                        item.getValue(),
                        DEFAULT_ENTRY_VALUE.equals(item.getIsDefault())))
                .toList();
        return Optional.of(entries);
    }
}

package com.nexora.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexora.system.api.DictionaryEntry;
import com.nexora.system.api.SystemDictionaryReader;
import com.nexora.system.entity.SysDict;
import com.nexora.system.entity.SysDictData;
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
        SysDict dict = dictService.getOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getType, type)
                .eq(SysDict::getStatus, ENABLED_STATUS), false);
        if (dict == null) {
            return Optional.empty();
        }
        List<DictionaryEntry> entries = dictDataService.list(new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictId, dict.getId())
                        .eq(SysDictData::getStatus, ENABLED_STATUS)
                        .orderByAsc(SysDictData::getSort)
                        .orderByAsc(SysDictData::getId))
                .stream()
                .map(item -> new DictionaryEntry(
                        item.getLabel(),
                        item.getValue(),
                        DEFAULT_ENTRY_VALUE.equals(item.getIsDefault())))
                .toList();
        return Optional.of(entries);
    }
}

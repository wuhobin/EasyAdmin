package com.nexora.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexora.system.entity.SysDict;
import com.nexora.system.entity.SysDictData;
import com.nexora.system.service.SysDictDataService;
import com.nexora.system.service.SysDictService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SysDictionaryReaderTest {

    private final SysDictService dictService = mock(SysDictService.class);
    private final SysDictDataService dictDataService = mock(SysDictDataService.class);
    private final SysDictionaryReader reader = new SysDictionaryReader(dictService, dictDataService);

    @Test
    void returnsEmptyWhenEnabledDictionaryDoesNotExist() {
        when(dictService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(null);

        assertThat(reader.findEnabledEntries("mail_provider")).isEmpty();
        verifyNoInteractions(dictDataService);
    }

    @Test
    void mapsInternalDictionaryDataToPublicEntries() {
        SysDict dict = new SysDict();
        dict.setId(10L);
        SysDictData item = new SysDictData();
        item.setLabel("QQ邮箱");
        item.setValue("QQ");
        item.setIsDefault("1");
        when(dictService.getOne(any(LambdaQueryWrapper.class), eq(false))).thenReturn(dict);
        when(dictDataService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        assertThat(reader.findEnabledEntries("mail_provider"))
                .hasValueSatisfying(entries -> assertThat(entries)
                        .singleElement()
                        .satisfies(entry -> {
                            assertThat(entry.label()).isEqualTo("QQ邮箱");
                            assertThat(entry.value()).isEqualTo("QQ");
                            assertThat(entry.defaultEntry()).isTrue();
                        }));
    }
}

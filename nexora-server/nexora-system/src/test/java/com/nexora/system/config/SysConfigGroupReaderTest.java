package com.nexora.system.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.cache.SysConfigGroupCache;
import com.nexora.system.constants.SysConfigGroupEnum;
import com.nexora.system.domain.form.EmailConfigForm;
import com.nexora.system.domain.form.RegisterConfigForm;
import com.nexora.system.service.SysConfigGroupService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysConfigGroupReaderTest {

    private final SysConfigGroupService configService = mock(SysConfigGroupService.class);
    private final SysConfigGroupCache configCache = mock(SysConfigGroupCache.class);
    private final SysConfigGroupRegistry registry = mock(SysConfigGroupRegistry.class);
    private final SysConfigGroupReader reader =
            new SysConfigGroupReader(configService, configCache, registry);

    @Test
    void readsAndParsesAWholeConfigurationGroup() {
        String json = "{\"enabled\":true}";
        RegisterConfigForm expected = new RegisterConfigForm();
        expected.setEnabled(true);
        when(registry.normalizeCode("register")).thenReturn("register");
        when(configCache.get(eq("register"), any())).thenReturn(json);
        when(registry.parse("register", json, RegisterConfigForm.class)).thenReturn(expected);

        RegisterConfigForm result = reader.register();

        assertThat(result).isSameAs(expected);
        verify(configCache).get(eq(SysConfigGroupEnum.REGISTER.getCode()), any());
    }

    @Test
    void rejectsAMissingRequiredGroup() {
        when(registry.normalizeCode("register")).thenReturn("register");
        when(configCache.get(eq("register"), any())).thenReturn(null);

        assertThatThrownBy(() -> reader.read("register", RegisterConfigForm.class))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("register");
    }

    @Test
    void readsTheEmailConfigurationGroup() {
        String json = "{\"enabled\":false}";
        EmailConfigForm expected = new EmailConfigForm();
        expected.setEnabled(false);
        when(registry.normalizeCode("email")).thenReturn("email");
        when(configCache.get(eq("email"), any())).thenReturn(json);
        when(registry.parse("email", json, EmailConfigForm.class)).thenReturn(expected);

        assertThat(reader.email()).isSameAs(expected);
        verify(configCache).get(eq(SysConfigGroupEnum.EMAIL.getCode()), any());
    }
}

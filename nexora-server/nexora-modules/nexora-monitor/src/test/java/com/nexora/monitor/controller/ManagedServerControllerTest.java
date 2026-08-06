package com.nexora.monitor.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.nexora.annotation.OperationLogger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedServerControllerTest {

    @Test
    void protectsTerminalAndFingerprintActionsWithDedicatedPermissions() throws Exception {
        Method ticket = ManagedServerController.class.getMethod(
                "terminalTicket", Long.class,
                com.nexora.monitor.domain.form.TerminalTicketForm.class);
        Method fingerprint = ManagedServerController.class.getMethod(
                "resetFingerprint", Long.class);

        assertThat(ticket.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("monitor:server:terminal");
        assertThat(fingerprint.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("monitor:server:fingerprint");
    }

    @Test
    void doesNotRecordServerOperationsInTheOperationLog() {
        assertThat(ManagedServerController.class.getDeclaredMethods())
                .allSatisfy(method ->
                        assertThat(method.getAnnotation(OperationLogger.class)).isNull());
    }
}

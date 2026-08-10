package com.nexora.handler.onlineuser;

import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineSessionTokenResolverTest {

    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TOKEN_VALUE = "credential-that-must-not-escape";

    private final StpLogic stpLogic = mock(StpLogic.class);
    private final OnlineSessionTokenResolver resolver =
            new OnlineSessionTokenResolver(() -> stpLogic);

    @Test
    void resolvesCurrentPublicSessionIdWithoutReturningToken() {
        when(stpLogic.getTerminalInfo()).thenReturn(terminal());

        assertThat(resolver.currentSessionId()).contains(SESSION_ID);
    }

    @Test
    void ignoresLegacyCurrentTerminalWithoutUuidV4DeviceId() {
        when(stpLogic.getTerminalInfo()).thenReturn(new SaTerminalInfo()
                .setDeviceId("legacy-device")
                .setTokenValue(TOKEN_VALUE));

        assertThat(resolver.currentSessionId()).isEmpty();
    }

    @Test
    void returnsOnlyValidDeviceIdsForTheRequestedUser() {
        SaTerminalInfo valid = terminal();
        SaTerminalInfo invalid = new SaTerminalInfo()
                .setDeviceId("550e8400-e29b-41d4-a716-446655440001")
                .setTokenValue("expired");
        when(stpLogic.getTerminalListByLoginId(7)).thenReturn(List.of(valid, invalid));
        when(stpLogic.getLoginIdByToken(TOKEN_VALUE)).thenReturn("7");
        when(stpLogic.getLoginIdByToken("expired")).thenReturn(null);

        assertThat(resolver.onlineSessionIds(7)).containsExactly(SESSION_ID);
    }

    @Test
    void excludesValidLegacyTerminalsFromOnlineSessionIds() {
        SaTerminalInfo legacy = new SaTerminalInfo()
                .setDeviceId("legacy-device")
                .setTokenValue("legacy-credential");
        when(stpLogic.getTerminalListByLoginId(7)).thenReturn(List.of(terminal(), legacy));
        when(stpLogic.getLoginIdByToken(TOKEN_VALUE)).thenReturn("7");
        when(stpLogic.getLoginIdByToken("legacy-credential")).thenReturn("7");

        assertThat(resolver.onlineSessionIds(7)).containsExactly(SESSION_ID);
    }

    @Test
    void logsOutOnlyTheMatchingValidTerminal() {
        when(stpLogic.getTerminalListByLoginId(7)).thenReturn(List.of(terminal()));
        when(stpLogic.getLoginIdByToken(TOKEN_VALUE)).thenReturn("7");

        assertThat(resolver.logoutSession(7, SESSION_ID)).isTrue();

        verify(stpLogic).logoutByTokenValue(TOKEN_VALUE);
    }

    @Test
    void refusesTerminalOwnedByAnotherUser() {
        when(stpLogic.getTerminalListByLoginId(7)).thenReturn(List.of(terminal()));
        when(stpLogic.getLoginIdByToken(TOKEN_VALUE)).thenReturn("8");

        assertThat(resolver.logoutSession(7, SESSION_ID)).isFalse();
    }

    @Test
    void refusesNonUuidSessionIdBeforeEnumeratingTerminals() {
        assertThat(resolver.logoutSession(7, "legacy-device")).isFalse();

        verify(stpLogic, never()).getTerminalListByLoginId(7);
    }

    private static SaTerminalInfo terminal() {
        return new SaTerminalInfo()
                .setDeviceId(SESSION_ID)
                .setTokenValue(TOKEN_VALUE);
    }
}

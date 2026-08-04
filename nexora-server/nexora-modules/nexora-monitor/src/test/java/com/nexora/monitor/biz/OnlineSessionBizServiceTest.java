package com.nexora.monitor.biz;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.monitor.domain.form.OnlineSessionQueryForm;
import com.nexora.monitor.domain.vo.ForceLogoutResultVo;
import com.nexora.monitor.domain.vo.OnlineSessionVo;
import com.nexora.monitor.infrastructure.IpRegionUtils;
import com.nexora.monitor.infrastructure.OperationLogContext;
import com.nexora.security.session.OnlineSessionRecord;
import com.nexora.security.session.OnlineSessionRegistry;
import com.nexora.security.session.OnlineSessionTokenResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OnlineSessionBizServiceTest {

    private static final String FIRST_SESSION_ID =
            "550e8400-e29b-41d4-a716-446655440000";
    private static final String SECOND_SESSION_ID =
            "550e8400-e29b-41d4-a716-446655440001";
    private static final String THIRD_SESSION_ID =
            "550e8400-e29b-41d4-a716-446655440002";
    private static final LocalDateTime LOGIN_TIME =
            LocalDateTime.of(2026, 8, 4, 10, 0);

    private final OnlineSessionRegistry onlineSessionRegistry =
            mock(OnlineSessionRegistry.class);
    private final OnlineSessionTokenResolver tokenResolver =
            mock(OnlineSessionTokenResolver.class);
    private final OnlineSessionBizService service =
            new OnlineSessionBizService(onlineSessionRegistry, tokenResolver);

    @AfterEach
    void clearOperationLogContext() {
        OperationLogContext.clear();
    }

    @Test
    void returnsTheDefaultPageSortedByLoginTimeAndMarksTheCurrentSession() {
        OnlineSessionRecord first = record(
                FIRST_SESSION_ID, 7, "first@example.com", "First",
                "10.0.0.1", LOGIN_TIME);
        OnlineSessionRecord second = record(
                SECOND_SESSION_ID, 7, "second@example.com", "Second",
                "10.0.0.2", LOGIN_TIME.plusMinutes(1));
        when(onlineSessionRegistry.listSessionIds())
                .thenReturn(List.of(FIRST_SESSION_ID, SECOND_SESSION_ID));
        when(onlineSessionRegistry.findAll(List.of(FIRST_SESSION_ID, SECOND_SESSION_ID)))
                .thenReturn(Map.of(FIRST_SESSION_ID, first, SECOND_SESSION_ID, second));
        when(tokenResolver.onlineSessionIds(7))
                .thenReturn(Set.of(FIRST_SESSION_ID, SECOND_SESSION_ID));
        long firstAccess = toMillis(LOGIN_TIME.plusSeconds(30));
        long secondAccess = toMillis(LOGIN_TIME.plusMinutes(2));
        when(onlineSessionRegistry.findLastAccessTimes(
                List.of(SECOND_SESSION_ID, FIRST_SESSION_ID)))
                .thenReturn(Map.of(
                        FIRST_SESSION_ID, firstAccess,
                        SECOND_SESSION_ID, secondAccess));
        when(tokenResolver.currentSessionId()).thenReturn(Optional.of(SECOND_SESSION_ID));

        try (MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            ipRegion.when(() -> IpRegionUtils.resolve("10.0.0.1"))
                    .thenReturn("Location A");
            ipRegion.when(() -> IpRegionUtils.resolve("10.0.0.2"))
                    .thenReturn("Location B");

            IPage<OnlineSessionVo> page = service.list(new OnlineSessionQueryForm());

            assertThat(page.getCurrent()).isEqualTo(1);
            assertThat(page.getSize()).isEqualTo(10);
            assertThat(page.getTotal()).isEqualTo(2);
            assertThat(page.getRecords())
                    .extracting(OnlineSessionVo::getSessionId)
                    .containsExactly(SECOND_SESSION_ID, FIRST_SESSION_ID);
            assertThat(page.getRecords().getFirst().isCurrentSession()).isTrue();
            assertThat(page.getRecords().getFirst().getLocation()).isEqualTo("Location B");
            assertThat(page.getRecords().getFirst().getLastAccessTime())
                    .isEqualTo(LOGIN_TIME.plusMinutes(2));
        }

        verify(tokenResolver, times(1)).onlineSessionIds(7);
    }

    @Test
    void appliesCaseInsensitiveNicknameSearchAndIpContainsSearch() {
        OnlineSessionRecord matching = record(
                FIRST_SESSION_ID, 7, "owner@example.com", "Alice Admin",
                "203.0.113.8", LOGIN_TIME);
        OnlineSessionRecord wrongIp = record(
                SECOND_SESSION_ID, 8, "alice@example.com", "Alice",
                "198.51.100.2", LOGIN_TIME.plusMinutes(1));
        OnlineSessionRecord emptyNickname = record(
                THIRD_SESSION_ID, 9, "other@example.com", null,
                "203.0.113.9", LOGIN_TIME.plusMinutes(2));
        when(onlineSessionRegistry.listSessionIds())
                .thenReturn(List.of(
                        FIRST_SESSION_ID, SECOND_SESSION_ID, THIRD_SESSION_ID));
        when(onlineSessionRegistry.findAll(List.of(
                FIRST_SESSION_ID, SECOND_SESSION_ID, THIRD_SESSION_ID)))
                .thenReturn(Map.of(
                        FIRST_SESSION_ID, matching,
                        SECOND_SESSION_ID, wrongIp,
                        THIRD_SESSION_ID, emptyNickname));
        when(tokenResolver.onlineSessionIds(7)).thenReturn(Set.of(FIRST_SESSION_ID));
        when(tokenResolver.onlineSessionIds(8)).thenReturn(Set.of(SECOND_SESSION_ID));
        when(tokenResolver.onlineSessionIds(9)).thenReturn(Set.of(THIRD_SESSION_ID));
        when(onlineSessionRegistry.findLastAccessTimes(List.of(FIRST_SESSION_ID)))
                .thenReturn(Map.of());
        when(tokenResolver.currentSessionId()).thenReturn(Optional.empty());
        OnlineSessionQueryForm form = new OnlineSessionQueryForm();
        form.setKeyword("  aDmIn  ");
        form.setIp("203.0.113");

        try (MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            ipRegion.when(() -> IpRegionUtils.resolve("203.0.113.8"))
                    .thenReturn("Location");

            IPage<OnlineSessionVo> page = service.list(form);

            assertThat(page.getTotal()).isEqualTo(1);
            assertThat(page.getRecords())
                    .extracting(OnlineSessionVo::getSessionId)
                    .containsExactly(FIRST_SESSION_ID);
        }
    }

    @Test
    void matchesEmailWhenNicknameIsNull() {
        OnlineSessionRecord record = record(
                FIRST_SESSION_ID, 7, "USER@example.com", null,
                "203.0.113.8", LOGIN_TIME);
        when(onlineSessionRegistry.listSessionIds())
                .thenReturn(List.of(FIRST_SESSION_ID));
        when(onlineSessionRegistry.findAll(List.of(FIRST_SESSION_ID)))
                .thenReturn(Map.of(FIRST_SESSION_ID, record));
        when(tokenResolver.onlineSessionIds(7)).thenReturn(Set.of(FIRST_SESSION_ID));
        when(onlineSessionRegistry.findLastAccessTimes(List.of(FIRST_SESSION_ID)))
                .thenReturn(Map.of());
        when(tokenResolver.currentSessionId()).thenReturn(Optional.empty());
        OnlineSessionQueryForm form = new OnlineSessionQueryForm();
        form.setKeyword(" user@ ");

        try (MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            ipRegion.when(() -> IpRegionUtils.resolve("203.0.113.8"))
                    .thenReturn("Location");

            IPage<OnlineSessionVo> page = service.list(form);

            assertThat(page.getRecords())
                    .extracting(OnlineSessionVo::getSessionId)
                    .containsExactly(FIRST_SESSION_ID);
        }
    }

    @Test
    void removesMissingAndOfflineRecordsButKeepsAnIdleValidSession() {
        OnlineSessionRecord offline = record(
                SECOND_SESSION_ID, 7, "offline@example.com", null,
                "10.0.0.2", LOGIN_TIME);
        OnlineSessionRecord idle = record(
                THIRD_SESSION_ID, 7, "idle@example.com", null,
                "10.0.0.3", LOGIN_TIME.plusMinutes(1));
        List<String> indexedSessionIds =
                List.of(FIRST_SESSION_ID, SECOND_SESSION_ID, THIRD_SESSION_ID);
        when(onlineSessionRegistry.listSessionIds()).thenReturn(indexedSessionIds);
        when(onlineSessionRegistry.findAll(indexedSessionIds))
                .thenReturn(Map.of(SECOND_SESSION_ID, offline, THIRD_SESSION_ID, idle));
        when(tokenResolver.onlineSessionIds(7)).thenReturn(Set.of(THIRD_SESSION_ID));
        when(onlineSessionRegistry.findLastAccessTimes(List.of(THIRD_SESSION_ID)))
                .thenReturn(Map.of());
        when(tokenResolver.currentSessionId()).thenReturn(Optional.empty());

        try (MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            ipRegion.when(() -> IpRegionUtils.resolve("10.0.0.3"))
                    .thenReturn("Location");

            IPage<OnlineSessionVo> page = service.list(new OnlineSessionQueryForm());

            assertThat(page.getRecords())
                    .extracting(OnlineSessionVo::getSessionId)
                    .containsExactly(THIRD_SESSION_ID);
            assertThat(page.getRecords().getFirst().getLastAccessTime())
                    .isEqualTo(idle.loginTime());
        }

        verify(onlineSessionRegistry).removeStaleSessions(new LinkedHashSet<>(
                List.of(FIRST_SESSION_ID, SECOND_SESSION_ID)));
        verify(tokenResolver, times(1)).onlineSessionIds(7);
    }

    @Test
    void returnsTheRequestedPageAfterFilteringAndSorting() {
        OnlineSessionRecord older = record(
                FIRST_SESSION_ID, 7, "older@example.com", null,
                "10.0.0.1", LOGIN_TIME);
        OnlineSessionRecord newer = record(
                SECOND_SESSION_ID, 7, "newer@example.com", null,
                "10.0.0.2", LOGIN_TIME.plusMinutes(1));
        when(onlineSessionRegistry.listSessionIds())
                .thenReturn(List.of(FIRST_SESSION_ID, SECOND_SESSION_ID));
        when(onlineSessionRegistry.findAll(List.of(FIRST_SESSION_ID, SECOND_SESSION_ID)))
                .thenReturn(Map.of(FIRST_SESSION_ID, older, SECOND_SESSION_ID, newer));
        when(tokenResolver.onlineSessionIds(7))
                .thenReturn(Set.of(FIRST_SESSION_ID, SECOND_SESSION_ID));
        when(onlineSessionRegistry.findLastAccessTimes(List.of(FIRST_SESSION_ID)))
                .thenReturn(Map.of());
        when(tokenResolver.currentSessionId()).thenReturn(Optional.empty());
        OnlineSessionQueryForm form = new OnlineSessionQueryForm();
        form.setPageNum(2);
        form.setPageSize(1);

        try (MockedStatic<IpRegionUtils> ipRegion = mockStatic(IpRegionUtils.class)) {
            ipRegion.when(() -> IpRegionUtils.resolve("10.0.0.1"))
                    .thenReturn("Location");

            IPage<OnlineSessionVo> page = service.list(form);

            assertThat(page.getCurrent()).isEqualTo(2);
            assertThat(page.getSize()).isEqualTo(1);
            assertThat(page.getTotal()).isEqualTo(2);
            assertThat(page.getRecords())
                    .extracting(OnlineSessionVo::getSessionId)
                    .containsExactly(FIRST_SESSION_ID);
        }
    }

    @Test
    void forceLogoutInvalidatesOnlyTheSelectedDeviceSession() {
        OnlineSessionRecord target = record(
                FIRST_SESSION_ID, 7, "user@example.com", "User",
                "10.0.0.1", LOGIN_TIME);
        when(onlineSessionRegistry.find(FIRST_SESSION_ID))
                .thenReturn(Optional.of(target));
        when(tokenResolver.currentSessionId())
                .thenReturn(Optional.of(SECOND_SESSION_ID));
        when(tokenResolver.logoutSession(7, FIRST_SESSION_ID))
                .thenReturn(true);

        ForceLogoutResultVo result = service.forceLogout(FIRST_SESSION_ID);

        assertThat(result.getOutcome())
                .isEqualTo(ForceLogoutResultVo.Outcome.LOGGED_OUT);
        assertThat(result.isCurrentSession()).isFalse();
        verify(tokenResolver).logoutSession(7, FIRST_SESSION_ID);
        verify(onlineSessionRegistry).remove(FIRST_SESSION_ID, 7);
        verify(onlineSessionRegistry, never()).removeByUserId(7);
        assertThat(OperationLogContext.parameters())
                .containsEntry("targetUserId", 7)
                .containsEntry("targetEmail", "user@example.com")
                .containsEntry("targetSessionId", FIRST_SESSION_ID)
                .containsEntry("targetIp", "10.0.0.1")
                .containsEntry("outcome", "LOGGED_OUT");
    }

    @Test
    void forceLogoutCalculatesCurrentSessionBeforeInvalidatingIt() {
        OnlineSessionRecord target = record(
                FIRST_SESSION_ID, 7, "user@example.com", "User",
                "10.0.0.1", LOGIN_TIME);
        when(onlineSessionRegistry.find(FIRST_SESSION_ID))
                .thenReturn(Optional.of(target));
        when(tokenResolver.currentSessionId())
                .thenReturn(Optional.of(FIRST_SESSION_ID));
        when(tokenResolver.logoutSession(7, FIRST_SESSION_ID))
                .thenReturn(true);

        ForceLogoutResultVo result = service.forceLogout(
                FIRST_SESSION_ID.toUpperCase());

        assertThat(result.getOutcome())
                .isEqualTo(ForceLogoutResultVo.Outcome.LOGGED_OUT);
        assertThat(result.isCurrentSession()).isTrue();
        verify(tokenResolver).logoutSession(7, FIRST_SESSION_ID);
    }

    @Test
    void repeatedOrRacingForceLogoutReturnsAlreadyOfflineAndCleansState() {
        OnlineSessionRecord target = record(
                FIRST_SESSION_ID, 7, "user@example.com", "User",
                "10.0.0.1", LOGIN_TIME);
        when(onlineSessionRegistry.find(FIRST_SESSION_ID))
                .thenReturn(Optional.of(target));
        when(tokenResolver.currentSessionId()).thenReturn(Optional.empty());
        when(tokenResolver.logoutSession(7, FIRST_SESSION_ID))
                .thenReturn(false);

        ForceLogoutResultVo result = service.forceLogout(FIRST_SESSION_ID);

        assertThat(result.getOutcome())
                .isEqualTo(ForceLogoutResultVo.Outcome.ALREADY_OFFLINE);
        verify(onlineSessionRegistry).remove(FIRST_SESSION_ID, 7);
    }

    @Test
    void missingRegistryRecordReturnsAlreadyOfflineAndCleansStaleIndexes() {
        when(onlineSessionRegistry.find(FIRST_SESSION_ID))
                .thenReturn(Optional.empty());

        ForceLogoutResultVo result = service.forceLogout(FIRST_SESSION_ID);

        assertThat(result.getOutcome())
                .isEqualTo(ForceLogoutResultVo.Outcome.ALREADY_OFFLINE);
        assertThat(result.isCurrentSession()).isFalse();
        verify(onlineSessionRegistry)
                .removeStaleSessions(List.of(FIRST_SESSION_ID));
        verifyNoInteractions(tokenResolver);
        assertThat(OperationLogContext.parameters())
                .containsOnlyKeys("outcome")
                .containsEntry("outcome", "ALREADY_OFFLINE");
    }

    @Test
    void rejectsCredentialsAndNonUuidV4ValuesBeforeRegistryAccess() {
        assertThatThrownBy(() -> service.forceLogout("credential-value"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.forceLogout(
                "550e8400-e29b-11d4-a716-446655440000"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(onlineSessionRegistry, tokenResolver);
    }

    private static OnlineSessionRecord record(
            String sessionId,
            Integer userId,
            String email,
            String nickname,
            String ip,
            LocalDateTime loginTime) {
        return new OnlineSessionRecord(
                sessionId,
                userId,
                email,
                nickname,
                ip,
                "Chrome",
                "Windows",
                loginTime);
    }

    private static long toMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}

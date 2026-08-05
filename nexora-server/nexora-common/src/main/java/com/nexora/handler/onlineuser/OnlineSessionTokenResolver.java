package com.nexora.handler.onlineuser;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;
import com.aurora.starter.security.account.AccountType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Keeps credential-bearing Sa-Token terminal details behind a token-free API.
 */
@Component
public class OnlineSessionTokenResolver {

    private final Supplier<StpLogic> stpLogicSupplier;

    public OnlineSessionTokenResolver() {
        this(() -> SaManager.getStpLogic(AccountType.LOGIN.getCode()));
    }

    OnlineSessionTokenResolver(Supplier<StpLogic> stpLogicSupplier) {
        this.stpLogicSupplier = Objects.requireNonNull(stpLogicSupplier);
    }

    public Optional<String> currentSessionId() {
        SaTerminalInfo terminal = stpLogic().getTerminalInfo();
        return terminal == null
                ? Optional.empty()
                : optionalText(terminal.getDeviceId())
                        .filter(OnlineSessionTokenResolver::isPublicSessionId);
    }

    public long currentTokenTimeout() {
        return stpLogic().getTokenTimeout();
    }

    public Set<String> onlineSessionIds(Object loginId) {
        if (loginId == null) {
            return Set.of();
        }
        List<SaTerminalInfo> terminals = stpLogic().getTerminalListByLoginId(loginId);
        if (terminals == null || terminals.isEmpty()) {
            return Set.of();
        }
        return terminals.stream()
                .filter(terminal -> isValidTerminal(loginId, terminal))
                .map(SaTerminalInfo::getDeviceId)
                .filter(OnlineSessionTokenResolver::isPublicSessionId)
                .collect(Collectors.collectingAndThen(
                        Collectors.toSet(), Collections::unmodifiableSet));
    }

    public boolean isSessionOnline(Object loginId, String sessionId) {
        return findValidTerminal(loginId, sessionId).isPresent();
    }

    /**
     * Logs out exactly one terminal without exposing its credential to callers.
     */
    public boolean logoutSession(Object loginId, String sessionId) {
        Optional<SaTerminalInfo> terminal = findValidTerminal(loginId, sessionId);
        if (terminal.isEmpty()) {
            return false;
        }
        String tokenValue = terminal.get().getTokenValue();
        stpLogic().logoutByTokenValue(tokenValue);
        return true;
    }

    private Optional<SaTerminalInfo> findValidTerminal(Object loginId, String sessionId) {
        if (loginId == null || !isPublicSessionId(sessionId)) {
            return Optional.empty();
        }
        List<SaTerminalInfo> terminals = stpLogic().getTerminalListByLoginId(loginId);
        if (terminals == null) {
            return Optional.empty();
        }
        return terminals.stream()
                .filter(terminal -> sessionId.equals(terminal.getDeviceId()))
                .filter(terminal -> isValidTerminal(loginId, terminal))
                .findFirst();
    }

    private boolean isValidTerminal(Object loginId, SaTerminalInfo terminal) {
        if (terminal == null || terminal.getTokenValue() == null) {
            return false;
        }
        Object resolvedLoginId = stpLogic().getLoginIdByToken(terminal.getTokenValue());
        return resolvedLoginId != null
                && String.valueOf(loginId).equals(String.valueOf(resolvedLoginId));
    }

    private StpLogic stpLogic() {
        return stpLogicSupplier.get();
    }

    private static Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static boolean isPublicSessionId(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID sessionId = UUID.fromString(value);
            return sessionId.version() == 4 && sessionId.toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

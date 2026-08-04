package com.nexora.monitor.biz;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexora.monitor.domain.form.OnlineSessionQueryForm;
import com.nexora.monitor.domain.vo.OnlineSessionVo;
import com.nexora.monitor.infrastructure.IpRegionUtils;
import com.nexora.security.session.OnlineSessionRecord;
import com.nexora.security.session.OnlineSessionRegistry;
import com.nexora.security.session.OnlineSessionTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineSessionBizService {

    private final OnlineSessionRegistry onlineSessionRegistry;
    private final OnlineSessionTokenResolver tokenResolver;

    public IPage<OnlineSessionVo> list(OnlineSessionQueryForm form) {
        QueryCriteria criteria = QueryCriteria.from(form);
        List<String> indexedSessionIds = onlineSessionRegistry.listSessionIds();
        if (indexedSessionIds.isEmpty()) {
            return emptyPage(criteria);
        }

        Map<String, OnlineSessionRecord> records =
                onlineSessionRegistry.findAll(indexedSessionIds);
        Map<Integer, Set<String>> onlineSessionIdsByUser =
                resolveOnlineSessionIdsByUser(records.values());
        LinkedHashSet<String> staleSessionIds = findStaleSessionIds(
                indexedSessionIds, records, onlineSessionIdsByUser);
        if (!staleSessionIds.isEmpty()) {
            onlineSessionRegistry.removeStaleSessions(staleSessionIds);
        }

        List<OnlineSessionRecord> filteredRecords = indexedSessionIds.stream()
                .filter(sessionId -> !staleSessionIds.contains(sessionId))
                .map(records::get)
                .filter(record -> record != null && criteria.matches(record))
                .sorted(Comparator.comparing(OnlineSessionRecord::loginTime).reversed())
                .toList();
        return toPage(filteredRecords, criteria);
    }

    private Map<Integer, Set<String>> resolveOnlineSessionIdsByUser(
            Iterable<OnlineSessionRecord> records) {
        Map<Integer, Set<String>> onlineSessionIdsByUser = new HashMap<>();
        for (OnlineSessionRecord record : records) {
            onlineSessionIdsByUser.computeIfAbsent(
                    record.userId(), tokenResolver::onlineSessionIds);
        }
        return onlineSessionIdsByUser;
    }

    private static LinkedHashSet<String> findStaleSessionIds(
            List<String> indexedSessionIds,
            Map<String, OnlineSessionRecord> records,
            Map<Integer, Set<String>> onlineSessionIdsByUser) {
        LinkedHashSet<String> staleSessionIds = new LinkedHashSet<>();
        for (String sessionId : indexedSessionIds) {
            OnlineSessionRecord record = records.get(sessionId);
            if (record == null
                    || !onlineSessionIdsByUser
                            .getOrDefault(record.userId(), Set.of())
                            .contains(sessionId)) {
                staleSessionIds.add(sessionId);
            }
        }
        return staleSessionIds;
    }

    private IPage<OnlineSessionVo> toPage(
            List<OnlineSessionRecord> records,
            QueryCriteria criteria) {
        Page<OnlineSessionVo> page =
                new Page<>(criteria.pageNum(), criteria.pageSize(), records.size());
        long offset = (criteria.pageNum() - 1L) * criteria.pageSize();
        if (offset >= records.size()) {
            page.setRecords(List.of());
            return page;
        }

        int fromIndex = Math.toIntExact(offset);
        int toIndex = Math.min(fromIndex + criteria.pageSize(), records.size());
        List<OnlineSessionRecord> pageRecords = records.subList(fromIndex, toIndex);
        List<String> pageSessionIds = pageRecords.stream()
                .map(OnlineSessionRecord::sessionId)
                .toList();
        Map<String, Long> lastAccessTimes =
                onlineSessionRegistry.findLastAccessTimes(pageSessionIds);
        Optional<String> currentSessionId = tokenResolver.currentSessionId();

        List<OnlineSessionVo> result = new ArrayList<>(pageRecords.size());
        for (OnlineSessionRecord record : pageRecords) {
            result.add(toVo(record, lastAccessTimes.get(record.sessionId()), currentSessionId));
        }
        page.setRecords(result);
        return page;
    }

    private static IPage<OnlineSessionVo> emptyPage(QueryCriteria criteria) {
        return new Page<>(criteria.pageNum(), criteria.pageSize(), 0);
    }

    private static OnlineSessionVo toVo(
            OnlineSessionRecord record,
            Long lastAccessTime,
            Optional<String> currentSessionId) {
        OnlineSessionVo vo = new OnlineSessionVo();
        vo.setSessionId(record.sessionId());
        vo.setEmail(record.email());
        vo.setNickname(record.nickname());
        vo.setIp(record.ip());
        vo.setLocation(IpRegionUtils.resolve(record.ip()));
        vo.setBrowser(record.browser());
        vo.setOs(record.os());
        vo.setLoginTime(record.loginTime());
        vo.setLastAccessTime(lastAccessTime == null
                ? record.loginTime()
                : LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(lastAccessTime),
                        ZoneId.systemDefault()));
        vo.setCurrentSession(currentSessionId
                .map(record.sessionId()::equals)
                .orElse(false));
        return vo;
    }

    private record QueryCriteria(
            String keyword,
            String ip,
            int pageNum,
            int pageSize) {

        private static QueryCriteria from(OnlineSessionQueryForm form) {
            OnlineSessionQueryForm source =
                    form == null ? new OnlineSessionQueryForm() : form;
            int pageNum = source.getPageNum() == null
                    ? PageParam.DEFAULT_PAGE
                    : source.getPageNum();
            int pageSize = source.getPageSize() == null
                    ? PageParam.DEFAULT_SIZE
                    : source.getPageSize();
            return new QueryCriteria(
                    normalize(source.getKeyword()),
                    normalize(source.getIp()),
                    pageNum,
                    pageSize);
        }

        private boolean matches(OnlineSessionRecord record) {
            return matchesKeyword(record) && matchesIp(record);
        }

        private boolean matchesKeyword(OnlineSessionRecord record) {
            if (keyword == null) {
                return true;
            }
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            return containsIgnoreCase(record.email(), normalizedKeyword)
                    || containsIgnoreCase(record.nickname(), normalizedKeyword);
        }

        private boolean matchesIp(OnlineSessionRecord record) {
            return ip == null || record.ip().contains(ip);
        }

        private static boolean containsIgnoreCase(String value, String normalizedKeyword) {
            return value != null
                    && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
        }

        private static String normalize(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}

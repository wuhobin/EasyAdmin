package com.nexora.identity.infrastructure;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentInfo;
import cn.hutool.http.useragent.UserAgentUtil;
import com.aurora.starter.webmvc.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves credential-free client metadata for a newly authenticated session.
 */
@Component
public class LoginClientInfoResolver {

    private static final String USER_AGENT_HEADER = "User-Agent";

    public ClientInfo resolve() {
        return resolve(ServletUtils.getRequest());
    }

    ClientInfo resolve(HttpServletRequest request) {
        String ip = ServletUtils.getClientIp(request);
        String userAgentValue = ServletUtils.getHeader(request, USER_AGENT_HEADER);
        if (userAgentValue == null || userAgentValue.isBlank()) {
            return new ClientInfo(ip, null, null);
        }
        try {
            UserAgent userAgent = UserAgentUtil.parse(userAgentValue);
            if (userAgent == null) {
                return new ClientInfo(ip, null, null);
            }
            return new ClientInfo(
                    ip,
                    format(userAgent.getBrowser(), userAgent.getVersion()),
                    format(userAgent.getOs(), userAgent.getOsVersion()));
        } catch (RuntimeException exception) {
            return new ClientInfo(ip, null, null);
        }
    }

    private static String format(UserAgentInfo info, String version) {
        if (info == null || info.isUnknown()) {
            return null;
        }
        return version == null || version.isBlank()
                ? info.getName()
                : info.getName() + " " + version;
    }

    public record ClientInfo(String ip, String browser, String os) {
    }
}

package com.nexora.monitor.infrastructure.serverssh;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.monitor.constants.ServerConstants;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@Component
public class SshTargetValidator {

    private static final Set<String> CONTAINER_HOST_ALIASES = Set.of(
            "host.docker.internal",
            "gateway.docker.internal",
            "host.containers.internal");

    private final Set<InetAddress> localAddresses = loadLocalAddresses();

    public InetAddress resolveAllowedAddress(String host) {
        String normalizedHost = normalizeHost(host);
        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
            if (addresses.length == 0) {
                throw new BizException(ServerConstants.SSH_TARGET_RESOLVE_FAILED_MESSAGE);
            }
            for (InetAddress address : addresses) {
                if (isForbidden(address)) {
                    throw new BizException(ServerConstants.SSH_TARGET_FORBIDDEN_MESSAGE);
                }
            }
            return addresses[0];
        } catch (UnknownHostException exception) {
            throw new BizException(ServerConstants.SSH_TARGET_RESOLVE_FAILED_MESSAGE);
        }
    }

    private boolean isForbidden(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()
                || localAddresses.contains(address)) {
            return true;
        }
        if (address instanceof Inet4Address) {
            return isReservedIpv4(address.getAddress());
        }
        if (address instanceof Inet6Address) {
            return isReservedIpv6(address.getAddress());
        }
        return false;
    }

    private static boolean isReservedIpv4(byte[] address) {
        return hasPrefix(address, new int[]{0}, 8)
                || hasPrefix(address, new int[]{100, 64}, 10)
                || hasPrefix(address, new int[]{192, 0, 0}, 24)
                || hasPrefix(address, new int[]{192, 0, 2}, 24)
                || hasPrefix(address, new int[]{198, 18}, 15)
                || hasPrefix(address, new int[]{198, 51, 100}, 24)
                || hasPrefix(address, new int[]{203, 0, 113}, 24)
                || hasPrefix(address, new int[]{224}, 4);
    }

    private static boolean isReservedIpv6(byte[] address) {
        return hasPrefix(address, new int[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, 64)
                || hasPrefix(address, new int[]{0x20, 0x01, 0x0d, 0xb8}, 32)
                || hasPrefix(address, new int[]{0xfe, 0xc0}, 10);
    }

    private static boolean hasPrefix(byte[] address, int[] prefixBytes, int prefixBits) {
        int fullBytes = prefixBits / 8;
        int remainingBits = prefixBits % 8;
        for (int index = 0; index < fullBytes; index++) {
            if ((address[index] & 0xff) != prefixBytes[index]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xff << (8 - remainingBits);
        return ((address[fullBytes] & 0xff) & mask)
                == (prefixBytes[fullBytes] & mask);
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            throw new BizException(ServerConstants.SSH_TARGET_RESOLVE_FAILED_MESSAGE);
        }
        String normalized = host.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static Set<InetAddress> loadLocalAddresses() {
        Set<InetAddress> result = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return result;
            }
            while (interfaces.hasMoreElements()) {
                Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
                while (addresses.hasMoreElements()) {
                    result.add(addresses.nextElement());
                }
            }
        } catch (SocketException ignored) {
            // The per-address safety checks still apply when local interfaces cannot be enumerated.
        }
        CONTAINER_HOST_ALIASES.forEach(alias -> addResolvedAddresses(result, alias));
        return result;
    }

    private static void addResolvedAddresses(Set<InetAddress> addresses, String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                addresses.add(address);
            }
        } catch (UnknownHostException ignored) {
            // The alias is platform-specific and may not exist outside a container runtime.
        }
    }
}

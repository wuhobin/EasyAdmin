package com.aurora.utils;

import com.aurora.common.Constants;
import com.aurora.starter.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.IOException;
import java.io.InputStream;

/**
 * Resolves IP addresses with the bundled ip2region database.
 */
@Slf4j
public final class IpRegionUtils {

    private static final String DATABASE = "/ip2region.xdb";
    private static final Searcher SEARCHER = createSearcher();

    private IpRegionUtils() {
    }

    public static String resolve(String ip) {
        if (StringUtils.isBlank(ip) || SEARCHER == null) {
            return Constants.UNKNOWN;
        }
        try {
            String region = SEARCHER.search(ip);
            if (StringUtils.isBlank(region)) {
                return Constants.UNKNOWN;
            }
            return region.replace("|0", "").replace("0|", "");
        } catch (Exception e) {
            log.debug("Unable to resolve IP region: {}", ip, e);
            return Constants.UNKNOWN;
        }
    }

    private static Searcher createSearcher() {
        try (InputStream input = IpRegionUtils.class.getResourceAsStream(DATABASE)) {
            if (input == null) {
                log.error("ip2region database not found: {}", DATABASE);
                return null;
            }
            return Searcher.newWithBuffer(input.readAllBytes());
        } catch (IOException e) {
            log.error("Unable to load ip2region database: {}", DATABASE, e);
            return null;
        }
    }
}

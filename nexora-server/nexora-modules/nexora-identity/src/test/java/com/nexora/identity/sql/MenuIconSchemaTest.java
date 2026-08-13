package com.nexora.identity.sql;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MenuIconSchemaTest {

    private static final Pattern INITIALIZATION_ICON_PATTERN = Pattern.compile(
            "INSERT INTO `sys_menu` VALUES \\((\\d+), [^\\r\\n]*?, '([^']*)', '(?:CATALOG|MENU)'");
    private static final Pattern MIGRATION_ICON_PATTERN = Pattern.compile(
            "WHEN (\\d+) THEN '([^']+)'");

    @Test
    void initializationAndMigrationUseAntDesignIconsForEverySeededMenu() throws Exception {
        Path repositoryRoot = repositoryRoot();
        String initializationSql = Files.readString(
                repositoryRoot.resolve("nexora-admin.sql"), StandardCharsets.UTF_8);
        String migrationSql = Files.readString(
                repositoryRoot.resolve("deploy/migrations/20260811_menu_icons_ant_design.sql"),
                StandardCharsets.UTF_8);

        Map<Integer, String> expectedIcons = Map.ofEntries(
                Map.entry(1, "antd:SettingOutlined"),
                Map.entry(2, "antd:TeamOutlined"),
                Map.entry(4, "antd:MonitorOutlined"),
                Map.entry(9, "antd:DatabaseOutlined"),
                Map.entry(13, "antd:MenuOutlined"),
                Map.entry(14, "antd:UserOutlined"),
                Map.entry(18, "antd:ScheduleOutlined"),
                Map.entry(32, "antd:FileTextOutlined"),
                Map.entry(33, "antd:AuditOutlined"),
                Map.entry(54, "antd:IdcardOutlined"),
                Map.entry(111, "antd:FolderOpenOutlined"),
                Map.entry(112, "antd:FileOutlined"),
                Map.entry(117, "antd:MailOutlined"),
                Map.entry(118, "antd:InboxOutlined"),
                Map.entry(127, "antd:AccountBookOutlined"),
                Map.entry(130, "antd:ToolOutlined"),
                Map.entry(135, "antd:HistoryOutlined"),
                Map.entry(139, "antd:TeamOutlined"),
                Map.entry(142, "antd:CloudServerOutlined"),
                Map.entry(150, "antd:BellOutlined"));

        Map<Integer, String> initializationIcons = extractIcons(
                initializationSql, INITIALIZATION_ICON_PATTERN);
        Map<Integer, String> migrationIcons = extractIcons(
                migrationSql, MIGRATION_ICON_PATTERN);

        assertThat(initializationIcons).containsExactlyInAnyOrderEntriesOf(expectedIcons);
        assertThat(initializationIcons.values()).allMatch(icon -> icon.startsWith("antd:"));
        assertThat(migrationIcons).containsExactlyInAnyOrderEntriesOf(expectedIcons);
    }

    private static Map<Integer, String> extractIcons(String sql, Pattern pattern) {
        Map<Integer, String> icons = new LinkedHashMap<>();
        var matcher = pattern.matcher(sql);
        while (matcher.find()) {
            icons.put(Integer.parseInt(matcher.group(1)), matcher.group(2));
        }
        return icons;
    }

    private static Path repositoryRoot() {
        Path currentPath = Path.of("").toAbsolutePath().normalize();
        while (currentPath != null) {
            if (Files.isRegularFile(currentPath.resolve("nexora-admin.sql"))
                    && Files.isDirectory(currentPath.resolve("deploy"))) {
                return currentPath;
            }
            currentPath = currentPath.getParent();
        }
        throw new IllegalStateException("Unable to locate the repository root");
    }
}

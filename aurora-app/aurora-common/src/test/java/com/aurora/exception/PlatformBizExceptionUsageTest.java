package com.aurora.exception;

import com.aurora.common.ResultCode;
import com.aurora.starter.webmvc.enums.DefaultBizCode;
import com.aurora.starter.webmvc.exception.BizCode;
import com.aurora.starter.webmvc.exception.BizException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformBizExceptionUsageTest {

    @Test
    void preservesEasyAdminBusinessCodesAndMessages() {
        BizException codedException = new BizException(ResultCode.ERROR_PASSWORD);
        BizException messageException = new BizException("用户不存在");

        assertThat(codedException.getCode()).isEqualTo(ResultCode.ERROR_PASSWORD.code);
        assertThat(codedException.getMessage()).isEqualTo(ResultCode.ERROR_PASSWORD.desc);
        assertThat(ResultCode.ERROR_PASSWORD).isInstanceOf(BizCode.class);
        assertThat(messageException.getCode()).isEqualTo(DefaultBizCode.SERVER_ERROR.getCode());
        assertThat(messageException.getMessage()).isEqualTo("用户不存在");
    }

    @Test
    void productionSourcesUsePlatformBusinessExceptionOnly() throws IOException {
        Path appRoot = findAuroraAppRoot();
        Path serviceImplementationRoot = appRoot.resolve(
                "aurora-admin/src/main/java/com/aurora/service/impl");
        Path localBusinessException = appRoot.resolve(
                "aurora-common/src/main/java/com/aurora/exception/BusinessException.java");

        assertThat(localBusinessException).doesNotExist();

        List<String> violations = new ArrayList<>();
        for (String module : List.of("aurora-common", "aurora-admin", "aurora-server")) {
            Path sourceRoot = appRoot.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> sources = Files.walk(sourceRoot)) {
                for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(source);
                    if (content.contains("com.aurora.exception.BusinessException")
                            || content.contains("new BusinessException(")
                            || (source.startsWith(serviceImplementationRoot)
                            && content.contains("throw new RuntimeException("))) {
                        violations.add(appRoot.relativize(source).toString());
                    }
                }
            }
        }

        assertThat(violations)
                .as("production sources must use platform BizException for business failures")
                .isEmpty();
    }

    private static Path findAuroraAppRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (isAuroraAppRoot(candidate)) {
                return candidate;
            }
            Path nestedApp = candidate.resolve("aurora-app");
            if (isAuroraAppRoot(nestedApp)) {
                return nestedApp;
            }
        }
        throw new IllegalStateException("Cannot locate aurora-app root from " + current);
    }

    private static boolean isAuroraAppRoot(Path candidate) {
        return Files.isRegularFile(candidate.resolve("aurora-common/pom.xml"))
                && Files.isRegularFile(candidate.resolve("aurora-admin/pom.xml"));
    }
}

package com.nexora.exception;

import com.nexora.constants.ResultCode;
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
    void preservesNexoraBusinessCodesAndMessages() {
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
        Path appRoot = findNexoraServerRoot();
        Path serviceImplementationRoot = appRoot.resolve(
                "nexora-system/src/main/java/com/nexora/service/impl");
        Path localBusinessException = appRoot.resolve(
                "nexora-common/src/main/java/com/nexora/exception/BusinessException.java");

        assertThat(localBusinessException).doesNotExist();

        List<String> violations = new ArrayList<>();
        for (String module : List.of("nexora-common", "nexora-system", "nexora-boot")) {
            Path sourceRoot = appRoot.resolve(module).resolve("src/main/java");
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try (Stream<Path> sources = Files.walk(sourceRoot)) {
                for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(source);
                    if (content.contains("com.nexora.exception.BusinessException")
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

    private static Path findNexoraServerRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (isNexoraServerRoot(candidate)) {
                return candidate;
            }
            Path nestedServer = candidate.resolve("nexora-server");
            if (isNexoraServerRoot(nestedServer)) {
                return nestedServer;
            }
        }
        throw new IllegalStateException("Cannot locate nexora-server root from " + current);
    }

    private static boolean isNexoraServerRoot(Path candidate) {
        return Files.isRegularFile(candidate.resolve("nexora-common/pom.xml"))
                && Files.isRegularFile(candidate.resolve("nexora-system/pom.xml"));
    }
}

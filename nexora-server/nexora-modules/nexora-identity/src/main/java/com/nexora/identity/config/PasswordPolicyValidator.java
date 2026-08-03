package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.constants.CommonConstants;
import com.nexora.system.api.PasswordSettings;
import com.nexora.system.api.SystemConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class PasswordPolicyValidator {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final SystemConfigReader configReader;

    public String validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BizException(CommonConstants.PASSWORD_REQUIRED_MESSAGE);
        }
        PasswordSettings policy = configReader.password();
        int length = password.codePointCount(0, password.length());
        if (length < policy.getMinLength() || length > policy.getMaxLength()) {
            throw new BizException(CommonConstants.PASSWORD_DYNAMIC_LENGTH_INVALID_MESSAGE.formatted(
                    policy.getMinLength(), policy.getMaxLength()));
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BizException(CommonConstants.PASSWORD_BCRYPT_BYTES_INVALID_MESSAGE);
        }
        if (Boolean.TRUE.equals(policy.getRequireUppercase())
                && password.codePoints().noneMatch(Character::isUpperCase)) {
            throw new BizException(CommonConstants.PASSWORD_UPPERCASE_REQUIRED_MESSAGE);
        }
        if (Boolean.TRUE.equals(policy.getRequireLowercase())
                && password.codePoints().noneMatch(Character::isLowerCase)) {
            throw new BizException(CommonConstants.PASSWORD_LOWERCASE_REQUIRED_MESSAGE);
        }
        if (Boolean.TRUE.equals(policy.getRequireNumber())
                && password.codePoints().noneMatch(Character::isDigit)) {
            throw new BizException(CommonConstants.PASSWORD_NUMBER_REQUIRED_MESSAGE);
        }
        if (Boolean.TRUE.equals(policy.getRequireSpecial())
                && password.codePoints().noneMatch(codePoint -> !Character.isLetterOrDigit(codePoint))) {
            throw new BizException(CommonConstants.PASSWORD_SPECIAL_REQUIRED_MESSAGE);
        }
        return password;
    }
}

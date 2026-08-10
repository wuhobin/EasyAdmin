package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.infrastructure.PasswordPolicyValidator;
import com.nexora.system.api.SystemConfigReader;
import com.nexora.identity.constants.IdentityConstants;
import com.nexora.system.api.PasswordSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordPolicyValidatorTest {

    private final SystemConfigReader configReader = mock(SystemConfigReader.class);
    private final PasswordPolicyValidator passwordValidator = new PasswordPolicyValidator(configReader);
    private PasswordSettings policy;

    @BeforeEach
    void setUp() {
        policy = policy(8, 64, true, true, true, true);
        when(configReader.password()).thenReturn(policy);
    }

    @Test
    void acceptsAUnicodePasswordByCodePointLength() {
        policy.setMaxLength(9);

        assertThat(passwordValidator.validateNewPassword("A密bcde1!😀"))
                .isEqualTo("A密bcde1!😀");
    }

    @Test
    void enforcesAllConfiguredCharacterClasses() {
        assertInvalid("abcdef1!", IdentityConstants.PASSWORD_UPPERCASE_REQUIRED_MESSAGE);
        assertInvalid("ABCDEF1!", IdentityConstants.PASSWORD_LOWERCASE_REQUIRED_MESSAGE);
        assertInvalid("Abcdefg!", IdentityConstants.PASSWORD_NUMBER_REQUIRED_MESSAGE);
        assertInvalid("Abcdef12", IdentityConstants.PASSWORD_SPECIAL_REQUIRED_MESSAGE);
    }

    @Test
    void rejectsPasswordsOutsideTheConfiguredLengthRange() {
        assertInvalid("Aa1!", "密码长度必须在8到64个字符之间");
    }

    @Test
    void rejectsPasswordsLongerThanTheBcryptUtf8Limit() {
        String password = "Aa1!" + "密".repeat(23);

        assertInvalid(password, IdentityConstants.PASSWORD_BCRYPT_BYTES_INVALID_MESSAGE);
    }

    @Test
    void rejectsBlankPasswordsBeforeReadingThePolicy() {
        assertThatThrownBy(() -> passwordValidator.validateNewPassword("  "))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(IdentityConstants.PASSWORD_REQUIRED_MESSAGE);
    }

    private void assertInvalid(String password, String message) {
        assertThatThrownBy(() -> passwordValidator.validateNewPassword(password))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(message);
    }

    private static PasswordSettings policy(
            int minLength, int maxLength, boolean uppercase, boolean lowercase,
            boolean number, boolean special) {
        PasswordSettings policy = new PasswordSettings();
        policy.setMinLength(minLength);
        policy.setMaxLength(maxLength);
        policy.setRequireUppercase(uppercase);
        policy.setRequireLowercase(lowercase);
        policy.setRequireNumber(number);
        policy.setRequireSpecial(special);
        return policy;
    }
}

package com.nexora.identity.config;

import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.system.config.SysConfigGroupReader;
import com.nexora.constants.CommonConstants;
import com.nexora.system.domain.form.PasswordConfigForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordPolicyValidatorTest {

    private final SysConfigGroupReader configReader = mock(SysConfigGroupReader.class);
    private final PasswordPolicyValidator passwordValidator = new PasswordPolicyValidator(configReader);
    private PasswordConfigForm policy;

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
        assertInvalid("abcdef1!", CommonConstants.PASSWORD_UPPERCASE_REQUIRED_MESSAGE);
        assertInvalid("ABCDEF1!", CommonConstants.PASSWORD_LOWERCASE_REQUIRED_MESSAGE);
        assertInvalid("Abcdefg!", CommonConstants.PASSWORD_NUMBER_REQUIRED_MESSAGE);
        assertInvalid("Abcdef12", CommonConstants.PASSWORD_SPECIAL_REQUIRED_MESSAGE);
    }

    @Test
    void rejectsPasswordsOutsideTheConfiguredLengthRange() {
        assertInvalid("Aa1!", "密码长度必须在8到64个字符之间");
    }

    @Test
    void rejectsPasswordsLongerThanTheBcryptUtf8Limit() {
        String password = "Aa1!" + "密".repeat(23);

        assertInvalid(password, CommonConstants.PASSWORD_BCRYPT_BYTES_INVALID_MESSAGE);
    }

    @Test
    void rejectsBlankPasswordsBeforeReadingThePolicy() {
        assertThatThrownBy(() -> passwordValidator.validateNewPassword("  "))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(CommonConstants.PASSWORD_REQUIRED_MESSAGE);
    }

    private void assertInvalid(String password, String message) {
        assertThatThrownBy(() -> passwordValidator.validateNewPassword(password))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(message);
    }

    private static PasswordConfigForm policy(
            int minLength, int maxLength, boolean uppercase, boolean lowercase,
            boolean number, boolean special) {
        PasswordConfigForm policy = new PasswordConfigForm();
        policy.setMinLength(minLength);
        policy.setMaxLength(maxLength);
        policy.setRequireUppercase(uppercase);
        policy.setRequireLowercase(lowercase);
        policy.setRequireNumber(number);
        policy.setRequireSpecial(special);
        return policy;
    }
}

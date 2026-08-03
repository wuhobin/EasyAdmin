package com.nexora.identity.utils;

import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.scene.VerificationScene;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationMailTemplateUtilsTest {

    @Test
    void rendersCopyForEveryCommonScene() {
        assertScene(CommonVerificationScene.REGISTER, "注册验证", "账号注册");
        assertScene(CommonVerificationScene.LOGIN, "登录验证", "账号登录");
        assertScene(CommonVerificationScene.RESET_PASSWORD, "忘记密码验证", "忘记密码");
        assertScene(CommonVerificationScene.CHANGE_EMAIL, "邮箱换绑验证", "邮箱换绑");
    }

    @Test
    void rendersGenericCopyForCustomScene() {
        VerificationScene customScene = () -> "CUSTOM";

        MailVerificationSendRequest request =
                VerificationMailTemplateUtils.createRequest("user@example.com", customScene);

        assertThat(request.subject()).isEqualTo("Nexora Admin 邮箱验证码");
        assertThat(request.content()).contains("邮箱验证", "身份验证");
        assertCommonTemplate(request);
    }

    private static void assertScene(CommonVerificationScene scene, String title, String sceneName) {
        MailVerificationSendRequest request =
                VerificationMailTemplateUtils.createRequest("user@example.com", scene);

        assertThat(request.scene()).isEqualTo(scene);
        assertThat(request.subject()).contains("Nexora Admin", "验证码");
        assertThat(request.content()).contains(title, sceneName);
        assertCommonTemplate(request);
    }

    private static void assertCommonTemplate(MailVerificationSendRequest request) {
        assertThat(request.contentType()).isEqualTo(MailContentType.HTML);
        assertThat(request.content())
                .contains("<!doctype html>", "{code}", "{expireMinutes}", "安全提示")
                .doesNotContain(
                        "{verificationTitle}",
                        "{verificationDescription}",
                        "{verificationScene}",
                        "{verificationGuide}");
    }
}

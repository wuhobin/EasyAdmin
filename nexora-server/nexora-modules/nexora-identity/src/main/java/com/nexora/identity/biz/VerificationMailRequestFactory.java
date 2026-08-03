package com.nexora.identity.biz;

import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.scene.VerificationScene;
import com.nexora.identity.constants.IdentityConstants;

public final class VerificationMailRequestFactory {

    private VerificationMailRequestFactory() {
    }

    public static MailVerificationSendRequest createRequest(String email, VerificationScene scene) {
        SceneCopy sceneCopy = resolveSceneCopy(scene);
        String content = IdentityConstants.VERIFICATION_EMAIL_TEMPLATE
                .replace(IdentityConstants.VERIFICATION_TITLE_PLACEHOLDER, sceneCopy.title())
                .replace(IdentityConstants.VERIFICATION_DESCRIPTION_PLACEHOLDER, sceneCopy.description())
                .replace(IdentityConstants.VERIFICATION_SCENE_PLACEHOLDER, sceneCopy.sceneName())
                .replace(IdentityConstants.VERIFICATION_GUIDE_PLACEHOLDER, sceneCopy.guide());
        return new MailVerificationSendRequest(
                email, scene, sceneCopy.subject(), content, MailContentType.HTML);
    }

    private static SceneCopy resolveSceneCopy(VerificationScene scene) {
        if (!(scene instanceof CommonVerificationScene commonScene)) {
            return defaultSceneCopy();
        }
        return switch (commonScene) {
            case REGISTER -> new SceneCopy(
                    IdentityConstants.REGISTER_EMAIL_SUBJECT,
                    IdentityConstants.REGISTER_VERIFICATION_TITLE,
                    IdentityConstants.REGISTER_VERIFICATION_DESCRIPTION,
                    IdentityConstants.REGISTER_VERIFICATION_SCENE,
                    IdentityConstants.REGISTER_VERIFICATION_GUIDE);
            case LOGIN -> new SceneCopy(
                    IdentityConstants.LOGIN_EMAIL_SUBJECT,
                    IdentityConstants.LOGIN_VERIFICATION_TITLE,
                    IdentityConstants.LOGIN_VERIFICATION_DESCRIPTION,
                    IdentityConstants.LOGIN_VERIFICATION_SCENE,
                    IdentityConstants.LOGIN_VERIFICATION_GUIDE);
            case RESET_PASSWORD -> new SceneCopy(
                    IdentityConstants.RESET_PASSWORD_EMAIL_SUBJECT,
                    IdentityConstants.RESET_PASSWORD_VERIFICATION_TITLE,
                    IdentityConstants.RESET_PASSWORD_VERIFICATION_DESCRIPTION,
                    IdentityConstants.RESET_PASSWORD_VERIFICATION_SCENE,
                    IdentityConstants.RESET_PASSWORD_VERIFICATION_GUIDE);
            case CHANGE_EMAIL -> new SceneCopy(
                    IdentityConstants.CHANGE_EMAIL_SUBJECT,
                    IdentityConstants.CHANGE_EMAIL_VERIFICATION_TITLE,
                    IdentityConstants.CHANGE_EMAIL_VERIFICATION_DESCRIPTION,
                    IdentityConstants.CHANGE_EMAIL_VERIFICATION_SCENE,
                    IdentityConstants.CHANGE_EMAIL_VERIFICATION_GUIDE);
        };
    }

    private static SceneCopy defaultSceneCopy() {
        return new SceneCopy(
                IdentityConstants.DEFAULT_VERIFICATION_EMAIL_SUBJECT,
                IdentityConstants.DEFAULT_VERIFICATION_TITLE,
                IdentityConstants.DEFAULT_VERIFICATION_DESCRIPTION,
                IdentityConstants.DEFAULT_VERIFICATION_SCENE,
                IdentityConstants.DEFAULT_VERIFICATION_GUIDE);
    }

    private record SceneCopy(String subject, String title, String description, String sceneName, String guide) {
    }
}

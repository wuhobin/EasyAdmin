package com.nexora.identity.biz;

import com.aurora.starter.verification.mail.MailContentType;
import com.aurora.starter.verification.mail.MailVerificationSendRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.verification.scene.VerificationScene;
import com.nexora.constants.CommonConstants;

public final class VerificationMailRequestFactory {

    private VerificationMailRequestFactory() {
    }

    public static MailVerificationSendRequest createRequest(String email, VerificationScene scene) {
        SceneCopy sceneCopy = resolveSceneCopy(scene);
        String content = CommonConstants.VERIFICATION_EMAIL_TEMPLATE
                .replace(CommonConstants.VERIFICATION_TITLE_PLACEHOLDER, sceneCopy.title())
                .replace(CommonConstants.VERIFICATION_DESCRIPTION_PLACEHOLDER, sceneCopy.description())
                .replace(CommonConstants.VERIFICATION_SCENE_PLACEHOLDER, sceneCopy.sceneName())
                .replace(CommonConstants.VERIFICATION_GUIDE_PLACEHOLDER, sceneCopy.guide());
        return new MailVerificationSendRequest(
                email, scene, sceneCopy.subject(), content, MailContentType.HTML);
    }

    private static SceneCopy resolveSceneCopy(VerificationScene scene) {
        if (!(scene instanceof CommonVerificationScene commonScene)) {
            return defaultSceneCopy();
        }
        return switch (commonScene) {
            case REGISTER -> new SceneCopy(
                    CommonConstants.REGISTER_EMAIL_SUBJECT,
                    CommonConstants.REGISTER_VERIFICATION_TITLE,
                    CommonConstants.REGISTER_VERIFICATION_DESCRIPTION,
                    CommonConstants.REGISTER_VERIFICATION_SCENE,
                    CommonConstants.REGISTER_VERIFICATION_GUIDE);
            case LOGIN -> new SceneCopy(
                    CommonConstants.LOGIN_EMAIL_SUBJECT,
                    CommonConstants.LOGIN_VERIFICATION_TITLE,
                    CommonConstants.LOGIN_VERIFICATION_DESCRIPTION,
                    CommonConstants.LOGIN_VERIFICATION_SCENE,
                    CommonConstants.LOGIN_VERIFICATION_GUIDE);
            case RESET_PASSWORD -> new SceneCopy(
                    CommonConstants.RESET_PASSWORD_EMAIL_SUBJECT,
                    CommonConstants.RESET_PASSWORD_VERIFICATION_TITLE,
                    CommonConstants.RESET_PASSWORD_VERIFICATION_DESCRIPTION,
                    CommonConstants.RESET_PASSWORD_VERIFICATION_SCENE,
                    CommonConstants.RESET_PASSWORD_VERIFICATION_GUIDE);
            case CHANGE_EMAIL -> new SceneCopy(
                    CommonConstants.CHANGE_EMAIL_SUBJECT,
                    CommonConstants.CHANGE_EMAIL_VERIFICATION_TITLE,
                    CommonConstants.CHANGE_EMAIL_VERIFICATION_DESCRIPTION,
                    CommonConstants.CHANGE_EMAIL_VERIFICATION_SCENE,
                    CommonConstants.CHANGE_EMAIL_VERIFICATION_GUIDE);
        };
    }

    private static SceneCopy defaultSceneCopy() {
        return new SceneCopy(
                CommonConstants.DEFAULT_VERIFICATION_EMAIL_SUBJECT,
                CommonConstants.DEFAULT_VERIFICATION_TITLE,
                CommonConstants.DEFAULT_VERIFICATION_DESCRIPTION,
                CommonConstants.DEFAULT_VERIFICATION_SCENE,
                CommonConstants.DEFAULT_VERIFICATION_GUIDE);
    }

    private record SceneCopy(String subject, String title, String description, String sceneName, String guide) {
    }
}

package com.nexora.identity.biz;

import com.aurora.starter.verification.exception.VerificationCooldownException;
import com.aurora.starter.verification.exception.VerificationException;
import com.aurora.starter.verification.mail.MailVerificationService;
import com.aurora.starter.verification.mail.MailVerificationVerifyRequest;
import com.aurora.starter.verification.scene.CommonVerificationScene;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.identity.constants.IdentityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 邮件验证码编排器，封装 send/verify 重复模式，
 * 供 {@link AuthBizService} 和 {@link SysUserBizService} 共用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailVerificationOrchestrator {

    private final ObjectProvider<MailVerificationService> mailVerificationServiceProvider;

    /**
     * 发送指定场景的邮件验证码。
     */
    public void sendCode(String email, CommonVerificationScene scene) {
        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(IdentityConstants.EMAIL_CODE_SEND_FAILED_MESSAGE);
        }
        try {
            verificationService.send(VerificationMailRequestFactory.createRequest(email, scene));
        } catch (VerificationCooldownException exception) {
            throw new BizException(IdentityConstants.EMAIL_CODE_SEND_TOO_FREQUENT_MESSAGE);
        } catch (VerificationException | IllegalArgumentException exception) {
            log.error("Failed to send verification code to {} for {}", email, scene, exception);
            throw new BizException(IdentityConstants.EMAIL_CODE_SEND_FAILED_MESSAGE, exception);
        }
    }

    /**
     * 校验并消费指定场景的邮件验证码。
     *
     * @return true 表示校验通过
     */
    public boolean verifyCode(String email, CommonVerificationScene scene, String code) {
        MailVerificationService verificationService = mailVerificationServiceProvider.getIfAvailable();
        if (verificationService == null) {
            throw new BizException(IdentityConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        boolean verified;
        try {
            verified = verificationService.verifyAndConsume(
                    new MailVerificationVerifyRequest(email, scene, code));
        } catch (VerificationException | IllegalArgumentException exception) {
            throw new BizException(IdentityConstants.EMAIL_CODE_VERIFY_FAILED_MESSAGE);
        }
        if (!verified) {
            throw new BizException(IdentityConstants.EMAIL_CODE_INVALID_MESSAGE);
        }
        return true;
    }
}

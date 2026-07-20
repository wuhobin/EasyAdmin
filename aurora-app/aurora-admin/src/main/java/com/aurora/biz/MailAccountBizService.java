package com.aurora.biz;

import com.aurora.domain.convert.MailAccountConvert;
import com.aurora.domain.form.mail.MailAccountForm;
import com.aurora.domain.vo.mail.MailAccountVo;
import com.aurora.entity.MailAccount;
import com.aurora.handler.mail.ImapMailClient;
import com.aurora.handler.mail.MailCredentialCipher;
import com.aurora.service.MailAccountService;
import com.aurora.starter.webmvc.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MailAccountBizService {
    private final MailAccountService mailAccountService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;

    public List<MailAccountVo> list() {
        return MailAccountConvert.INSTANCE.toVoList(mailAccountService.listOrdered());
    }

    public MailAccountVo add(MailAccountForm form) {
        if (form.getAuthCode() == null || form.getAuthCode().isBlank()) {
            throw new BizException("新增邮箱时授权码不能为空");
        }
        validateAndNormalizeEmail(form);
        checkEmailUnique(form.getEmail(), null);
        MailAccount account = MailAccountConvert.INSTANCE.toEntity(form);
        normalize(account);
        account.setAuthCodeCiphertext(credentialCipher.encrypt(form.getAuthCode().trim()));
        mailAccountService.save(account);
        return MailAccountConvert.INSTANCE.toVo(account);
    }

    public void update(MailAccountForm form) {
        if (form.getId() == null) {
            throw new BizException("邮箱账户ID不能为空");
        }
        MailAccount current = getRequired(form.getId());
        validateAndNormalizeEmail(form);
        checkEmailUnique(form.getEmail(), form.getId());
        MailAccount account = MailAccountConvert.INSTANCE.toEntity(form);
        normalize(account);
        if (form.getAuthCode() == null || form.getAuthCode().isBlank()) {
            account.setAuthCodeCiphertext(current.getAuthCodeCiphertext());
        } else {
            account.setAuthCodeCiphertext(credentialCipher.encrypt(form.getAuthCode().trim()));
        }
        mailAccountService.updateById(account);
    }

    public void delete(Long id) {
        if (!mailAccountService.removeById(id)) {
            throw new BizException("邮箱账户不存在");
        }
    }

    public void test(Long id) {
        MailAccount account = getRequired(id);
        try {
            imapMailClient.testConnection(account, credentialCipher.decrypt(account.getAuthCodeCiphertext()));
            updateConnection(account, null);
        } catch (BizException exception) {
            updateConnection(account, exception.getMessage());
            throw exception;
        }
    }

    private MailAccount getRequired(Long id) {
        MailAccount account = mailAccountService.getById(id);
        if (account == null) {
            throw new BizException("邮箱账户不存在");
        }
        return account;
    }

    private void checkEmailUnique(String email, Long excludedId) {
        if (mailAccountService.existsByEmail(email, excludedId)) {
            throw new BizException("该邮箱已经添加");
        }
    }

    private static void validateAndNormalizeEmail(MailAccountForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (!form.getProvider().matchesEmail(email)) {
            throw new BizException(form.getProvider().getDescription()
                    + "地址必须以 @" + form.getProvider().getEmailDomain() + " 结尾");
        }
        form.setEmail(email);
    }

    private static void normalize(MailAccount account) {
        account.setEmail(account.getEmail().trim().toLowerCase());
        account.setAccountName(account.getAccountName().trim());
        if (account.getEnabled() == null) {
            account.setEnabled(1);
        }
        if (account.getSort() == null) {
            account.setSort(0);
        }
    }

    private void updateConnection(MailAccount account, String error) {
        MailAccount update = new MailAccount();
        update.setId(account.getId());
        update.setLastConnectTime(LocalDateTime.now());
        update.setLastError(error == null ? "" : error);
        mailAccountService.updateById(update);
    }
}

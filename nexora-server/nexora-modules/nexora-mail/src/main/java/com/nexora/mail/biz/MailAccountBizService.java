package com.nexora.mail.biz;

import com.nexora.mail.constants.MailConstants;
import com.nexora.mail.constants.MailProviderEnum;
import com.nexora.mail.domain.convert.MailAccountConvert;
import com.nexora.mail.domain.form.MailAccountForm;
import com.nexora.mail.domain.vo.MailAccountVo;
import com.nexora.mail.domain.vo.MailProviderVo;
import com.nexora.mail.entity.MailAccount;
import com.nexora.mail.infrastructure.ImapMailClient;
import com.nexora.mail.infrastructure.MailCredentialCipher;
import com.nexora.mail.service.MailAccountService;
import com.nexora.system.api.DictionaryEntry;
import com.nexora.system.api.SystemDictionaryReader;
import com.aurora.starter.security.context.SecurityUtils;
import com.aurora.starter.webmvc.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailAccountBizService {
    private final MailAccountService mailAccountService;
    private final SystemDictionaryReader dictionaryReader;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;

    public List<MailAccountVo> list() {
        return MailAccountConvert.INSTANCE.toVoList(
                mailAccountService.listOrderedByOwnerId(currentOwnerId()));
    }

    public List<MailProviderVo> listProviders() {
        List<DictionaryEntry> entries = dictionaryReader
                .findEnabledEntries(MailConstants.MAIL_PROVIDER_DICT_TYPE)
                .orElseThrow(() -> new BizException(MailConstants.MAIL_PROVIDER_NOT_CONFIGURED_MESSAGE));
        List<MailProviderVo> providers = entries
                .stream()
                .map(this::toProviderVo)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (providers.isEmpty()) {
            throw new BizException(MailConstants.MAIL_PROVIDER_EMPTY_MESSAGE);
        }
        return providers;
    }

    public MailAccountVo add(MailAccountForm form) {
        if (form.getAuthCode() == null || form.getAuthCode().isBlank()) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_AUTH_CODE_REQUIRED_MESSAGE);
        }
        validateAndNormalizeEmail(form);
        Integer ownerId = currentOwnerId();
        checkEmailUnique(ownerId, form.getEmail(), null);
        MailAccount account = MailAccountConvert.INSTANCE.toEntity(form);
        account.setOwnerId(ownerId);
        normalize(account);
        account.setAuthCodeCiphertext(credentialCipher.encrypt(form.getAuthCode().trim()));
        try {
            mailAccountService.save(account);
        } catch (DuplicateKeyException exception) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_EXISTS_MESSAGE);
        }
        return MailAccountConvert.INSTANCE.toVo(account);
    }

    public void update(MailAccountForm form) {
        if (form.getId() == null) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_ID_REQUIRED_MESSAGE);
        }
        MailAccount current = getRequired(form.getId());
        validateAndNormalizeEmail(form);
        checkEmailUnique(current.getOwnerId(), form.getEmail(), form.getId());
        MailAccount account = MailAccountConvert.INSTANCE.toEntity(form);
        normalize(account);
        if (form.getAuthCode() == null || form.getAuthCode().isBlank()) {
            account.setAuthCodeCiphertext(current.getAuthCodeCiphertext());
        } else {
            account.setAuthCodeCiphertext(credentialCipher.encrypt(form.getAuthCode().trim()));
        }
        try {
            mailAccountService.updateById(account);
        } catch (DuplicateKeyException exception) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_EXISTS_MESSAGE);
        }
    }

    public void delete(Long id) {
        if (!mailAccountService.removeByIdAndOwnerId(id, currentOwnerId())) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_UNAVAILABLE_MESSAGE);
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
        MailAccount account = mailAccountService.getByIdAndOwnerId(id, currentOwnerId());
        if (account == null) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_UNAVAILABLE_MESSAGE);
        }
        return account;
    }

    private void checkEmailUnique(Integer ownerId, String email, Long excludedId) {
        if (mailAccountService.existsByOwnerIdAndEmail(ownerId, email, excludedId)) {
            throw new BizException(MailConstants.MAIL_ACCOUNT_EXISTS_MESSAGE);
        }
    }

    private static void validateAndNormalizeEmail(MailAccountForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (!form.getProvider().matchesEmail(email)) {
            throw new BizException(MailConstants.MAIL_ADDRESS_DOMAIN_REQUIRED_MESSAGE.formatted(
                    form.getProvider().getDescription(), form.getProvider().getEmailDomain()));
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

    private MailProviderVo toProviderVo(DictionaryEntry item) {
        try {
            MailProviderEnum provider =
                    MailProviderEnum.valueOf(item.value());
            return MailProviderVo.builder()
                    .label(item.label())
                    .value(provider.name())
                    .domain(provider.getEmailDomain())
                    .imapHost(provider.getHost())
                    .imapPort(provider.getPort())
                    .defaultProvider(item.defaultEntry())
                    .build();
        } catch (IllegalArgumentException exception) {
            log.warn("Ignore unsupported mail provider dictionary value: {}", item.value());
            return null;
        }
    }

    private void updateConnection(MailAccount account, String error) {
        MailAccount update = new MailAccount();
        update.setId(account.getId());
        update.setLastConnectTime(LocalDateTime.now());
        update.setLastError(error == null ? "" : error);
        mailAccountService.updateById(update);
    }

    private static Integer currentOwnerId() {
        return SecurityUtils.getLoginIdAsInt();
    }
}

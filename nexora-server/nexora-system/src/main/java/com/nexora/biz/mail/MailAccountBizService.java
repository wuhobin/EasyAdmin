package com.nexora.biz.mail;

import com.nexora.constants.MailProviderEnum;
import com.nexora.domain.convert.MailAccountConvert;
import com.nexora.domain.form.mail.MailAccountForm;
import com.nexora.domain.vo.mail.MailAccountVo;
import com.nexora.domain.vo.mail.MailProviderVo;
import com.nexora.entity.MailAccount;
import com.nexora.entity.SysDict;
import com.nexora.entity.SysDictData;
import com.nexora.handler.mail.ImapMailClient;
import com.nexora.handler.mail.MailCredentialCipher;
import com.nexora.service.MailAccountService;
import com.nexora.service.SysDictDataService;
import com.nexora.service.SysDictService;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailAccountBizService {
    private static final String MAIL_PROVIDER_DICT_TYPE = "mail_provider";

    private final MailAccountService mailAccountService;
    private final SysDictService sysDictService;
    private final SysDictDataService sysDictDataService;
    private final MailCredentialCipher credentialCipher;
    private final ImapMailClient imapMailClient;

    public List<MailAccountVo> list() {
        return MailAccountConvert.INSTANCE.toVoList(mailAccountService.listOrdered());
    }

    public List<MailProviderVo> listProviders() {
        SysDict dict = sysDictService.getOne(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getType, MAIL_PROVIDER_DICT_TYPE)
                .eq(SysDict::getStatus, 1), false);
        if (dict == null) {
            throw new BizException("未配置邮箱类型字典 mail_provider");
        }
        List<MailProviderVo> providers = sysDictDataService.list(new LambdaQueryWrapper<SysDictData>()
                        .eq(SysDictData::getDictId, dict.getId())
                        .eq(SysDictData::getStatus, 1)
                        .orderByAsc(SysDictData::getSort)
                        .orderByAsc(SysDictData::getId))
                .stream()
                .map(this::toProviderVo)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (providers.isEmpty()) {
            throw new BizException("邮箱类型字典没有可用数据");
        }
        return providers;
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

    private MailProviderVo toProviderVo(SysDictData item) {
        try {
            MailProviderEnum provider =
                    MailProviderEnum.valueOf(item.getValue());
            return MailProviderVo.builder()
                    .label(item.getLabel())
                    .value(provider.name())
                    .domain(provider.getEmailDomain())
                    .imapHost(provider.getHost())
                    .imapPort(provider.getPort())
                    .defaultProvider("1".equals(item.getIsDefault()))
                    .build();
        } catch (IllegalArgumentException exception) {
            log.warn("Ignore unsupported mail provider dictionary value: {}", item.getValue());
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
}

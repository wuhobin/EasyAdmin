package com.nexora.identity.biz;

import com.aurora.starter.redis.core.RedisCache;
import com.aurora.starter.webmvc.exception.BizException;
import com.nexora.constants.RedisConstants;
import com.nexora.identity.constants.LoginTypeEnum;
import com.nexora.identity.constants.SysUserStatusEnum;
import com.nexora.identity.domain.form.WechatLoginPollForm;
import com.nexora.identity.domain.vo.LoginUserInfoVo;
import com.nexora.identity.domain.vo.WechatLoginPollVo;
import com.nexora.identity.domain.vo.WechatLoginTransactionVo;
import com.nexora.identity.entity.SysRole;
import com.nexora.identity.entity.SysUser;
import com.nexora.identity.entity.UserIdentity;
import com.nexora.identity.infrastructure.WechatMpClientManager;
import com.nexora.identity.service.SysRoleService;
import com.nexora.identity.service.SysUserService;
import com.nexora.identity.service.UserIdentityService;
import com.nexora.system.api.RegistrationSettings;
import com.nexora.system.api.SystemConfigReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WechatLoginService {

    public static final String PROVIDER = "WECHAT_MP";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> TAKE_SCRIPT =
            new DefaultRedisScript<>("local v=redis.call('GET',KEYS[1]); if v then redis.call('DEL',KEYS[1]); end; return v", String.class);

    private final RedisCache redisCache;
    private final SystemConfigReader configReader;
    private final WechatMpClientManager clientManager;
    private final UserIdentityService userIdentityService;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final AuthBizService authBizService;

    public WechatLoginTransactionVo createTransaction() {
        clientManager.requireSettings();
        String transactionId = UUID.randomUUID().toString();
        String pollToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        String code;
        do {
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (!Boolean.TRUE.equals(redisCache.setIfAbsent(codeKey(code), transactionId,
                RedisConstants.WECHAT_LOGIN_CODE_TTL_SECONDS, TimeUnit.SECONDS)));
        redisCache.setCacheObject(transactionKey(transactionId),
                new PendingTransaction(code, sha256(pollToken)),
                RedisConstants.WECHAT_LOGIN_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        return new WechatLoginTransactionVo(transactionId, pollToken, code,
                RedisConstants.WECHAT_LOGIN_CODE_TTL_SECONDS);
    }

    public WechatLoginPollVo poll(WechatLoginPollForm form) {
        String transactionId = form.getTransactionId().strip();
        String secretHash = sha256(form.getPollToken());
        LoginResult result = redisCache.getCacheObject(resultKey(transactionId));
        if (result != null) {
            assertSecret(result.secretHash(), secretHash);
            if (!Boolean.TRUE.equals(redisCache.setIfAbsent(consumedKey(transactionId), "1",
                    RedisConstants.WECHAT_LOGIN_RESULT_TTL_SECONDS, TimeUnit.SECONDS))) {
                return new WechatLoginPollVo("EXPIRED", "登录结果已被使用", null);
            }
            redisCache.deleteObject(resultKey(transactionId));
            if ("SUCCESS".equals(result.status())) {
                SysUser user = sysUserService.getById(result.userId());
                LoginUserInfoVo loginUser = authBizService.loginUser(user, false);
                return new WechatLoginPollVo("SUCCESS", result.message(), loginUser);
            }
            return new WechatLoginPollVo(result.status(), result.message(), null);
        }
        PendingTransaction pending = redisCache.getCacheObject(transactionKey(transactionId));
        if (pending == null) {
            return new WechatLoginPollVo("EXPIRED", "登录码已过期，请重新获取", null);
        }
        assertSecret(pending.secretHash(), secretHash);
        return new WechatLoginPollVo("PENDING", "等待在公众号中发送登录码", null);
    }

    public void cancel(WechatLoginPollForm form) {
        PendingTransaction pending = redisCache.getCacheObject(transactionKey(form.getTransactionId()));
        if (pending != null && MessageDigest.isEqual(
                pending.secretHash().getBytes(StandardCharsets.UTF_8),
                sha256(form.getPollToken()).getBytes(StandardCharsets.UTF_8))) {
            redisCache.deleteObject(transactionKey(form.getTransactionId()));
            redisCache.deleteObject(codeKey(pending.code()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public String acceptCode(String code, String openId) {
        if (code == null || !code.matches("\\d{6}")) {
            return "请输入网页显示的6位数字登录码";
        }
        String transactionId = redisCache.execute(TAKE_SCRIPT, List.of(codeKey(code)));
        if (transactionId == null) {
            return "登录码错误或已过期，请返回网页重新获取";
        }
        PendingTransaction pending = redisCache.getCacheObject(transactionKey(transactionId));
        if (pending == null) {
            return "登录码已过期，请返回网页重新获取";
        }
        redisCache.deleteObject(transactionKey(transactionId));
        SysUser user = resolveUser(openId);
        String status;
        String message;
        if (Integer.valueOf(SysUserStatusEnum.PENDING.getCode()).equals(user.getStatus())) {
            status = "PENDING_AUDIT";
            message = "账号已创建，等待管理员审核";
        } else if (Integer.valueOf(SysUserStatusEnum.NORMAL.getCode()).equals(user.getStatus())) {
            status = "SUCCESS";
            message = "验证成功，请返回网页完成登录";
        } else {
            status = "FAILED";
            message = "账号已被禁用，请联系管理员";
        }
        redisCache.setCacheObject(resultKey(transactionId),
                new LoginResult(pending.secretHash(), status, message, user.getId()),
                RedisConstants.WECHAT_LOGIN_RESULT_TTL_SECONDS, TimeUnit.SECONDS);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysUser resolveUser(String openId) {
        String appId = clientManager.requireSettings().getAppId();
        UserIdentity identity = userIdentityService.getByProviderIdentity(PROVIDER, appId, openId);
        if (identity != null) {
            SysUser user = sysUserService.getById(identity.getUserId());
            if (user == null) {
                throw new BizException("微信身份关联的用户不存在，请联系管理员");
            }
            return user;
        }
        RegistrationSettings registration = configReader.register();
        SysRole role = sysRoleService.getByCode(registration.getDefaultRoleCode());
        if (role == null) {
            throw new BizException("注册配置不完整，请联系管理员");
        }
        SysUser user = new SysUser();
        user.setNickname("微信用户-" + randomSuffix());
        user.setLoginType(LoginTypeEnum.WECHAT_MP.getCode());
        user.setStatus(Boolean.TRUE.equals(registration.getNeedAudit())
                ? SysUserStatusEnum.PENDING.getCode() : SysUserStatusEnum.NORMAL.getCode());
        try {
            if (!sysUserService.save(user)) {
                throw new BizException("微信用户创建失败，请稍后重试");
            }
            sysRoleService.addUserRoles(user.getId(), List.of(role.getId()));
            UserIdentity newIdentity = new UserIdentity();
            newIdentity.setUserId(user.getId());
            newIdentity.setProvider(PROVIDER);
            newIdentity.setProviderAppId(appId);
            newIdentity.setProviderUserId(openId);
            if (!userIdentityService.save(newIdentity)) {
                throw new BizException("微信身份绑定失败，请稍后重试");
            }
            return user;
        } catch (DuplicateKeyException exception) {
            UserIdentity existing = userIdentityService.getByProviderIdentity(PROVIDER, appId, openId);
            if (existing != null) {
                SysUser existingUser = sysUserService.getById(existing.getUserId());
                if (existingUser == null) {
                    throw new BizException("微信身份关联的用户不存在，请联系管理员");
                }
                return existingUser;
            }
            throw exception;
        }
    }

    public void testConnection() {
        try {
            clientManager.service().getAccessToken(true);
        } catch (Exception exception) {
            throw new BizException("微信公众号连接测试失败：" + exception.getMessage());
        }
    }

    private static void assertSecret(String expected, String actual) {
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            throw new BizException("微信登录轮询凭证无效");
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String randomSuffix() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder result = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            result.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return result.toString();
    }

    private static String codeKey(String code) {
        return com.aurora.starter.common.utils.RedisKeyUtil.generate(RedisConstants.WECHAT_LOGIN_CODE_KEY, code);
    }

    private static String transactionKey(String id) {
        return com.aurora.starter.common.utils.RedisKeyUtil.generate(RedisConstants.WECHAT_LOGIN_TRANSACTION_KEY, id);
    }

    private static String resultKey(String id) {
        return com.aurora.starter.common.utils.RedisKeyUtil.generate(RedisConstants.WECHAT_LOGIN_RESULT_KEY, id);
    }

    private static String consumedKey(String id) {
        return com.aurora.starter.common.utils.RedisKeyUtil.generate(RedisConstants.WECHAT_LOGIN_CONSUMED_KEY, id);
    }

    private record PendingTransaction(String code, String secretHash) {
    }

    private record LoginResult(String secretHash, String status, String message, Integer userId) {
    }
}

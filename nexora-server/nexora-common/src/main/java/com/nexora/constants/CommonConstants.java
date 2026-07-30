package com.nexora.constants;

public class CommonConstants {
    public static final int ROOT_USER_ID = 1;

    public static final String UTF8 = "UTF-8";

    public static final String DEFAULT_PASSWORD = "123456";

    public static final String ADMIN = "admin";

    public static final String TEST = "test";

    public static final String UNKNOWN = "未知";

    public static final int YES = 1;

    public static final int NO = 0;

    public static final String CURRENT_USER = "current_user";

    public static final String FILE_EMPTY_MESSAGE = "上传文件不能为空";

    public static final String FILE_UPLOAD_FAILED_MESSAGE = "上传文件失败";

    public static final String FILE_CURRENT_USER_REQUIRED_MESSAGE = "无法获取当前登录用户";

    public static final String FILE_NOT_FOUND_OR_FORBIDDEN_MESSAGE = "文件不存在或无权访问";

    public static final String FILE_OSS_DELETE_FAILED_MESSAGE = "OSS 文件删除失败";

    public static final String FILE_RECORD_DELETE_FAILED_MESSAGE = "文件记录删除失败, id=%s, fileId=%s";

    public static final String FILE_OBJECT_KEY_UNAVAILABLE_MESSAGE = "无法解析 OSS 文件对象名称";

    public static final String FILE_DEFAULT_ORDER = "create_time desc";

    public static final String REGISTER_ENABLED_CONFIG_KEY = "register.enabled";

    public static final String REGISTER_ROLE_CODE_CONFIG_KEY = "register.role-code";

    public static final String TRUE_VALUE = "true";

    public static final String PARENT_VIEW = "ParentView";

    public static final Object USER = "user";

    public static final String EMAIL_REQUIRED_MESSAGE = "邮箱不能为空";

    public static final String EMAIL_IN_USE_MESSAGE = "邮箱已经被使用";

    public static final String EMAIL_NOT_REGISTERED_MESSAGE = "该邮箱未注册";

    public static final String EMAIL_UNCHANGED_MESSAGE = "新邮箱不能与当前邮箱相同";

    public static final String USER_NOT_FOUND_MESSAGE = "用户不存在";

    public static final String USER_ID_REQUIRED_MESSAGE = "用户ID不能为空";

    public static final String NICKNAME_REQUIRED_MESSAGE = "用户昵称不能为空";

    public static final String NICKNAME_TOO_LONG_MESSAGE = "用户昵称长度不能超过30个字符";

    public static final String PASSWORD_REQUIRED_MESSAGE = "密码不能为空";

    public static final String PASSWORD_LENGTH_INVALID_MESSAGE = "密码长度必须在6到20个字符之间";

    public static final String PASSWORD_RESET_FAILED_MESSAGE = "密码重置失败，请稍后重试";

    public static final String OLD_PASSWORD_INCORRECT_MESSAGE = "旧密码错误";

    public static final String ROLE_REQUIRED_MESSAGE = "请选择角色";

    public static final String ROOT_USER_DELETE_FORBIDDEN_MESSAGE = "根用户不能删除";

    public static final String ROOT_USER_DISABLE_FORBIDDEN_MESSAGE = "根用户不能停用";

    public static final String ROOT_USER_ADMIN_ROLE_REQUIRED_MESSAGE = "根用户不能移除管理员角色";

    public static final String EMAIL_CODE_REQUIRED_MESSAGE = "邮箱验证码不能为空";

    public static final String EMAIL_CODE_SEND_FAILED_MESSAGE = "验证码发送失败，请稍后重试";

    public static final String EMAIL_CODE_SEND_TOO_FREQUENT_MESSAGE = "验证码发送过于频繁，请稍后再试";

    public static final String EMAIL_CODE_VERIFY_FAILED_MESSAGE = "验证码校验失败，请稍后重试";

    public static final String EMAIL_CODE_INVALID_MESSAGE = "验证码不正确或已过期，请重新输入";

    public static final String EMAIL_CODE_FORMAT_INVALID_MESSAGE = "邮箱验证码格式不正确";

    public static final String EMAIL_CODE_PATTERN = "\\d{4,8}";

    public static final String IMAGE_CAPTCHA_REQUIRED_MESSAGE = "请先完成图片验证";

    public static final String IMAGE_CAPTCHA_INVALID_MESSAGE = "图片验证无效或已过期，请重新验证";

    public static final String IMAGE_CAPTCHA_VERIFY_FAILED_MESSAGE = "图片验证失败，请稍后重试";

    public static final String REGISTER_DISABLED_MESSAGE = "系统暂未开放注册";

    public static final String REGISTER_CONFIG_INCOMPLETE_MESSAGE = "注册配置不完整，请联系管理员";

    public static final String REGISTER_FAILED_MESSAGE = "注册失败，请稍后重试";

    public static final int MAX_NICKNAME_LENGTH = 30;

    public static final String CONFIG_KEY_EXISTS_MESSAGE = "配置键已存在";

    public static final String CONFIG_ADD_FAILED_MESSAGE = "新增配置失败";

    public static final String CONFIG_ID_REQUIRED_MESSAGE = "配置ID不能为空";

    public static final String CONFIG_NOT_FOUND_MESSAGE = "配置不存在";

    public static final String CONFIG_KEY_IMMUTABLE_MESSAGE = "配置键创建后不允许修改";

    public static final String CONFIG_UPDATE_FAILED_MESSAGE = "修改配置失败";

    public static final String CONFIG_DELETE_FAILED_MESSAGE = "删除配置失败";

    public static final String CONFIG_DEFAULT_ORDER = "update_time desc";

    public static final String MAIL_PROVIDER_DICT_TYPE = "mail_provider";

    public static final String MAIL_ACCOUNT_AUTH_CODE_REQUIRED_MESSAGE = "新增邮箱时授权码不能为空";

    public static final String MAIL_ACCOUNT_ID_REQUIRED_MESSAGE = "邮箱账户ID不能为空";

    public static final String MAIL_ACCOUNT_EXISTS_MESSAGE = "该邮箱已经添加";

    public static final String MAIL_ACCOUNT_UNAVAILABLE_MESSAGE = "邮箱账户不存在或不可用";

    public static final String MAIL_PROVIDER_NOT_CONFIGURED_MESSAGE = "未配置邮箱类型字典 mail_provider";

    public static final String MAIL_PROVIDER_EMPTY_MESSAGE = "邮箱类型字典没有可用数据";

    public static final String MAIL_ADDRESS_DOMAIN_REQUIRED_MESSAGE = "%s地址必须以 @%s 结尾";

    public static final String MAIL_CURSOR_INVALID_MESSAGE = "邮件分页游标无效，请刷新后重试";

    public static final String MAIL_CURSOR_CREATE_FAILED_MESSAGE = "邮件分页游标生成失败";

    public static final String VERIFICATION_TITLE_PLACEHOLDER = "{verificationTitle}";

    public static final String VERIFICATION_DESCRIPTION_PLACEHOLDER = "{verificationDescription}";

    public static final String VERIFICATION_SCENE_PLACEHOLDER = "{verificationScene}";

    public static final String VERIFICATION_GUIDE_PLACEHOLDER = "{verificationGuide}";

    public static final String REGISTER_EMAIL_SUBJECT = "Nexora Admin 注册验证码";

    public static final String REGISTER_VERIFICATION_TITLE = "注册验证";

    public static final String REGISTER_VERIFICATION_DESCRIPTION =
            "您正在注册 Nexora Admin 账号，请使用下方验证码完成验证。";

    public static final String REGISTER_VERIFICATION_SCENE = "账号注册";

    public static final String REGISTER_VERIFICATION_GUIDE =
            "返回注册页面，输入上方验证码并提交，即可继续完成账号注册。";

    public static final String LOGIN_EMAIL_SUBJECT = "Nexora Admin 登录验证码";

    public static final String LOGIN_VERIFICATION_TITLE = "登录验证";

    public static final String LOGIN_VERIFICATION_DESCRIPTION =
            "您正在使用验证码登录 Nexora Admin，请使用下方验证码完成验证。";

    public static final String LOGIN_VERIFICATION_SCENE = "账号登录";

    public static final String LOGIN_VERIFICATION_GUIDE =
            "返回登录页面，输入上方验证码并提交，即可继续完成登录。";

    public static final String RESET_PASSWORD_EMAIL_SUBJECT = "Nexora Admin 找回密码验证码";

    public static final String RESET_PASSWORD_VERIFICATION_TITLE = "忘记密码验证";

    public static final String RESET_PASSWORD_VERIFICATION_DESCRIPTION =
            "您正在重置 Nexora Admin 登录密码，请使用下方验证码完成身份验证。";

    public static final String RESET_PASSWORD_VERIFICATION_SCENE = "忘记密码";

    public static final String RESET_PASSWORD_VERIFICATION_GUIDE =
            "返回密码重置页面，输入上方验证码并提交，即可设置新的登录密码。";

    public static final String CHANGE_EMAIL_SUBJECT = "Nexora Admin 邮箱换绑验证码";

    public static final String CHANGE_EMAIL_VERIFICATION_TITLE = "邮箱换绑验证";

    public static final String CHANGE_EMAIL_VERIFICATION_DESCRIPTION =
            "您正在修改 Nexora Admin 登录邮箱，请使用下方验证码完成验证。";

    public static final String CHANGE_EMAIL_VERIFICATION_SCENE = "邮箱换绑";

    public static final String CHANGE_EMAIL_VERIFICATION_GUIDE =
            "返回个人中心，输入上方验证码并提交，即可完成邮箱换绑。";

    public static final String DEFAULT_VERIFICATION_EMAIL_SUBJECT = "Nexora Admin 邮箱验证码";

    public static final String DEFAULT_VERIFICATION_TITLE = "邮箱验证";

    public static final String DEFAULT_VERIFICATION_DESCRIPTION =
            "您正在进行 Nexora Admin 身份验证，请使用下方验证码完成验证。";

    public static final String DEFAULT_VERIFICATION_SCENE = "身份验证";

    public static final String DEFAULT_VERIFICATION_GUIDE =
            "返回验证页面，输入上方验证码并提交，即可继续完成当前操作。";

    public static final String VERIFICATION_EMAIL_TEMPLATE = """
            <!doctype html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>{verificationTitle}</title>
            </head>
            <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Microsoft YaHei',Arial,sans-serif;color:#111827;">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;background-color:#f5f7fa;">
                    <tr>
                        <td align="center" style="padding:32px 12px;">
                            <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="width:100%;max-width:600px;background-color:#ffffff;border:1px solid #edf0f5;border-radius:20px;">
                                <tr>
                                    <td style="padding:40px 32px;">
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center">
                                            <tr>
                                                <td align="center" width="32" height="32" style="width:32px;height:32px;background-color:#635bff;border-radius:10px;color:#ffffff;font-size:18px;font-weight:700;line-height:32px;">N</td>
                                                <td style="padding-left:10px;color:#635bff;font-size:20px;font-weight:700;">Nexora Admin</td>
                                            </tr>
                                        </table>

                                        <h1 style="margin:28px 0 8px;text-align:center;color:#0f172a;font-size:30px;line-height:42px;font-weight:700;">{verificationTitle}</h1>
                                        <p style="margin:0;text-align:center;color:#475569;font-size:16px;line-height:26px;">{verificationDescription}</p>

                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" style="margin-top:16px;">
                                            <tr>
                                                <td style="padding:7px 14px;background-color:#f0efff;border-radius:999px;color:#635bff;font-size:13px;font-weight:600;">{expireMinutes} 分钟内有效</td>
                                            </tr>
                                        </table>

                                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;margin-top:30px;background-color:#fafbfc;border:1px dashed #cbd5e1;border-radius:16px;">
                                            <tr>
                                                <td align="center" style="padding:32px 20px;">
                                                    <p style="margin:0 0 12px;color:#94a3b8;font-size:12px;font-weight:600;">本次验证码</p>
                                                    <div style="display:inline-block;padding:14px 24px;background-color:#ffffff;border:1px solid #dbe1ea;border-radius:8px;color:#0f172a;font-family:Consolas,'Courier New',monospace;font-size:30px;line-height:38px;font-weight:700;letter-spacing:8px;">{code}</div>
                                                    <p style="margin:18px 0 0;color:#ef4444;font-size:12px;line-height:20px;font-weight:600;">请勿向任何人分享此验证码，每个验证码仅可使用一次。</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;margin-top:30px;background-color:#0b1324;border-radius:16px;">
                                            <tr>
                                                <td width="50%" valign="top" style="width:50%;padding:24px;">
                                                    <p style="margin:0 0 8px;color:#93c5fd;font-size:12px;line-height:18px;">验证场景</p>
                                                    <p style="margin:0;color:#ffffff;font-size:20px;line-height:28px;font-weight:700;">{verificationScene}</p>
                                                </td>
                                                <td width="50%" valign="top" style="width:50%;padding:24px;">
                                                    <p style="margin:0 0 8px;color:#93c5fd;font-size:12px;line-height:18px;">失效时间</p>
                                                    <p style="margin:0;color:#ffffff;font-size:20px;line-height:28px;font-weight:700;">{expireMinutes} 分钟</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;margin-top:30px;background-color:#f8fafc;border-radius:16px;">
                                            <tr>
                                                <td style="padding:24px;">
                                                    <p style="margin:0 0 6px;color:#0f172a;font-size:14px;line-height:22px;font-weight:700;">如何完成验证</p>
                                                    <p style="margin:0 0 20px;color:#94a3b8;font-size:13px;line-height:22px;">{verificationGuide}</p>
                                                    <p style="margin:0 0 6px;color:#0f172a;font-size:14px;line-height:22px;font-weight:700;">安全提示</p>
                                                    <p style="margin:0;color:#94a3b8;font-size:13px;line-height:22px;">如果这不是你的操作，请忽略本邮件，你的账户信息不会发生改变。</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="margin:28px 0 0;text-align:center;color:#c0c7d2;font-size:12px;line-height:20px;">此邮件由系统自动发送，请勿直接回复。</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;
}

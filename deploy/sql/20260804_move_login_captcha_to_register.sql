START TRANSACTION;

SET @login_captcha_enabled = (
    SELECT COALESCE(
            JSON_EXTRACT(`config_value`, '$.captchaEnabled'),
            CAST('true' AS JSON)
    )
    FROM `sys_config_group`
    WHERE `group_code` = 'login'
    LIMIT 1
);

UPDATE `sys_config_group`
SET `config_value` = JSON_SET(
        `config_value`,
        '$.captchaEnabled',
        COALESCE(@login_captcha_enabled, CAST('true' AS JSON))
)
WHERE `group_code` = 'register';

UPDATE `sys_config_group`
SET `config_value` = JSON_REMOVE(`config_value`, '$.captchaEnabled')
WHERE `group_code` = 'login';

COMMIT;

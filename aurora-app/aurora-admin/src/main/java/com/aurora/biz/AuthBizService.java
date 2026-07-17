package com.aurora.biz;

import com.aurora.domain.convert.AuthConvert;
import com.aurora.domain.form.auth.LoginForm;
import com.aurora.domain.vo.auth.LoginUserInfoVo;
import com.aurora.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthBizService {

    private final AuthService authService;

    public LoginUserInfoVo login(LoginForm form) {
        return AuthConvert.INSTANCE.toVo(authService.login(
                form.getUsername(), form.getPassword(), form.isRememberMe()));
    }

    public void logout() {
        authService.logout();
    }

    public LoginUserInfoVo getLoginUserInfo() {
        return AuthConvert.INSTANCE.toVo(authService.getLoginUserInfo());
    }
}

package com.nexora.identity.controller;

import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import com.nexora.identity.biz.AuthBizService;
import com.nexora.identity.biz.WechatLoginService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void delegatesImageCaptchaOperationsToTheBizService() {
        AuthBizService authBizService = mock(AuthBizService.class);
        AuthController controller = new AuthController(authBizService, mock(WechatLoginService.class));
        ImageCaptchaVO captcha = new ImageCaptchaVO();
        captcha.setId("captcha-id");
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        when(authBizService.generateImageCaptcha()).thenReturn(captcha);
        when(authBizService.matchImageCaptcha("captcha-id", track)).thenReturn(true);

        var generated = controller.generateImageCaptcha();
        var matched = controller.matchImageCaptcha("captcha-id", track);

        assertThat(generated.getData()).isSameAs(captcha);
        assertThat(matched.getData()).isTrue();
        verify(authBizService).generateImageCaptcha();
        verify(authBizService).matchImageCaptcha("captcha-id", track);
    }
}

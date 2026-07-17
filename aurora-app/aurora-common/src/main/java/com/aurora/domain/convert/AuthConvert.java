package com.aurora.domain.convert;

import com.aurora.domain.dto.user.LoginUserInfo;
import com.aurora.domain.vo.auth.LoginUserInfoVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AuthConvert {
    AuthConvert INSTANCE = Mappers.getMapper(AuthConvert.class);
    LoginUserInfoVo toVo(LoginUserInfo source);
}

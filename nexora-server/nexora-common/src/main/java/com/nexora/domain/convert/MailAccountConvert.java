package com.nexora.domain.convert;

import com.nexora.domain.form.mail.MailAccountForm;
import com.nexora.domain.vo.mail.MailAccountVo;
import com.nexora.entity.MailAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MailAccountConvert {
    MailAccountConvert INSTANCE = Mappers.getMapper(MailAccountConvert.class);

    @Mapping(target = "authCodeCiphertext", ignore = true)
    MailAccount toEntity(MailAccountForm form);

    MailAccountVo toVo(MailAccount entity);

    List<MailAccountVo> toVoList(List<MailAccount> entities);
}

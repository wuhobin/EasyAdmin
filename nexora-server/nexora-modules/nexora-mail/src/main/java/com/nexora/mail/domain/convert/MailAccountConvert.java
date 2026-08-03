package com.nexora.mail.domain.convert;

import com.nexora.mail.domain.form.MailAccountForm;
import com.nexora.mail.domain.vo.MailAccountVo;
import com.nexora.mail.entity.MailAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MailAccountConvert {
    MailAccountConvert INSTANCE = Mappers.getMapper(MailAccountConvert.class);

    @Mapping(target = "authCodeCiphertext", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    MailAccount toEntity(MailAccountForm form);

    MailAccountVo toVo(MailAccount entity);

    List<MailAccountVo> toVoList(List<MailAccount> entities);
}

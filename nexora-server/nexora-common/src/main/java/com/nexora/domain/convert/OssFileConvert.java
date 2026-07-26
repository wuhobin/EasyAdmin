package com.nexora.domain.convert;

import com.nexora.domain.form.query.file.OssFileQueryForm;
import com.nexora.domain.query.OssFileQuery;
import com.nexora.domain.vo.file.SysOssFileVo;
import com.nexora.entity.SysOssFile;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OssFileConvert {
    OssFileConvert INSTANCE = Mappers.getMapper(OssFileConvert.class);
    OssFileQuery toQuery(OssFileQueryForm form);
    SysOssFileVo toVo(SysOssFile entity);
}

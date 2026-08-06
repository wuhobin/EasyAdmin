package com.nexora.file.domain.convert;

import com.nexora.file.domain.form.OssFileQueryForm;
import com.nexora.file.domain.query.OssFileQuery;
import com.nexora.file.domain.vo.SysOssFileVo;
import com.nexora.file.entity.SysOssFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OssFileConvert {
    OssFileConvert INSTANCE = Mappers.getMapper(OssFileConvert.class);
    @Mapping(target = "fileId", ignore = true)
    OssFileQuery toQuery(OssFileQueryForm form);
    SysOssFileVo toVo(SysOssFile entity);
}

package com.aurora.domain.convert;

import com.aurora.domain.form.query.file.OssFileQueryForm;
import com.aurora.domain.query.OssFileQuery;
import com.aurora.domain.vo.file.SysOssFileVo;
import com.aurora.entity.SysOssFile;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OssFileConvert {
    OssFileConvert INSTANCE = Mappers.getMapper(OssFileConvert.class);
    OssFileQuery toQuery(OssFileQueryForm form);
    SysOssFileVo toVo(SysOssFile entity);
}

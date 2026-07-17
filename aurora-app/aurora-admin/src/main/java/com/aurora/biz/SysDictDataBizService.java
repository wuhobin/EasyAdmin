package com.aurora.biz;

import com.aurora.domain.convert.SysDictDataConvert;
import com.aurora.domain.form.query.system.SysDictDataQueryForm;
import com.aurora.domain.form.system.SysDictDataForm;
import com.aurora.domain.vo.system.SysDictDataVo;
import com.aurora.entity.SysDictData;
import com.aurora.service.SysDictDataService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictDataBizService {
    private final SysDictDataService sysDictDataService;
    public IPage<SysDictDataVo> list(SysDictDataQueryForm form, PageParam pageParam) {
        IPage<SysDictData> page = sysDictDataService.listDictData(SysDictDataConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysDictDataConvert.INSTANCE::toVo);
    }
    public void add(SysDictDataForm form) { sysDictDataService.addDictData(SysDictDataConvert.INSTANCE.toEntity(form)); }
    public void update(SysDictDataForm form) { sysDictDataService.updateDictData(SysDictDataConvert.INSTANCE.toEntity(form)); }
    public void delete(List<Long> ids) { sysDictDataService.removeBatchByIds(ids); }
}

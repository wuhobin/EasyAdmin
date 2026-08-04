package com.nexora.system.biz;

import com.nexora.system.domain.convert.SysDictDataConvert;
import com.nexora.system.domain.form.SysDictDataForm;
import com.nexora.system.domain.form.SysDictDataQueryForm;
import com.nexora.system.domain.vo.SysDictDataVo;
import com.nexora.system.entity.SysDictData;
import com.nexora.system.service.SysDictDataService;
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
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy("sort asc");
        }
        IPage<SysDictData> page = sysDictDataService.listDictData(SysDictDataConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysDictDataConvert.INSTANCE::toVo);
    }
    public void add(SysDictDataForm form) { sysDictDataService.save(SysDictDataConvert.INSTANCE.toEntity(form)); }
    public void update(SysDictDataForm form) { sysDictDataService.updateById(SysDictDataConvert.INSTANCE.toEntity(form)); }
    public void delete(List<Long> ids) { sysDictDataService.removeBatchByIds(ids); }
}

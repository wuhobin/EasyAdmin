package com.aurora.biz;

import com.aurora.domain.convert.SysDictConvert;
import com.aurora.domain.form.query.system.SysDictQueryForm;
import com.aurora.domain.form.system.SysDictForm;
import com.aurora.domain.vo.system.SysDictVo;
import com.aurora.entity.SysDict;
import com.aurora.service.SysDictService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictBizService {
    private final SysDictService sysDictService;
    public IPage<SysDictVo> list(SysDictQueryForm form, PageParam pageParam) {
        IPage<SysDict> page = sysDictService.getDictPageList(SysDictConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysDictConvert.INSTANCE::toVo);
    }
    public void add(SysDictForm form) { sysDictService.addDict(SysDictConvert.INSTANCE.toEntity(form)); }
    public void update(SysDictForm form) { sysDictService.updateDict(SysDictConvert.INSTANCE.toEntity(form)); }
    public void delete(List<Long> ids) { sysDictService.removeBatchByIds(ids); }
}

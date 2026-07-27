package com.nexora.biz.system;

import com.nexora.domain.convert.SysDictConvert;
import com.nexora.domain.form.query.system.SysDictQueryForm;
import com.nexora.domain.form.system.SysDictForm;
import com.nexora.domain.vo.system.SysDictVo;
import com.nexora.entity.SysDict;
import com.nexora.service.SysDictService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aurora.starter.webmvc.exception.BizException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictBizService {
    private final SysDictService sysDictService;
    public IPage<SysDictVo> list(SysDictQueryForm form, PageParam pageParam) {
        setDefaultOrder(pageParam, "sort asc");
        IPage<SysDict> page = sysDictService.getDictPageList(SysDictConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysDictConvert.INSTANCE::toVo);
    }

    public void add(SysDictForm form) {
        SysDict dict = SysDictConvert.INSTANCE.toEntity(form);
        if (sysDictService.existsByType(dict.getType(), null)) {
            throw new BizException("字典类型已存在");
        }
        sysDictService.save(dict);
    }

    public void update(SysDictForm form) {
        SysDict dict = SysDictConvert.INSTANCE.toEntity(form);
        if (sysDictService.getById(dict.getId()) == null) {
            throw new BizException("字典不存在");
        }
        if (sysDictService.existsByType(dict.getType(), dict.getId())) {
            throw new BizException("字典类型已存在");
        }
        sysDictService.updateById(dict);
    }

    public void delete(List<Long> ids) { sysDictService.removeBatchByIds(ids); }

    private static void setDefaultOrder(PageParam pageParam, String orderBy) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy(orderBy);
        }
    }
}

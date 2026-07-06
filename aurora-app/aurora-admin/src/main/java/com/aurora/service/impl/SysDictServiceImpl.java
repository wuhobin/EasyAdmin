package com.aurora.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.mybatisplus.mybatis.PageUtils;
import com.aurora.entity.SysDict;
import com.aurora.mapper.SysDictMapper;
import com.aurora.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysDictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictService {

    @Override
    public IPage<SysDict> getDictPageList(String name, Integer status, PageParam pageParam) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .like(StringUtils.hasText(name),SysDict::getName, name)
                .eq(status != null,SysDict::getStatus, status)
                .orderByAsc(SysDict::getSort);

        return baseMapper.selectPage(PageUtils.buildPage(pageParam), wrapper);
    }

    @Override
    public void addDict(SysDict dict) {
        // 检查字典类型是否已存在
        if (checkTypeExists(dict.getType(), null)) {
            throw new RuntimeException("字典类型已存在");
        }
        save(dict);
    }

    @Override
    public void updateDict(SysDict dict) {
        // 检查字典是否存在
        if (getById(dict.getId()) == null) {
            throw new RuntimeException("字典不存在");
        }
        // 检查字典类型是否已存在
        if (checkTypeExists(dict.getType(), dict.getId())) {
            throw new RuntimeException("字典类型已存在");
        }
        updateById(dict);
    }

    /**
     * 检查字典类型是否已存在
     */
    private boolean checkTypeExists(String type, Long excludeId) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getType, type);
        if (excludeId != null) {
            wrapper.ne(SysDict::getId, excludeId);
        }
        return baseMapper.selectCount(wrapper) > 0;
    }
}

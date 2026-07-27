package com.nexora.biz.system;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.cache.SysConfigCache;
import com.nexora.domain.convert.SysConfigConvert;
import com.nexora.domain.form.query.system.SysConfigQueryForm;
import com.nexora.domain.form.system.SysConfigForm;
import com.nexora.domain.vo.system.SysConfigVo;
import com.nexora.entity.SysConfig;
import com.nexora.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SysConfigBizService {

    private final SysConfigService sysConfigService;
    private final SysConfigCache sysConfigCache;

    public IPage<SysConfigVo> list(SysConfigQueryForm form, PageParam pageParam) {
        setDefaultOrder(pageParam);
        IPage<SysConfig> page = sysConfigService.getConfigPageList(
                SysConfigConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysConfigConvert.INSTANCE::toVo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysConfigForm form) {
        if (sysConfigService.existsByConfigKey(form.getConfigKey())) {
            throw new BizException("配置键已存在");
        }
        SysConfig config = SysConfigConvert.INSTANCE.toEntity(form);
        if (!sysConfigService.save(config)) {
            throw new BizException("新增配置失败");
        }
        sysConfigCache.setAfterCommit(config.getConfigKey(), config.getConfigValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigForm form) {
        if (form.getId() == null) {
            throw new BizException("配置ID不能为空");
        }
        SysConfig config = sysConfigService.getById(form.getId());
        if (config == null) {
            throw new BizException("配置不存在");
        }
        if (!config.getConfigKey().equals(form.getConfigKey())) {
            throw new BizException("配置键创建后不允许修改");
        }
        config.setConfigValue(form.getConfigValue());
        config.setRemark(form.getRemark());
        if (!sysConfigService.updateById(config)) {
            throw new BizException("修改配置失败");
        }
        sysConfigCache.setAfterCommit(config.getConfigKey(), config.getConfigValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfig config = sysConfigService.getById(id);
        if (config == null) {
            throw new BizException("配置不存在");
        }
        if (!sysConfigService.removeById(id)) {
            throw new BizException("删除配置失败");
        }
        sysConfigCache.evictAfterCommit(config.getConfigKey());
    }

    private static void setDefaultOrder(PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy("update_time desc");
        }
    }
}

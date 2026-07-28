package com.nexora.biz.system;

import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.starter.webmvc.exception.BizException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexora.cache.SysConfigCache;
import com.nexora.config.SysConfigReader;
import com.nexora.constants.CommonConstants;
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
    private final SysConfigReader sysConfigReader;

    public String getValue(String configKey) {
        return sysConfigReader.getString(configKey, null);
    }

    public IPage<SysConfigVo> list(SysConfigQueryForm form, PageParam pageParam) {
        setDefaultOrder(pageParam);
        IPage<SysConfig> page = sysConfigService.getConfigPageList(
                SysConfigConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysConfigConvert.INSTANCE::toVo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysConfigForm form) {
        if (sysConfigService.existsByConfigKey(form.getConfigKey())) {
            throw new BizException(CommonConstants.CONFIG_KEY_EXISTS_MESSAGE);
        }
        SysConfig config = SysConfigConvert.INSTANCE.toEntity(form);
        if (!sysConfigService.save(config)) {
            throw new BizException(CommonConstants.CONFIG_ADD_FAILED_MESSAGE);
        }
        sysConfigCache.setAfterCommit(config.getConfigKey(), config.getConfigValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysConfigForm form) {
        if (form.getId() == null) {
            throw new BizException(CommonConstants.CONFIG_ID_REQUIRED_MESSAGE);
        }
        SysConfig config = sysConfigService.getById(form.getId());
        if (config == null) {
            throw new BizException(CommonConstants.CONFIG_NOT_FOUND_MESSAGE);
        }
        if (!config.getConfigKey().equals(form.getConfigKey())) {
            throw new BizException(CommonConstants.CONFIG_KEY_IMMUTABLE_MESSAGE);
        }
        config.setConfigValue(form.getConfigValue());
        config.setRemark(form.getRemark());
        if (!sysConfigService.updateById(config)) {
            throw new BizException(CommonConstants.CONFIG_UPDATE_FAILED_MESSAGE);
        }
        sysConfigCache.setAfterCommit(config.getConfigKey(), config.getConfigValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfig config = sysConfigService.getById(id);
        if (config == null) {
            throw new BizException(CommonConstants.CONFIG_NOT_FOUND_MESSAGE);
        }
        if (!sysConfigService.removeById(id)) {
            throw new BizException(CommonConstants.CONFIG_DELETE_FAILED_MESSAGE);
        }
        sysConfigCache.evictAfterCommit(config.getConfigKey());
    }

    private static void setDefaultOrder(PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy(CommonConstants.CONFIG_DEFAULT_ORDER);
        }
    }
}

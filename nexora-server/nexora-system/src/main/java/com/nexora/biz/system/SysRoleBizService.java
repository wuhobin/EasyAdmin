package com.nexora.biz.system;

import com.nexora.domain.convert.SysRoleConvert;
import com.nexora.domain.form.query.system.SysRoleQueryForm;
import com.nexora.domain.form.system.SysRoleForm;
import com.nexora.domain.vo.system.SysRoleVo;
import com.nexora.entity.SysRole;
import com.nexora.cache.SecurityAuthorizationCache;
import com.nexora.service.SysRoleService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.nexora.utils.FastExcelUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aurora.starter.webmvc.exception.BizException;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleBizService {
    private final SysRoleService sysRoleService;
    private final SecurityAuthorizationCache authorizationCache;

    public IPage<SysRoleVo> list(SysRoleQueryForm form, PageParam pageParam) {
        if (pageParam != null && (pageParam.getOrderBy() == null || pageParam.getOrderBy().isBlank())) {
            pageParam.setOrderBy("create_time desc");
        }
        IPage<SysRole> page = sysRoleService.listRoles(SysRoleConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysRoleConvert.INSTANCE::toVo);
    }
    public SysRoleVo get(Integer id) { return SysRoleConvert.INSTANCE.toVo(sysRoleService.getById(id)); }

    public void add(SysRoleForm form) {
        SysRole role = SysRoleConvert.INSTANCE.toEntity(form);
        if (sysRoleService.existsByCode(role.getCode(), null)) {
            throw new BizException("角色编码已存在");
        }
        sysRoleService.save(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleForm form) {
        SysRole role = SysRoleConvert.INSTANCE.toEntity(form);
        if (sysRoleService.getById(role.getId()) == null) {
            throw new BizException("角色不存在");
        }
        if (sysRoleService.existsByCode(role.getCode(), role.getId())) {
            throw new BizException("角色编码已存在");
        }
        List<Integer> userIds = sysRoleService.listUserIdsByRoleIds(List.of(role.getId()));
        sysRoleService.updateById(role);
        authorizationCache.evictUsersAfterCommit(userIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        List<Integer> userIds = sysRoleService.listUserIdsByRoleIds(ids);
        sysRoleService.removeBatchByIds(ids);
        sysRoleService.deleteRoleMenus(ids);
        authorizationCache.evictUsersAfterCommit(userIds);
    }

    public List<Integer> getRoleMenus(Integer id) { return sysRoleService.getRoleMenus(id); }

    @Transactional(rollbackFor = Exception.class)
    public void updateRoleMenus(Integer id, List<Integer> menuIds) {
        List<Integer> userIds = sysRoleService.listUserIdsByRoleIds(List.of(id));
        sysRoleService.deleteRoleMenus(List.of(id));
        if (menuIds != null && !menuIds.isEmpty()) {
            sysRoleService.insertRoleMenus(id, menuIds);
        }
        authorizationCache.evictUsersAfterCommit(userIds);
    }
    public List<SysRoleVo> all() { return sysRoleService.list().stream().map(SysRoleConvert.INSTANCE::toVo).toList(); }
    public void export(HttpServletResponse response) throws IOException {
        FastExcelUtils.exportExcel(sysRoleService.list(), SysRole.class, "角色列表", response);
    }
}

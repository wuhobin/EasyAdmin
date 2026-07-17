package com.aurora.biz;

import com.aurora.domain.convert.SysRoleConvert;
import com.aurora.domain.form.query.system.SysRoleQueryForm;
import com.aurora.domain.form.system.SysRoleForm;
import com.aurora.domain.vo.system.SysRoleVo;
import com.aurora.entity.SysRole;
import com.aurora.service.SysRoleService;
import com.aurora.starter.mybatisplus.model.PageParam;
import com.aurora.utils.FastExcelUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleBizService {
    private final SysRoleService sysRoleService;

    public IPage<SysRoleVo> list(SysRoleQueryForm form, PageParam pageParam) {
        IPage<SysRole> page = sysRoleService.listRoles(SysRoleConvert.INSTANCE.toQuery(form), pageParam);
        return page.convert(SysRoleConvert.INSTANCE::toVo);
    }
    public SysRoleVo get(Integer id) { return SysRoleConvert.INSTANCE.toVo(sysRoleService.getById(id)); }
    public void add(SysRoleForm form) { sysRoleService.addRole(SysRoleConvert.INSTANCE.toEntity(form)); }
    public void update(SysRoleForm form) { sysRoleService.updateRole(SysRoleConvert.INSTANCE.toEntity(form)); }
    public void delete(List<Integer> ids) { sysRoleService.delete(ids); }
    public List<Integer> getRoleMenus(Integer id) { return sysRoleService.getRoleMenus(id); }
    public Void updateRoleMenus(Integer id, List<Integer> menuIds) { return sysRoleService.updateRoleMenus(id, menuIds); }
    public List<SysRoleVo> all() { return sysRoleService.list().stream().map(SysRoleConvert.INSTANCE::toVo).toList(); }
    public void export(HttpServletResponse response) throws IOException {
        FastExcelUtils.exportExcel(sysRoleService.list(), SysRole.class, "角色列表", response);
    }
}

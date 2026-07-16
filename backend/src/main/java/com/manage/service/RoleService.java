package com.manage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manage.common.BusinessException;
import com.manage.dto.RoleSaveRequest;
import com.manage.entity.SysRole;
import com.manage.entity.SysUserRole;
import com.manage.mapper.SysRoleMapper;
import com.manage.mapper.SysUserRoleMapper;
import com.manage.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<RoleVO> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    public Page<RoleVO> page(int pageNum, int pageSize, String roleName) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                .orderByDesc(SysRole::getId);
        Page<SysRole> result = roleMapper.selectPage(page, wrapper);

        Page<RoleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toRoleVO).toList());
        return voPage;
    }

    @Transactional
    public void create(RoleSaveRequest request) {
        checkRoleCodeUnique(request.getRoleCode(), null);
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        roleMapper.insert(role);
    }

    @Transactional
    public void update(RoleSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("角色ID不能为空");
        }
        SysRole exist = roleMapper.selectById(request.getId());
        if (exist == null) {
            throw new BusinessException("角色不存在");
        }
        checkRoleCodeUnique(request.getRoleCode(), request.getId());
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException("角色ID不能为空");
        }
        if (id == 1L) {
            throw new BusinessException("不能删除管理员角色");
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (count > 0) {
            throw new BusinessException("角色已分配给用户，无法删除");
        }
        roleMapper.deleteById(id);
    }

    private void checkRoleCodeUnique(String roleCode, Long excludeId) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}

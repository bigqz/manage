package com.manage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manage.common.BusinessException;
import com.manage.dto.AssignRolesRequest;
import com.manage.dto.UserSaveRequest;
import com.manage.entity.SysRole;
import com.manage.entity.SysUser;
import com.manage.entity.SysUserRole;
import com.manage.mapper.SysRoleMapper;
import com.manage.mapper.SysUserMapper;
import com.manage.mapper.SysUserRoleMapper;
import com.manage.vo.RoleVO;
import com.manage.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<UserVO> page(int pageNum, int pageSize, String username) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(page, wrapper);

        List<Long> userIds = result.getRecords().stream().map(SysUser::getId).toList();
        Map<Long, List<RoleVO>> roleMap = loadRolesByUserIds(userIds);

        Page<UserVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(user -> {
            UserVO vo = toUserVO(user);
            vo.setRoles(roleMap.getOrDefault(user.getId(), List.of()));
            return vo;
        }).toList());
        return voPage;
    }

    @Transactional
    public void create(UserSaveRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("密码不能为空");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);
    }

    @Transactional
    public void update(UserSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        SysUser exist = userMapper.selectById(request.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }

        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .ne(SysUser::getId, request.getId()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(request, user);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (id == 1L) {
            throw new BusinessException("不能删除超级管理员");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Transactional
    public void assignRoles(Long userId, AssignRolesRequest request) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : request.getRoleIds()) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private Map<Long, List<RoleVO>> loadRolesByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> relations = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, RoleVO> roleMap = roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, this::toRoleVO));
        return relations.stream().collect(Collectors.groupingBy(
                SysUserRole::getUserId,
                Collectors.mapping(r -> roleMap.get(r.getRoleId()), Collectors.filtering(r -> r != null, Collectors.toList()))
        ));
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}

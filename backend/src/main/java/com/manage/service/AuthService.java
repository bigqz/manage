package com.manage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.manage.common.BusinessException;
import com.manage.dto.LoginRequest;
import com.manage.entity.SysRole;
import com.manage.entity.SysUser;
import com.manage.mapper.SysRoleMapper;
import com.manage.mapper.SysUserMapper;
import com.manage.mapper.SysUserRoleMapper;
import com.manage.security.JwtUtil;
import com.manage.security.UserContext;
import com.manage.vo.LoginVO;
import com.manage.vo.RoleVO;
import com.manage.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginVO login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserInfo(toUserInfo(user));
        vo.setRoles(listRolesByUserId(user.getId()));
        return vo;
    }

    public LoginVO currentUserInfo() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        LoginVO vo = new LoginVO();
        vo.setUserInfo(toUserInfo(user));
        vo.setRoles(listRolesByUserId(userId));
        return vo;
    }

    private UserInfoVO toUserInfo(SysUser user) {
        UserInfoVO info = new UserInfoVO();
        BeanUtils.copyProperties(user, info);
        return info;
    }

    private List<RoleVO> listRolesByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectByUserId(userId)
                .stream()
                .map(com.manage.entity.SysUserRole::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream().map(this::toRoleVO).toList();
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}

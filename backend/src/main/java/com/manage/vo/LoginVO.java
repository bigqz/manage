package com.manage.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {

    private String token;
    private UserInfoVO userInfo;
    private List<RoleVO> roles;
}

package com.manage.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<RoleVO> roles;
}

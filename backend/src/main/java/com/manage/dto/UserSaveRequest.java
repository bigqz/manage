package com.manage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSaveRequest {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String nickname;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

package com.manage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleSaveRequest {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    private String remark;
}

package com.manage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;
}

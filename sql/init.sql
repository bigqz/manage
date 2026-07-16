CREATE DATABASE IF NOT EXISTS `manage` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `manage`;

DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_role`;

CREATE TABLE `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1正常',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
  `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码',
  `remark`      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- 密码: admin123 (BCrypt)，若下方哈希与本地生成不一致，启动后端后可用 DataInitializer 或重新生成
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `remark`) VALUES
(1, '管理员', 'ADMIN', '系统管理员'),
(2, '普通用户', 'USER', '普通用户');

INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`) VALUES
(1, 'admin', '$2a$10$H8.6MIYfd6JE73Srrd9jdeIASZOuzLeQiOQ3JR6jOMvoPNUA3MLVe', '管理员', 1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

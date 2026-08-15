package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer sex;
    private Integer status;
    private Long deptId;

    @TableField(exist = false)
    private String deptName;

    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysUser
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @EqualsAndHashCode(callSuper = true) — 含父类字段比较
//   - @TableName("sys_user") — MyBatis-Plus 映射用户表
//   - @TableField(exist = false) — deptName 仅展示字段，非数据库列
//
// 【关联文件】
//   - 继承 BaseEntity → BaseEntity.java
//   - 由 UserService/Impl 读写 → service/*UserService*.java
//   - 被 SysUserRole 关联 → SysUserRole.java
//   - 被 LoginUser 组装为 Security 上下文 → dto/LoginUser.java
//   - 登录成功写入 lastLoginTime/Ip 由 AuthServiceImpl → service/impl/AuthServiceImpl.java
//
// 【核心作用】用户实体，持有账号、密码、个人信息、状态与所属部门。
//
// 【设计必要性】密码字段由服务层 BCrypt 比对，实体本身不负责加密逻辑。
//   status 与 UserStatus 枚举配合使用控制启用/禁用。
//
// 【注意事项】
//   - sex: 0未知/1男/2女
//   - status: 1启用/0禁用
//   - deptName 为关联展示字段，需手动 join 填充
//   - 删除前要检查角色关联
// ============================================================

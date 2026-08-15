package com.permission.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.permission.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private String deptName;
    private Long parentId;
    private String ancestors;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;

    @TableField(exist = false)
    private List<SysDept> children;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.entity.SysDept
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成访问器
//   - @EqualsAndHashCode(callSuper = true) — 比较时加入父类字段；必须 callSuper 否则 id 等失效
//   - @TableName("sys_dept") — MyBatis-Plus 映射数据库表
//   - @TableField(exist = false) — children 是内存组装的树节点，不对应数据库列
//
// 【关联文件】
//   - 继承 BaseEntity → BaseEntity.java
//   - 被 DeptService / DeptServiceImpl 读写 → service/*DeptService*.java
//   - 被 SysUser.deptId 关联 → SysUser.java
//   - 被 DataScope 数据权限切面解析时引用 → framework/aspect/DataScopeAspect.java
//
// 【核心作用】部门实体，用 parentId + ancestors 构成层级/祖籍树。
//
// 【设计必要性】ancestors 存储 "0,1,12" 这种祖籍链，支持一条 SQL 查询某部门
//   下所有子部门；children 仅在树型接口中组装返回，因此用 exist=false 排入 ORM。
//
// 【注意事项】
//   - 删除部门前要检查子部门与用户，否则破坏关系完整性
//   - ancestors 由服务层维护更新，不要直接修改
// ============================================================

package com.permission.common;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.BaseEntity
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Getter / @Setter — Lombok，自动生成访问器
//   - @TableId(type = IdType.AUTO) — MyBatis-Plus 主键，自增策略
//   - @TableField(fill = FieldFill.INSERT / INSERT_UPDATE) — 自动填充字段标记，
//     需配合 MetaObjectHandler 实现类完成 createTime / updateTime / deleted 自动赋值
//   - implements Serializable — 保证实体可序列化（Redis 缓存、RPC 传输依赖）
//
// 【关联文件】
//   - 被所有业务实体继承 → entity/SysUser / SysRole / SysMenu / SysDept 等
//   - 自动填充实现 → framework/handler/MyMetaObjectHandler.java
//   - 被 copyProperties / MyBatis-Plus  CRUD 体系依赖
//
// 【核心作用】为所有业务实体提供统一四个基础字段（id / createTime / updateTime / deleted），
//   实现字段复用与全局 ORM 行为一致。
//
// 【设计必要性】抽取公共字段不仅避免重复代码，更让软删除（@TableLogic）、自动填充策略
//   集中治理，新增实体只需 extends BaseEntity 即可获得权限系统需要的全部基础能力。
//
// 【注意事项】
//   - deleted 字段依赖 @TableLogic 才会转为软删除，否则只是普通字段
//   - createTime / updateTime 必须配合 MetaObjectHandler 才会自动写入
//   - 非业务实体（关联表、日志表）不要继承此基类
// ============================================================

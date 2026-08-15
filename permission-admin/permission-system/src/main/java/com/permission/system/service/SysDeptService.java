package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> getDeptTree(String keyword, Integer status);

    SysDept getDeptById(Long id);

    void createDept(SysDept dept);

    void updateDept(SysDept dept);

    void deleteDept(Long id);

    List<SysDept> getDeptTreeSelect();
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.SysDeptService
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - IService<SysDept> — MyBatis-Plus，继承通用 Service 接口，提供部门基础 CRUD 能力
//     （page/list/save/update/remove 等）
//
// 【关键依赖】
//   - 依赖 SysDept 实体 → 部门业务操作的数据载体
//   - MyBatis-Plus 通用 Service → 基础 CRUD 操作由父接口提供
//
// 【关联文件】
//   - 被 SysDeptServiceImpl 实现，封装部门业务逻辑
//   - 被 DeptController 调用，提供部门管理 API
//   - 被 DataInitializer 调用，初始化部门数据
//
// 【核心作用】
//   部门业务服务接口，定义部门树查询、实体查询、增删改、部门树下拉选择等业务方法。
//
// 【设计必要性】
//   接口与实现分离（ISP），便于后续替换实现或添加 AOP/事务切面，符合分层架构设计。
//
// 【注意事项/安全提示】
//   - 部门树形结构数据量通常可控，无需分页，直接加载后在前端/内存中构建树
//   - 实现类通过 @PreAuthorize 在 Controller 层控制权限，接口层不加权限注解
// ============================================================

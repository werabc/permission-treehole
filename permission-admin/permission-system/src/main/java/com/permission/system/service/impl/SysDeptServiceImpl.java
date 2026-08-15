package com.permission.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.permission.common.ResultCode;
import com.permission.common.entity.SysDept;
import com.permission.common.entity.SysUser;
import com.permission.common.exception.BusinessException;
import com.permission.system.mapper.SysDeptMapper;
import com.permission.system.mapper.SysUserMapper;
import com.permission.system.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> getDeptTree(String keyword, Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            // Escape LIKE special chars to prevent wildcard abuse
            String safeKeyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.like(SysDept::getDeptName, safeKeyword);
        }
        if (status != null) {
            wrapper.eq(SysDept::getStatus, status);
        }
        wrapper.orderByAsc(SysDept::getSort);
        List<SysDept> allDepts = baseMapper.selectList(wrapper);
        return buildTree(allDepts, 0L);
    }

    @Override
    public SysDept getDeptById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional
    public void createDept(SysDept dept) {
        validateDeptNameUnique(dept.getDeptName(), dept.getParentId(), null);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        dept.setAncestors(getAncestors(dept.getParentId()));
        baseMapper.insert(dept);
    }

    @Override
    @Transactional
    public void updateDept(SysDept dept) {
        SysDept existing = baseMapper.selectById(dept.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        validateDeptNameUnique(dept.getDeptName(), dept.getParentId(), dept.getId());
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        dept.setAncestors(getAncestors(dept.getParentId()));
        baseMapper.updateById(dept);
    }

    @Override
    @Transactional
    public void deleteDept(Long id) {
        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_CHILDREN);
        }
        long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_USERS);
        }
        baseMapper.deleteById(id);
    }

    @Override
    public List<SysDept> getDeptTreeSelect() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1).orderByAsc(SysDept::getSort);
        List<SysDept> allDepts = baseMapper.selectList(wrapper);
        List<SysDept> tree = buildTree(allDepts, 0L);

        SysDept root = new SysDept();
        root.setId(0L);
        root.setDeptName("顶级部门");
        root.setParentId(-1L);
        root.setChildren(tree);

        List<SysDept> result = new ArrayList<>();
        result.add(root);
        return result;
    }

    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> tree = new ArrayList<>();
        for (SysDept dept : depts) {
            if (parentId.equals(dept.getParentId())) {
                dept.setChildren(buildTree(depts, dept.getId()));
                tree.add(dept);
            }
        }
        return tree;
    }

    private String getAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return "0";
        }
        SysDept parent = baseMapper.selectById(parentId);
        if (parent != null) {
            String ancestors = parent.getAncestors();
            if (StrUtil.isNotBlank(ancestors)) {
                return ancestors + "," + parentId;
            }
            return "0," + parentId;
        }
        return "0";
    }

    private void validateDeptNameUnique(String deptName, Long parentId, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, deptName)
                .eq(SysDept::getParentId, parentId != null ? parentId : 0L);
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DEPT_NAME_EXISTS);
        }
    }
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.system.service.impl.SysDeptServiceImpl
// 【模块】permission-system
//
// 【使用的注解/技术】
//   - @Service — Spring，声明业务层组件
//   - @RequiredArgsConstructor — Lombok，生成必需参数构造器（构造器注入）
//   - @Override — Java，标识重写接口/父类方法
//   - @Transactional — Spring，声明式事务，保证写操作（增删改）的原子性
//   - ServiceImpl<SysDeptMapper, SysDept> — MyBatis-Plus，继承通用 Service 实现基类
//   - LambdaQueryWrapper — MyBatis-Plus，类型安全查询构造器
//   - StrUtil / CollUtil — Hutool，字符串/集合工具（防止空值与空遍历）
//   - BusinessException / ResultCode — 自定义业务异常与错误码
//
// 【关键依赖】
//   - 依赖 SysDeptMapper → 部门数据访问
//   - 依赖 SysUserMapper → 删除前校验部门下是否仍有用户
//   - 依赖 SysDept 实体 → 部门业务操作载体
//   - 依赖 SysDeptService 接口 → 实现该接口契约
//
// 【关联文件】
//   - 被 DeptController 调用，提供部门管理业务逻辑
//   - 被 UserDetailsServiceImpl 调用，查询部门名称
//   - 依赖 ResultCode 错误码枚举（DEPT_HAS_CHILDREN、DEPT_HAS_USERS、DEPT_NAME_EXISTS 等）
//
// 【核心作用】
//   部门业务服务实现：提供部门树形查询、单部门详情、新增部门（含 ancestors 路径同步）、
//   修改部门、删除部门（含级联校验：子部门/用户不允许删除）、管理员树下拉选择。
//
// 【设计必要性】
//   部门的层级关系（parentId + ancestors 路径）需要在业务层维护一致性，直接暴露 Mapper
//   无法保证 ancestors 正确性；删除时需要校验子部门与关联用户，这些业务规则都适合放在
//   Service 层统一实现。
//
// 【注意事项/安全提示】
//   - LIKE 查询已转义通配符：keyword 中的 /%/_ 会被转义为 \\/\\_\\_，防止用户输入
//     % 或 _ 导致全表匹配（LIKE 通配符注入风险）
//   - createDept / updateDept 新增 ancestors 路径（getAncestors 私有方法从父部门递归拼接）
//   - deleteDept 严格校验：存在子部门或关联用户时禁止删除，抛结构化业务异常
//   - validateDeptNameUnique 同 parentId 下校验部门名称唯一性，更新操作可排除自身 ID
// ============================================================

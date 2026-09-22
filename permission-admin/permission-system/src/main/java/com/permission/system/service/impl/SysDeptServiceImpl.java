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
import com.permission.system.support.DataScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    @Transactional(rollbackFor = Exception.class)
    public void createDept(SysDept dept) {
        validateDeptNameUnique(dept.getDeptName(), dept.getParentId(), null);
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        dept.setAncestors(getAncestors(dept.getParentId()));
        // 组织层级由 ancestors 推导并冗余存储：1-集团 2-公司 3-部门 4+-小组
        dept.setDeptLevel(DataScopeHelper.calcLevel(dept.getAncestors()));
        baseMapper.insert(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(SysDept dept) {
        SysDept existing = baseMapper.selectById(dept.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "部门不存在");
        }
        validateDeptNameUnique(dept.getDeptName(), dept.getParentId(), dept.getId());
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        String newAncestors = getAncestors(dept.getParentId());
        boolean moved = !Objects.equals(newAncestors, existing.getAncestors());
        dept.setAncestors(newAncestors);
        dept.setDeptLevel(DataScopeHelper.calcLevel(newAncestors));
        baseMapper.updateById(dept);

        // 部门被移动（换了上级）时，整棵子树的层级都会变，需要级联刷新
        if (moved) {
            refreshDescendantLevels(dept.getId());
        }
    }

    /**
     * 级联刷新子树层级：数据权限依赖 dept_level 判断"本部门及以下限N级"的边界，
     * 层级不准会直接导致越权或漏看，因此移动部门后必须同步
     */
    private void refreshDescendantLevels(Long deptId) {
        List<SysDept> descendants = baseMapper.selectList(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0));
        for (SysDept d : descendants) {
            if (Objects.equals(d.getId(), deptId)) continue;
            if (!DataScopeHelper.isDescendantOf(d, deptId)) continue;
            int level = DataScopeHelper.calcLevel(d.getAncestors());
            if (d.getDeptLevel() == null || d.getDeptLevel() != level) {
                SysDept patch = new SysDept();
                patch.setId(d.getId());
                patch.setDeptLevel(level);
                baseMapper.updateById(patch);
            }
        }
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


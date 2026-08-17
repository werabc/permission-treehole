package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.R;
import com.permission.common.entity.ThCategory;
import com.permission.system.mapper.ThCategoryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "树洞分类管理")
@RestController
@RequestMapping("/api/admin/th/category")
@RequiredArgsConstructor
public class ThCategoryAdminController {

    private final ThCategoryMapper categoryMapper;

    @Operation(summary = "分类列表(管理)")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<List<ThCategory>> list() {
        return R.ok(categoryMapper.selectList(new LambdaQueryWrapper<ThCategory>()
                .eq(ThCategory::getDeleted, 0)
                .orderByAsc(ThCategory::getSort)));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> create(@RequestBody ThCategory category) {
        long count = categoryMapper.selectCount(new LambdaQueryWrapper<ThCategory>()
                .eq(ThCategory::getCode, category.getCode()));
        if (count > 0) return R.fail(4001, "分类编码已存在");
        category.setPostCount(0);
        category.setSort(category.getSort() != null ? category.getSort() : 0);
        category.setStatus(category.getStatus() != null ? category.getStatus() : 1);
        categoryMapper.insert(category);
        return R.ok();
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> update(@PathVariable Long id, @RequestBody ThCategory category) {
        ThCategory existing = categoryMapper.selectById(id);
        if (existing == null) return R.fail(404, "分类不存在");
        existing.setName(category.getName());
        existing.setCode(category.getCode());
        existing.setIcon(category.getIcon());
        existing.setDescription(category.getDescription());
        existing.setSort(category.getSort());
        existing.setStatus(category.getStatus());
        categoryMapper.updateById(existing);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> delete(@PathVariable Long id) {
        ThCategory category = categoryMapper.selectById(id);
        if (category == null) return R.fail(404, "分类不存在");
        category.setDeleted(1);
        categoryMapper.updateById(category);
        return R.ok();
    }
}

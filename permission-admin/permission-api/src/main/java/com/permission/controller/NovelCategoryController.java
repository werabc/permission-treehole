package com.permission.controller;

import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.entity.NovelCategory;
import com.permission.system.service.NovelCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小说分类管理")
@RestController
@RequestMapping("/api/novel/category")
@RequiredArgsConstructor
public class NovelCategoryController {

    private final NovelCategoryService categoryService;

    @Operation(summary = "获取所有启用分类")
    @GetMapping("/list")
    public R<java.util.List<NovelCategory>> list() {
        return R.ok(categoryService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelCategory>()
                        .eq(NovelCategory::getStatus, 1)
                        .orderByAsc(NovelCategory::getSort)));
    }

    @Operation(summary = "分页查询分类(管理员)")
    @GetMapping("/page")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<?> page(@RequestParam(defaultValue = "1") long pageNum,
                     @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(categoryService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NovelCategory>()
                        .orderByAsc(NovelCategory::getSort)));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "小说分类", value = "新增分类")
    public R<Void> create(@Valid @RequestBody NovelCategory category) {
        categoryService.createCategory(category);
        return R.ok();
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "小说分类", value = "修改分类")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody NovelCategory category) {
        category.setId(id);
        categoryService.updateCategory(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{ids}")
    @PreAuthorize("hasAnyAuthority('admin')")
    @OperationLog(module = "小说分类", value = "删除分类")
    public R<Void> delete(@PathVariable List<Long> ids) {
        categoryService.deleteCategories(ids);
        return R.ok();
    }
}

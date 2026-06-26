package com.permission.controller;

import com.permission.common.R;
import com.permission.common.annotation.OperationLog;
import com.permission.common.entity.SysDept;
import com.permission.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/dept")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptService deptService;

    @Operation(summary = "获取部门树")
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('system:dept:list', 'admin')")
    public R<List<SysDept>> tree(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) Integer status) {
        return R.ok(deptService.getDeptTree(keyword, status));
    }

    @Operation(summary = "获取部门树（下拉选择用）")
    @GetMapping("/tree-select")
    public R<List<SysDept>> treeSelect() {
        return R.ok(deptService.getDeptTreeSelect());
    }

    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:dept:query', 'admin')")
    public R<SysDept> getById(@PathVariable Long id) {
        return R.ok(deptService.getDeptById(id));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('system:dept:add', 'admin')")
    @OperationLog(module = "部门管理", value = "新增部门")
    public R<Void> create(@Valid @RequestBody SysDept dept) {
        deptService.createDept(dept);
        return R.ok();
    }

    @Operation(summary = "修改部门")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:dept:edit', 'admin')")
    @OperationLog(module = "部门管理", value = "修改部门")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SysDept dept) {
        dept.setId(id);
        deptService.updateDept(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('system:dept:delete', 'admin')")
    @OperationLog(module = "部门管理", value = "删除部门")
    public R<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return R.ok();
    }
}

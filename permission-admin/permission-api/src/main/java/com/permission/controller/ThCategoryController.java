package com.permission.controller;

import com.permission.common.R;
import com.permission.common.entity.ThCategory;
import com.permission.system.service.ThCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "树洞分类")
@RestController
@RequestMapping("/api/th/category")
@RequiredArgsConstructor
public class ThCategoryController {

    private final ThCategoryService categoryService;

    @Operation(summary = "获取所有启用分类")
    @GetMapping("/list")
    public R<List<ThCategory>> list() {
        return R.ok(categoryService.listEnabled());
    }
}

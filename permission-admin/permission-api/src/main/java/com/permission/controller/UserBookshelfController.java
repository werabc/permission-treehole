package com.permission.controller;

import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.UserBookshelf;
import com.permission.system.service.UserBookshelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "书架管理")
@RestController
@RequestMapping("/api/bookshelf")
@RequiredArgsConstructor
public class UserBookshelfController {

    private final UserBookshelfService bookshelfService;

    @Operation(summary = "获取我的书架")
    @GetMapping
    public R<List<UserBookshelf>> myBookshelf(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(bookshelfService.listByUser(loginUser.getUserId()));
    }

    @Operation(summary = "检查是否在书架中")
    @GetMapping("/check/{novelId}")
    public R<Boolean> check(@PathVariable Long novelId, @AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(bookshelfService.isInBookshelf(loginUser.getUserId(), novelId));
    }

    @Operation(summary = "加入书架")
    @PostMapping("/{novelId}")
    public R<Void> add(@PathVariable Long novelId, @AuthenticationPrincipal LoginUser loginUser) {
        bookshelfService.addToBookshelf(loginUser.getUserId(), novelId);
        return R.ok();
    }

    @Operation(summary = "移出书架")
    @DeleteMapping("/{novelId}")
    public R<Void> remove(@PathVariable Long novelId, @AuthenticationPrincipal LoginUser loginUser) {
        bookshelfService.removeFromBookshelf(loginUser.getUserId(), novelId);
        return R.ok();
    }
}

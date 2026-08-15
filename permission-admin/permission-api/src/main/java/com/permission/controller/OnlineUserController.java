package com.permission.controller;

import com.permission.common.R;
import com.permission.system.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "在线用户")
@RestController
@RequestMapping("/api/online")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @Operation(summary = "获取在线用户列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<List<Map<String, Object>>> list() {
        return R.ok(onlineUserService.getOnlineUsers());
    }

    @Operation(summary = "获取在线用户数")
    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(onlineUserService.getOnlineCount());
    }

    @Operation(summary = "强制用户下线")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<Void> forceLogout(@PathVariable Long userId) {
        onlineUserService.forceLogout(userId);
        return R.ok();
    }
}

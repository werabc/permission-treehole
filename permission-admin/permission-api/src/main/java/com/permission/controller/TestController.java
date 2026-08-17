package com.permission.controller;

import com.permission.common.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/ping")
    public R<String> ping() {
        return R.ok("pong");
    }

    @GetMapping("/admin-ping")
    @PreAuthorize("hasAnyAuthority('admin')")
    public R<String> adminPing() {
        return R.ok("admin pong");
    }
}

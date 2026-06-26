package com.permission.controller;

import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.ReadingHistory;
import com.permission.system.service.ReadingHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "阅读历史")
@RestController
@RequestMapping("/api/reading-history")
@RequiredArgsConstructor
public class ReadingHistoryController {

    private final ReadingHistoryService historyService;

    @Operation(summary = "获取我的阅读历史")
    @GetMapping
    public R<List<ReadingHistory>> myHistory(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(historyService.listByUser(loginUser.getUserId(), 50));
    }

    @Operation(summary = "记录/更新阅读进度")
    @PostMapping
    public R<Void> save(@RequestBody Map<String, Object> body,
                        @AuthenticationPrincipal LoginUser loginUser) {
        Long novelId = Long.valueOf(body.get("novelId").toString());
        Long chapterId = Long.valueOf(body.get("chapterId").toString());
        String chapterTitle = (String) body.getOrDefault("chapterTitle", "");
        historyService.saveOrUpdateHistory(loginUser.getUserId(), novelId, chapterId, chapterTitle);
        return R.ok();
    }
}

package com.permission.controller;

import com.permission.common.R;
import com.permission.common.dto.LoginUser;
import com.permission.common.entity.NovelComment;
import com.permission.system.service.NovelCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小说评论")
@RestController
@RequestMapping("/api/novel/comment")
@RequiredArgsConstructor
public class NovelCommentController {

    private final NovelCommentService commentService;

    @Operation(summary = "获取小说评论列表")
    @GetMapping("/list/{novelId}")
    public R<List<NovelComment>> list(@PathVariable Long novelId) {
        return R.ok(commentService.listByNovel(novelId));
    }

    @Operation(summary = "发表评论")
    @PostMapping
    public R<Void> create(@RequestBody NovelComment comment,
                          @AuthenticationPrincipal LoginUser loginUser) {
        comment.setUserId(loginUser.getUserId());
        comment.setUserName(loginUser.getUsername());
        commentService.addComment(comment);
        return R.ok();
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        commentService.deleteComment(id, loginUser.getUserId());
        return R.ok();
    }
}

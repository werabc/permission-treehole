package com.permission.controller;

import com.permission.common.R;
import com.permission.framework.storage.FileStorageProperties;
import com.permission.framework.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件上传 / 读取。
 *
 * 目前只开放头像一类。上传要求登录（鉴权在 SecurityConfig 中统一要求），
 * 读取公开（<img> 不带 Authorization 头），安全性靠服务端生成的随机文件名。
 */
@Tag(name = "文件", description = "头像上传与读取")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileStorageProperties fileStorageProperties;

    @Operation(summary = "上传头像", description = "仅支持 JPG/PNG/GIF/WebP，默认不超过 2MB，返回可直接用于 <img> 的相对地址")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file, "avatar", fileStorageProperties.getAvatarMaxSize());
        return R.ok(url);
    }

    @Operation(summary = "读取文件")
    @GetMapping("/{category}/{filename}")
    public void read(@PathVariable String category, @PathVariable String filename,
                     HttpServletResponse response) throws IOException {
        Resource resource = fileStorageService.load(category, filename);
        if (resource == null) {
            // 统一 404，不区分"分类不存在/文件不存在/文件名非法"，避免把探测结果回显给攻击者。
            // 注意用 setStatus 而不是 sendError：sendError 会触发一次 ERROR 派发到 /error，
            // 而 /error 若未放行，最终状态码会被改写成 401，把"文件不存在"伪装成"未登录"。
            response.reset();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 为什么手写响应而不是 return ResponseEntity：
        // application.yml 里 server.servlet.encoding.force=true 会强制给每个响应设置字符编码，
        // 于是图片的 Content-Type 变成 "image/png;charset=UTF-8"（二进制资源不该带 charset）。
        // contentType()/header() 都压不住，必须先 reset() 清掉过滤器写入的编码再设置类型。
        response.reset();
        response.setContentType(fileStorageService.contentTypeOf(filename));
        // 文件名由 UUID 生成、内容永不变，可以放心长缓存
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000, immutable");
        response.setContentLengthLong(resource.contentLength());

        try (InputStream in = resource.getInputStream()) {
            in.transferTo(response.getOutputStream());
        }
    }
}

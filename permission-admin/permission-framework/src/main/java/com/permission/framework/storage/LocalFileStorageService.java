package com.permission.framework.storage;

import com.permission.common.ResultCode;
import com.permission.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地磁盘存储实现。
 *
 * 安全约束（每一条都对应一类真实攻击）：
 * 1. 绝不使用客户端文件名 —— 否则 ../../etc/passwd 或 shell 字符直接可用；
 * 2. 靠**文件头魔数**判类型，不信 Content-Type（浏览器可伪造）与扩展名；
 * 3. 明确拒绝 SVG —— SVG 可内嵌 <script>，当图片直出会造成存储型 XSS；
 * 4. 读取时 category 白名单 + 文件名严格正则 + 归一化后再确认位于根目录内。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;

    /** 允许的分类白名单：既是目录名，也是读取时的准入条件 */
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("avatar");

    /** 服务端生成的文件名：32 位 hex + 白名单扩展名，天然不含 / 、. 与 .. */
    private static final Pattern SAFE_FILENAME =
            Pattern.compile("^[0-9a-f]{32}\\.(jpg|gif|png|webp)$");

    private static final int SNIFF_LENGTH = 12;

    private Path root;

    @PostConstruct
    void init() {
        root = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("文件存储根目录: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建文件存储目录: " + root, e);
        }
    }

    @Override
    public boolean isAllowedCategory(String category) {
        return category != null && ALLOWED_CATEGORIES.contains(category);
    }

    @Override
    public String storeImage(MultipartFile file, String category, long maxSize) {
        if (!isAllowedCategory(category)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件分类");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "文件不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }

        String ext;
        try (InputStream in = file.getInputStream()) {
            ext = sniffImageExtension(in);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件读取失败");
        }
        if (ext == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "只支持 JPG / PNG / GIF / WebP 格式的图片");
        }

        // 文件名一律由服务端生成，客户端提供的 name 只用于日志排查
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = root.resolve(category).resolve(filename).normalize();
        if (!target.startsWith(root)) {
            // 理论上不会发生（名字是自己生成的），留一道兜底
            throw new BusinessException(ResultCode.BAD_REQUEST, "非法的文件路径");
        }

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            log.error("保存文件失败 path={}", target, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }

        log.info("文件已保存 category={} size={}B clientName={}", category, file.getSize(), file.getOriginalFilename());
        return properties.getPublicPrefix() + "/" + category + "/" + filename;
    }

    @Override
    public Resource load(String category, String filename) {
        if (!isAllowedCategory(category) || filename == null
                || !SAFE_FILENAME.matcher(filename).matches()) {
            return null;
        }
        Path target = root.resolve(category).resolve(filename).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            return null;
        }
        return new PathResource(target);
    }

    @Override
    public String contentTypeOf(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    /**
     * 读文件头魔数判断真实图片类型，返回扩展名；不是受支持的图片返回 null。
     * 只读前 12 字节，大文件也不会有额外开销。
     */
    private String sniffImageExtension(InputStream in) throws IOException {
        byte[] head = new byte[SNIFF_LENGTH];
        int read = in.readNBytes(head, 0, SNIFF_LENGTH);
        if (read < 12) {
            return null;
        }
        // JPEG: FF D8 FF
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G'
                && (head[4] & 0xFF) == 0x0D && (head[5] & 0xFF) == 0x0A
                && (head[6] & 0xFF) == 0x1A && (head[7] & 0xFF) == 0x0A) {
            return "png";
        }
        // GIF: "GIF8"
        if (head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') {
            return "gif";
        }
        // WebP: "RIFF" .... "WEBP"
        if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        // 其余（含 SVG、HTML、可执行文件）一律拒绝
        return null;
    }
}

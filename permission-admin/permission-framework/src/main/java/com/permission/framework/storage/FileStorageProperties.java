package com.permission.framework.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置。
 *
 * 全部可通过环境变量/配置覆盖，避免把磁盘路径写死在代码里：
 *   file.storage.dir          本地存储根目录（默认 ./uploads，相对应用启动目录）
 *   file.storage.public-prefix 对外访问前缀，必须与 SecurityConfig 的放行规则保持一致
 *   file.storage.avatar-max-size 头像大小上限（字节），默认 2MB
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /** 本地存储根目录 */
    private String dir = "uploads";

    /** 对外访问前缀，最终返回给前端的形如 /api/file/avatar/xxx.jpg */
    private String publicPrefix = "/api/file";

    /** 头像大小上限（字节） */
    private long avatarMaxSize = 2L * 1024 * 1024;
}

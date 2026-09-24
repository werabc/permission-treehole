package com.permission.framework.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务。
 *
 * 设计要点：
 * 1. 只做"图片"这一类，且靠**文件头魔数**判定真实类型，不信 Content-Type 与扩展名；
 * 2. 落盘文件名由服务端生成（UUID），完全丢弃客户端文件名 —— 从根上消除路径穿越；
 * 3. 读取时对 category 白名单 + 文件名正则双重校验，并再次确认最终路径在根目录之内。
 */
public interface FileStorageService {

    /**
     * 保存一张图片。
     *
     * @param file     上传的文件
     * @param category 业务分类（如 avatar），同时作为一级目录
     * @param maxSize  大小上限（字节），超限抛业务异常
     * @return 可直接给前端使用的相对地址，如 /api/file/avatar/1f2e....jpeg
     */
    String storeImage(MultipartFile file, String category, long maxSize);

    /**
     * 读取文件。
     *
     * @return 文件资源；不存在返回 null
     */
    Resource load(String category, String filename);

    /** 依据扩展名推断 Content-Type，未知返回 application/octet-stream */
    String contentTypeOf(String filename);

    /** category 是否是允许的取值 */
    boolean isAllowedCategory(String category);
}

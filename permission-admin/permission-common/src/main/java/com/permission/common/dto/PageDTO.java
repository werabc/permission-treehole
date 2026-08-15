package com.permission.common.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageDTO {

    @Min(value = 1, message = "页码最小为1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    private long pageSize = 10;

    private String keyword;
}

// ============================================================
// 文件注解与作用说明
// ============================================================
// 【文件路径】com.permission.common.dto.PageDTO
// 【模块】permission-common（公共基础模块）
//
// 【使用的注解/技术】
//   - @Data — Lombok，生成 getter/setter
//   - @Min — Jakarta Bean Validation，校验 pageNum / pageSize ≥ 1，拒绝负数或零
//
// 【关联文件】
//   - 被各 Controller 列表接口作为 @RequestBody 参数接收 → controller/*Controller.java
//   - 被 Service 层用于构造 MyBatis-Plus IPage → service/impl/*ServiceImpl.java
//   - 与 PageVO（若存在）成对使用返回分页结果
//
// 【核心作用】分页查询的通用请求参数对象，封装页码、页长、关键字。
//
// 【设计必要性】统一字段名 pageNum/pageSize 让前端和 Service 约定一致；
//   @Min 防御恶意参数导致 OFFSET 异常或全量查询。
//
// 【注意事项】
//   - 默认值 pageNum=1 / pageSize=10，前端可省略传参
//   - 未做 pageSize 上界，业务层可再限制防止一次拉取过大
// ============================================================

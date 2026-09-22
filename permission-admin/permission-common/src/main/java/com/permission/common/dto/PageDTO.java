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


package com.permission.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportDTO {

    private Long id;
    private String username;
    private String nickname;
    private String deptName;
    private String email;
    private String phone;
    private String sexLabel;
    private String statusLabel;
    private String lastLoginTime;
    private String createTime;
}

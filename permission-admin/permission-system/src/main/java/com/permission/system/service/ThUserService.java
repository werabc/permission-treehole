package com.permission.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.permission.common.dto.LoginDTO;
import com.permission.common.entity.ThUser;

import java.util.Map;

public interface ThUserService extends IService<ThUser> {

    ThUser register(LoginDTO loginDTO);

    Map<String, String> login(LoginDTO loginDTO);

    ThUser getUserById(Long id);
}

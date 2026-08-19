package com.permission.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.entity.ThSetting;
import com.permission.system.mapper.ThSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站点配置服务 — 带缓存，避免每次请求都查数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThSettingsService {

    private final ThSettingMapper settingMapper;

    /** 配置缓存 */
    private final Map<String, String> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    /** 刷新缓存 */
    public void refresh() {
        cache.clear();
        List<ThSetting> settings = settingMapper.selectList(new LambdaQueryWrapper<ThSetting>());
        for (ThSetting s : settings) {
            cache.put(s.getSettingKey(), s.getSettingValue());
        }
        log.info("站点配置已加载，共 {} 项", cache.size());
    }

    /** 获取配置值（带缓存） */
    public String get(String key) {
        return cache.get(key);
    }

    /** 获取配置值，带默认值 */
    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    /** 判断开关是否为开启（"1" 或 "true" 视为开启） */
    public boolean isEnabled(String key) {
        String val = cache.get(key);
        return "1".equals(val) || "true".equalsIgnoreCase(val);
    }

    /** 判断开关是否为关闭 */
    public boolean isDisabled(String key) {
        return !isEnabled(key);
    }

    /** 检查是否允许注册 */
    public boolean isRegistrationAllowed() {
        // 默认允许注册，只有明确设置为 "0" 才禁止
        return !"0".equals(cache.get("register_enabled"));
    }

    /** 检查是否允许匿名发帖 */
    public boolean isAnonymousAllowed() {
        // 默认允许匿名，只有明确设置为 "0" 才禁止
        return !"0".equals(cache.get("anonymous_enabled"));
    }

    /** 获取站点名称 */
    public String getSiteName() {
        return cache.getOrDefault("site_name", "树洞");
    }

    /** 获取完整缓存（供管理后台使用） */
    public Map<String, String> getAll() {
        return new HashMap<>(cache);
    }
}

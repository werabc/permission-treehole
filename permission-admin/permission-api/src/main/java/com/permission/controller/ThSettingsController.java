package com.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.permission.common.R;
import com.permission.common.entity.ThSetting;
import com.permission.system.mapper.ThSettingMapper;
import com.permission.system.service.ThSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "树洞站点配置")
@RestController
@RequestMapping("/api/admin/th/settings")
@RequiredArgsConstructor
public class ThSettingsController {

    private final ThSettingMapper settingMapper;
    private final ThSettingsService settingsService;

    @Operation(summary = "获取所有配置")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('th:settings:view', 'admin')")
    public R<Map<String, String>> getAll() {
        return R.ok(settingsService.getAll());
    }

    @Operation(summary = "更新配置")
    @PutMapping
    @PreAuthorize("hasAnyAuthority('th:settings:edit', 'admin')")
    public R<Void> update(@RequestBody Map<String, String> body) {
        for (Map.Entry<String, String> entry : body.entrySet()) {
            ThSetting setting = settingMapper.selectOne(new LambdaQueryWrapper<ThSetting>()
                    .eq(ThSetting::getSettingKey, entry.getKey()));
            if (setting != null) {
                setting.setSettingValue(entry.getValue());
                settingMapper.updateById(setting);
            } else {
                setting = new ThSetting();
                setting.setSettingKey(entry.getKey());
                setting.setSettingValue(entry.getValue());
                settingMapper.insert(setting);
            }
        }
        // 保存后刷新缓存，立即生效
        settingsService.refresh();
        return R.ok();
    }
}

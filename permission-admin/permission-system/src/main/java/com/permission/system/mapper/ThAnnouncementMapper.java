package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.ThAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ThAnnouncementMapper extends BaseMapper<ThAnnouncement> {

    /**
     * 查询当前生效的公告（status=1，未过期）
     */
    @Select("SELECT * FROM th_announcement WHERE deleted = 0 AND status = 1 " +
            "AND (expire_time IS NULL OR expire_time > NOW()) " +
            "ORDER BY create_time DESC LIMIT 20")
    List<ThAnnouncement> selectActiveAnnouncements();
}

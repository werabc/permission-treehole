package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.ThComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ThCommentMapper extends BaseMapper<ThComment> {

    @Update("UPDATE th_comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE th_comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);
}

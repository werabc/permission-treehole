package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.ThPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ThPostMapper extends BaseMapper<ThPost> {

    /**
     * 按分类统计「对普通用户可见」的帖子数（status=1 且未逻辑删除）。
     *
     * th_category.post_count 这一列历史上没有任何代码维护，前端直接读会和
     * 「共 N 条」对不上；分类数量很少，改成就地 GROUP BY 实时聚合最省事也最准。
     */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM th_post "
            + "WHERE deleted = 0 AND status = 1 AND category_id IS NOT NULL GROUP BY category_id")
    List<Map<String, Object>> countVisibleGroupByCategory();

    @Update("UPDATE th_post SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE th_post SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    @Update("UPDATE th_post SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Long id);

    @Update("UPDATE th_post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE th_post SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = #{id}")
    int decrementCommentCount(@Param("id") Long id);

    @Update("UPDATE th_post SET report_count = report_count + 1 WHERE id = #{id}")
    int incrementReportCount(@Param("id") Long id);
}

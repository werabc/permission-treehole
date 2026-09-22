package com.permission.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.permission.common.entity.ThUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ThUserMapper extends BaseMapper<ThUser> {

    /** 发帖数 +1（原子操作，避免并发覆盖） */
    @Update("UPDATE th_user SET post_count = post_count + 1 WHERE id = #{id}")
    int incrementPostCount(@Param("id") Long id);

    /** 发帖数 -1，带下限保护 */
    @Update("UPDATE th_user SET post_count = GREATEST(post_count - 1, 0) WHERE id = #{id}")
    int decrementPostCount(@Param("id") Long id);

    /** 评论数 +1 */
    @Update("UPDATE th_user SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Long id);

    /** 评论数 -1，带下限保护 */
    @Update("UPDATE th_user SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = #{id}")
    int decrementCommentCount(@Param("id") Long id);
}

package com.pan.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pan.pojo.entity.FileShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author 31696
* @description 针对表【file_share】的数据库操作Mapper
* @createDate 2025-01-15 17:27:43
* @Entity com.pan.pojo.entity.FileShare
*/
@Mapper
public interface FileShareMapper extends MPJBaseMapper<FileShare> {

    @Update("update file_share set show_count = show_count + 1 where share_id = #{shareId}")
    void updateShareShowCount(@Param("shareId") String shareId);
}





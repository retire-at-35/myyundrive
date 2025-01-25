package com.pan.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pan.pojo.entity.FileInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
* @author 31696
* @description 针对表【file_info】的数据库操作Mapper
* @createDate 2025-01-06 17:41:57
* @Entity com.pan.pojo.entity.FileInfo
*/
@Mapper
public interface FileInfoMapper extends MPJBaseMapper<FileInfo> {

    @Select("select ifnull(sum(file_size),0) from mypan.file_info where user_id = #{userId}")
    Long selectUserSpace(@Param("userId")String userId);


    Long selectSpaceSizeByfileIds(String userId, List<String> fileIdList);

    void insertBatch(List<FileInfo> resultFileList);


    Long selectFolderSize(@Param("children") List<String> children,Integer type);
}





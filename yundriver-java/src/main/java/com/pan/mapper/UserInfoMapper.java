package com.pan.mapper;

import com.pan.pojo.entity.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author 31696
* @description 针对表【user_info】的数据库操作Mapper
* @createDate 2024-12-29 22:56:14
* @Entity com.pan.pojo.entity.UserInfo
*/
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    @Update("update user_info set password = #{password} where email = #{email}")
    void forgetPassword(@Param("password") String password, @Param("email") String email);


    Integer updateUserSpace(@Param("userId") String userId,@Param("userSpace") Long userSpace,@Param("totalSpace") Long totalSpace);
}





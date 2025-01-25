package com.pan.mapper;

import com.pan.pojo.entity.EmailCode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 31696
* @description 针对表【email_code(邮箱验证码)】的数据库操作Mapper
* @createDate 2024-12-31 18:00:28
* @Entity com.pan.pojo.entity.EmailCode
*/
@Mapper
public interface EmailCodeMapper extends BaseMapper<EmailCode> {


    void disableLast(@Param("email") String email);
}





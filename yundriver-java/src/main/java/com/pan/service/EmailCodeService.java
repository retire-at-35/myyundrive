package com.pan.service;

import com.pan.pojo.entity.EmailCode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 31696
* @description 针对表【email_code(邮箱验证码)】的数据库操作Service
* @createDate 2024-12-31 18:00:28
*/
public interface EmailCodeService extends IService<EmailCode> {

    void sendEmailCode(String email, Integer type);

    void checkCode(String email, String emailCode);

}

package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import com.pan.pojo.enums.VerifyRegexEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * 用于接收注册接口参数的 DTO 类
 */
@Data // 自动生成 Getter、Setter、toString、equals 和 hashCode 方法
@NoArgsConstructor // 生成无参构造器
@AllArgsConstructor // 生成全参构造器
public class RegisterDto {

    /**
     * 用户邮箱
     */
    @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) // 必填，需符合邮箱格式
    private String email;

    /**
     * 用户昵称
     */
    @VerifyParam(required = true)
    // 必填
    private String nickName;

    /**
     * 用户密码
     */
    @VerifyParam(required = true) // 可选，但需符合密码正则规则
    private String password;

    /**
     * 验证码（比如图片验证码）
     */
    @VerifyParam(required = true) // 必填
    private String checkCode;

    /**
     * 邮件验证码
     */
    @VerifyParam(required = true) // 必填
    private String emailCode;
}

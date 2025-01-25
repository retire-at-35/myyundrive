package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import com.pan.pojo.enums.VerifyRegexEnum;
import lombok.Data;

@Data
public class CodeDto {
    @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL)
    private String email;
    @VerifyParam(required = true)
    private String checkCode;
    // type=0时为注册,1是找回密码
    @VerifyParam(required = true)
    private Integer type;
}

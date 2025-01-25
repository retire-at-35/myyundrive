package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import com.pan.pojo.enums.VerifyRegexEnum;
import lombok.Data;

@Data
public class LoginDto {
    @VerifyParam(regex = VerifyRegexEnum.EMAIL)
    private String email;

    @VerifyParam(required = true)
    private String password;

    @VerifyParam(required = true)
    private String checkCode;
}

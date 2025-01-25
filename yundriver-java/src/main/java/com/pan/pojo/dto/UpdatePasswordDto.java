package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import com.pan.pojo.enums.VerifyRegexEnum;
import lombok.Data;

@Data
public class UpdatePasswordDto {
    @VerifyParam(required = true)
    private String oldPassword;
    @VerifyParam(regex = VerifyRegexEnum.PASSWORD)
    private String password;
    @VerifyParam(required = true)
    private String checkCode;
}

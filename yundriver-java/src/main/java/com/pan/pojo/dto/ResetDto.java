package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;

@Data
public class ResetDto {
    @VerifyParam(required = true)
    private String email;
    @VerifyParam(required = true)
    private String password;
    @VerifyParam(required = true)
    private String emailCode;
    @VerifyParam
    private String checkCode;
}

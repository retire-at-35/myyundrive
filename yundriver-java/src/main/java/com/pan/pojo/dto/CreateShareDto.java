package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;

@Data
public class CreateShareDto {
    @VerifyParam(required = true)
    String fileId;
    @VerifyParam(required = true)
    Integer validType;
    String code;
}

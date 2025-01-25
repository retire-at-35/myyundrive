package com.pan.pojo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pan.constant.Constant;
import lombok.Data;

import java.io.Serializable;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SystemDto implements Serializable {
    private String title = "邮箱验证码";
    private String emailContent = "您好,您的邮箱验证码是:%s,15分钟有效,请妥善保管";

    private Long InitSpace = Constant.MB * 1024L;
}

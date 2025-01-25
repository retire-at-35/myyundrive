package com.pan.pojo.dto;

import lombok.Data;

@Data
public class UserSpaceDto {
    // 用户已经使用的空间
    private Long userSpace;
    // 用户总空间
    private Long totalSpace;

}

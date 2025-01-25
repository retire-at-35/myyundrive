package com.pan.pojo.dto;

import lombok.Data;

@Data
public class UpdateUserStatusDto {
    private String userId;
    private Integer status;
}

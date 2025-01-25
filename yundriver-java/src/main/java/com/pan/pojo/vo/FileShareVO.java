package com.pan.pojo.vo;

import lombok.Data;

import java.util.Date;

@Data
public class FileShareVO {
    private String shareId;
    private String userName;
    private String avatar;
    private Date shareTime;
    private String userId;
}

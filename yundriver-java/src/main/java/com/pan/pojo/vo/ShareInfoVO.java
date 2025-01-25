package com.pan.pojo.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ShareInfoVO {
    private Date shareTime;
    private Date expireTime;
    private String nickName;
    private String fileName;
    private Boolean currentUser;
    private String fileId;
    private String avatar;
    private String userId;
}

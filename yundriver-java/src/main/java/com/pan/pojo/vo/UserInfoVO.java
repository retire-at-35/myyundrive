package com.pan.pojo.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserInfoVO implements Serializable {
    private String userId;
    private String nickName;
    private String email;
    private String avatar;
    private Date joinTime;
    private Date lastLoginTime;
    private Integer status; // 0禁用,1启用
    private Long userSpace;
    private Long totalSpace;
}

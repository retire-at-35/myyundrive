package com.pan.pojo.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pan.annotation.VerifyParam;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * @TableName user_info
 */
@TableName(value ="user_info")
@Data
public class UserInfo implements Serializable {
    @TableId
    private String userId;

    private String nickName;

    @VerifyParam(required = true)
    private String email;

    private String avatar;

    private String password;

    private Date joinTime;

    private Date lastLoginTime;

    private Integer status;

    private Long userSpace;

    private Long totalSpace;

    private static final long serialVersionUID = 1L;
}
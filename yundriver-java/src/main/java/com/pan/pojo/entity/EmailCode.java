package com.pan.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName email_code
 */
@TableName(value ="email_code")
@Data
public class EmailCode implements Serializable {
    private String email;

    private String code;

    private Date createTime;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
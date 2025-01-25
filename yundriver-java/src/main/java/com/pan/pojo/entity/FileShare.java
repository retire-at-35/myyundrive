package com.pan.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName file_share
 */
@TableName(value ="file_share")
@Data
public class FileShare implements Serializable {
    @TableId
    private String shareId;

    private String fileId;

    private String userId;

    // 有效期类型
    private Integer validType;

    private Date expireTime;

    private Date shareTime;

    // 提取码
    private String code;

    // 浏览次数
    private Integer showCount;

    @TableField(exist = false)
    private String fileName;

    @TableField(exist = false)
    private String fileCover;

    private static final long serialVersionUID = 1L;
}
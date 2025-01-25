package com.pan.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.ToString;

/**
 * @TableName file_info
 */
@TableName(value ="file_info")
@Data
@ToString
public class FileInfo implements Serializable {
    private String userId;

    private String fileId;

    private String fileMd5;

    private String filePid;

    private Long fileSize;

    private String fileName;

    private String fileCover;

    private String filePath;

    private Date createTime;

    private Date lastUpdateTime;

    private Integer folderType;

    private Integer fileCategory;

    private Integer fileType;

    private Integer status;

    private Date recoverTime;

    private Integer delFlag;

    // 文件所属用户的邮箱
    @TableField(exist = false)
    private String email;

    private static final long serialVersionUID = 1L;
}
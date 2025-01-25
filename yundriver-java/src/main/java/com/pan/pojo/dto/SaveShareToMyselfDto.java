package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;

@Data
public class SaveShareToMyselfDto {
    @VerifyParam(required = true)
    private String fileIds; // 所选中的分享文件id
    @VerifyParam(required = true)
    private String filePid; // 目标文件夹的id
    @VerifyParam(required = true)
    private String userId; // 分享人的id
}

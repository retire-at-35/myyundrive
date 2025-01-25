package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;

@Data
public class NewFolderDto {

    @VerifyParam(required = true)
    private String filePid;
    @VerifyParam(required = true)
    private String folderName;
}

package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;

@Data
public class FileRenameDto {
    @VerifyParam(required = true)
    private String fileId;
    @VerifyParam(required = true)
    private String fileName;
}

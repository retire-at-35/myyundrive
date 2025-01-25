package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadDto {
    private String fileId;
    @VerifyParam(required = true)
    private String filePid;
    @VerifyParam(required = true)
    private String fileName;
    @VerifyParam(required = true)
    private String fileMd5;
    @VerifyParam(required = true)
    private Integer chunkIndex;
    @VerifyParam(required = true)
    private Integer chunks;
    private MultipartFile file;
}

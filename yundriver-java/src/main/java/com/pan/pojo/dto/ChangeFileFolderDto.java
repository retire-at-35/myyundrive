package com.pan.pojo.dto;

import com.pan.annotation.VerifyParam;
import lombok.Data;

@Data
public class ChangeFileFolderDto {
   @VerifyParam(required = true)
   private  String fileIds;// 多选的文件id
   @VerifyParam(required = true)
   private  String filePid;// 目的地文件id
}

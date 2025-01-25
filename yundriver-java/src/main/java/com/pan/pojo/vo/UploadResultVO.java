package com.pan.pojo.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UploadResultVO implements Serializable {
    private static final long serialVersionUID = -6849794470754567710L;

    private String fileId;
    private String status; // 上传状态
}

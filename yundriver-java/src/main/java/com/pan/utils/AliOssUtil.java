package com.pan.utils;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.pan.pojo.properties.AliOssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class AliOssUtil {

    @Autowired
    private AliOssProperties aliProperties;


    // 支持的图片类型列表
    private static final List<String> IMAGE_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    );
    /**
     * 实现上传图片到OSS
     */
    public String upload(MultipartFile file) throws IOException {
        validateImageFile(file);
        String endpoint = aliProperties.getEndpoint();
        String bucketName = aliProperties.getBucketName();
        String accessKeyId = aliProperties.getAccessKeyId();
        String accessKeySecret = aliProperties.getAccessKeySecret();
        // 获取上传的文件的输入流
        InputStream inputStream = file.getInputStream();

        // 避免文件覆盖
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + originalFilename.substring(originalFilename.lastIndexOf("."));

        //上传文件到 OSS
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        ossClient.putObject(bucketName, fileName, inputStream);
//        https://web-tlias-yaojiongdong.oss-cn-guangzhou.aliyuncs.com/137de07c-aef2-4582-a775-8543a1db3009.jpg
        //文件访问路径
        String url = "https://"+bucketName+"."+endpoint+"/"+fileName;
        // 关闭ossClient
        ossClient.shutdown();
        log.info("文件路径为,{}",url);
        return url;// 把上传到oss的路径返回
    }


    /**
     * 验证文件是否是图片类型
     */
    private void validateImageFile(MultipartFile file) {
        // 获取文件的 MIME 类型
        String contentType = file.getContentType();

        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType)) {
            log.error("上传的文件类型不合法，文件类型为: {}", contentType);
            throw new RuntimeException("文件类型不合法，请上传图片文件");
        }

        // 检查文件扩展名（可选，增加一层安全性）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(fileExtension)) {
                log.error("上传的文件扩展名不合法，文件名为: {}", originalFilename);
                throw new RuntimeException("文件扩展名不合法，请上传图片文件");
            }
        }
    }
}


package com.pan.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScaleFilter {
    private static Logger logger = LoggerFactory.getLogger(ScaleFilter.class);
    public static void createCover4Video(File sourceFile,Integer width,File targetFile) {
        try {
//            String cmd = "ffmpeg -i %s -y -vframes 1 -vf scale=%d:%d/a %s";
            String cmd = "ffmpeg -i %s -y -vframes 1 -vf scale=%d:trunc(ow/a/2)*2 %s";
            ProcessUtil.executeCommand(String.format(cmd,sourceFile.getAbsoluteFile(),width,width,targetFile.getAbsoluteFile()));
        } catch (Exception e) {
            throw new RuntimeException("生成视频封面失败");
        }
    }

    public static Boolean createThumbnailWidthFFmpeg(File file,int thumbnailWidth,File targetFile,Boolean delSource){
        try {
            BufferedImage src = ImageIO.read(file);
            // thumbnailWidth缩略图的宽度
            int sourceW = src.getWidth();
            int sourceH = src.getHeight();
            // 小于 指定高宽不压缩
            if(sourceW <= thumbnailWidth){
                return false;
            }
            compressImage(file,thumbnailWidth,targetFile,delSource);
            return true;
        } catch (Exception e) {
            logger.info("文件缩略图生成异常,{}",e.getMessage());
        }
        return false;
    }

    public static void compressImage(File sourceFile,Integer width,File targetFile,Boolean delSource){
        try {
            String cmd = "ffmpeg -i %s -vf scale=%d:-1 %s -y";
            ProcessUtil.executeCommand(String.format(cmd,sourceFile.getAbsoluteFile(),width,targetFile.getAbsoluteFile()));
            if (delSource){
                FileUtils.forceDelete(sourceFile);
            }
        } catch (Exception e) {
            logger.info("压缩图片失败");
        }
    }
}

package com.pan.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pan.component.RedisComponent;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.pojo.dto.DownloadFileDto;
import com.pan.pojo.entity.FileInfo;
import com.pan.pojo.enums.DelFlagEnum;
import com.pan.pojo.enums.FileCategoryEnum;
import com.pan.pojo.enums.FileFolderTypeEnum;
import com.pan.pojo.properties.AppProperties;
import com.pan.pojo.vo.Result;
import com.pan.service.FileInfoService;
import com.pan.service.impl.FileInfoServiceImpl;
import com.pan.utils.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CommonController {
    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);
    @Resource
    private AppProperties appProperties;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;
    protected void getImage(HttpServletResponse response,String imageFolder,String imageName){
        if (StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName) || !StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = appProperties.getFolder()+ Constant.FILE_FOLDER + imageFolder
                 + "/" +imageName;
        imageSuffix = imageSuffix.replace(".","");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Controller","max-age=2592000");
        readFile(response,filePath);
    }

    protected void getFile(HttpServletResponse response,String fileId,String userId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            FileInfo fileInfo = fileInfoService.getOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, realFileId));
            String fileName = fileInfo.getFilePath();
            // 202501/yzImdv7QdbofPSY1lu.mp4
            fileName = StringTools.extractFilePath(fileName)+"/"+fileId;
            filePath = appProperties.getFolder()+Constant.FILE_FOLDER+fileName;
            logger.info("ts文件的目录为{}",filePath);
        } else {
            FileInfo fileInfo = fileInfoService.getOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId, fileId).eq(FileInfo::getUserId, userId));
            if (null == fileInfo) {
                return;
            }
            // 视频文件单独处理
            if (FileCategoryEnum.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringTools.extractFilePath(fileInfo.getFilePath());
                filePath = appProperties.getFolder() + Constant.FILE_FOLDER + fileNameNoSuffix + "/" + Constant.M3U8_NAME;
                logger.info("m3u8文件的目录为{}",filePath);
            }
            else {
                filePath = appProperties.getFolder()+Constant.FILE_FOLDER+fileInfo.getFilePath();
            }
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
        }
        readFile(response, filePath);
    }

    void readFile(HttpServletResponse response,String filePath){
        if (!StringTools.pathIsOk(filePath)){
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;

        try {
            File file = new File(filePath);
            if (!file.exists()){
                return;
            }
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while (((len = in.read(byteData)) != -1)){
                out.write(byteData,0,len);
            }
            out.flush();
        } catch (Exception e) {
            logger.info("io异常",e);
        } finally {
            if (out != null){
                try {
                    out.close();
                } catch (Exception e) {
                    logger.info("io异常",e);
                }
            }
            if (in != null){
                try {
                    in.close();
                } catch (Exception e) {
                    logger.info("io异常",e);
                }
            }
        }
    }


    protected Result getAllFolderInfo(String userId,String filePid){
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getFilePid,filePid)
                .eq(FileInfo::getDelFlag, DelFlagEnum.NORMAL.getStatus())
                .eq(FileInfo::getFolderType,FileFolderTypeEnum.FOLDER.getType());
        List<FileInfo> result = fileInfoService.list(query);
        return Result.success(result);
    }

    protected Result createDownloadUrl(String fileId,String userId){
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,userId).eq(FileInfo::getFileId,fileId);
        FileInfo fileInfo = fileInfoService.getOne(query);
        if (null == fileInfo){
            throw new RuntimeException("未知异常");
        }
        if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())){
            throw new RuntimeException("未知异常");
        }
        String code = StringTools.getRandomString(50);
        // 放到redis里面
        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setDownloadCode(code);
        fileDto.setFilePath(fileInfo.getFilePath());
        fileDto.setFileName(fileInfo.getFileName());
        redisComponent.saveDownCode(fileDto);
        return Result.success(code);
    }

//    public void download(HttpServletRequest request, HttpServletResponse response,  String fileIds,String userId) throws UnsupportedEncodingException {
//        String[] fileIdArray = fileIds.split(",");
//        // 合法性效验
//        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
//        query.eq(FileInfo::getUserId,userId).in(FileInfo::getFileId,fileIdArray);
//        List<FileInfo> list = fileInfoService.list(query);
//        if (list.size() != fileIdArray.length){
//            throw new BussinessException("未知异常");
//        }
//        // 查询每个文件的路径
//        for (FileInfo fileInfo : list) {
//            String path = appProperties.getFolder() + Constant.FILE_FOLDER + fileInfo.getFilePath();
//            String fileName = fileInfo.getFileName();
//            response.setContentType("application/x-msdownload; charset=UTF-8");
//            if (request.getHeader("User-Agent").toLowerCase().indexOf("mise")>0){
//                fileName = URLEncoder.encode(fileName,"UTF-8");
//            }
//            else {
//                fileName = new String(fileName.getBytes("UTF-8"),"ISO8859-1");
//            }
//            response.setHeader("Content-Disposition","attachment;filename=\""+fileName+"\"");
//            readFile(response,path);
//        }
//    }

    public void download(HttpServletRequest request, HttpServletResponse response, String fileIds, String userId) throws UnsupportedEncodingException {
        String[] fileIdArray = fileIds.split(",");

        // 合法性效验
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, fileIdArray);
        List<FileInfo> list = fileInfoService.list(query);

        if (list.size() != fileIdArray.length) {
            throw new BussinessException("未知异常");
        }

        // 设置响应为ZIP文件
        response.setContentType("application/zip");

        String zipFileName = "files.zip"; // 默认的ZIP文件名
        if (request.getHeader("User-Agent").toLowerCase().indexOf("mise") > 0) {
            zipFileName = URLEncoder.encode(zipFileName, "UTF-8");
        } else {
            zipFileName = new String(zipFileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[1024];

            for (FileInfo fileInfo : list) {
                // 判断文件类型：文件夹或文件
                if (fileInfo.getFolderType().equals(FileFolderTypeEnum.FILE.getType())) {
                    // 是文件，直接添加到ZIP
                    addFileToZip(fileInfo, zos, buffer, "");
                } else {
                    // 是文件夹，递归获取文件夹内所有文件，保留层级结构
                    addFolderToZip(fileInfo, zos, buffer, "");
                }
            }
            zos.flush();
        } catch (IOException e) {
            logger.info("IO异常", e);
        }
    }

    /**
     * 将文件添加到ZIP压缩包
     */
    private void addFileToZip(FileInfo fileInfo, ZipOutputStream zos, byte[] buffer, String parentPath) throws IOException {
        String filePath = appProperties.getFolder() + Constant.FILE_FOLDER + fileInfo.getFilePath();
        File file = new File(filePath);

        if (file.exists()) {
            // 创建文件的路径并添加到 ZIP 中，路径要包括文件夹结构
            String zipEntryPath = parentPath + fileInfo.getFileName();
            ZipEntry zipEntry = new ZipEntry(zipEntryPath);
            zos.putNextEntry(zipEntry);

            try (FileInputStream fis = new FileInputStream(file)) {
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }

    /**
     * 递归地将文件夹内的文件（包括子文件夹中的文件）添加到ZIP压缩包
     */
    private void addFolderToZip(FileInfo folderInfo, ZipOutputStream zos, byte[] buffer, String parentPath) throws IOException {
        // 获取当前文件夹内的所有文件和子文件夹
        List<FileInfo> folderFiles = getFilesInFolder(folderInfo.getFileId());

        for (FileInfo folderFile : folderFiles) {
            // 计算当前文件的路径（保留文件夹结构）
            String newPath = parentPath + folderInfo.getFileName() + "/";

            // 如果是文件，则直接添加到ZIP
            if (folderFile.getFolderType().equals(FileFolderTypeEnum.FILE.getType())) {
                addFileToZip(folderFile, zos, buffer, newPath);
            } else {
                // 如果是文件夹，则递归处理该文件夹
                addFolderToZip(folderFile, zos, buffer, newPath);
            }
        }
    }

    /**
     * 根据 fileId 获取文件夹中的所有文件
     */
    private List<FileInfo> getFilesInFolder(String folderId) {
        LambdaQueryWrapper<FileInfo> folderQuery = new LambdaQueryWrapper<>();
        folderQuery.eq(FileInfo::getFilePid, folderId); // 通过 filePid 获取文件夹下的文件
        return fileInfoService.list(folderQuery);
    }



}

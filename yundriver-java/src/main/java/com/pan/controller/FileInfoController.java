package com.pan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pan.annotation.GlobalInterceptor;
import com.pan.annotation.VerifyParam;
import com.pan.constant.Constant;
import com.pan.pojo.dto.*;
import com.pan.pojo.entity.FileInfo;
import com.pan.pojo.enums.DelFlagEnum;
import com.pan.pojo.enums.FileCategoryEnum;
import com.pan.pojo.enums.FileFolderTypeEnum;
import com.pan.pojo.enums.FileTypeEnum;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.Result;
import com.pan.pojo.vo.UploadResultVO;
import com.pan.service.FileInfoService;
import com.pan.utils.StringTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.List;


@RestController
@RequestMapping("/file")
public class FileInfoController extends CommonController{
    @Resource
    private FileInfoService fileInfoService;

    @GetMapping("/loadPageList")
    @GlobalInterceptor(checkParams = true)
    public Result loadDataList(HttpSession session,
                               String fileName,
                               @VerifyParam(required = true) String filePid,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize
    )
    {
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        String userId = dto.getUserId();
        PageBean result = fileInfoService.loadDataList(fileName,filePid, userId, page, pageSize);
        return Result.success(result);
    }

    // 通过文件夹的id查询文件夹的总大小
    @GetMapping("/getFolderSpace")
    @GlobalInterceptor
    public Result getFolderSpace(@VerifyParam(required = true) String folderId,Integer type){
        Long folderSpace = fileInfoService.getFolderSpace(folderId,type);
        return Result.success(folderSpace);
    }

    @GetMapping("/loadSharerList")
    @GlobalInterceptor(checkParams = true)
    public Result loadDataList(@VerifyParam(required = true) String userId,
                               @VerifyParam(required = true) String filePid,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize
    )
    {
        PageBean result = fileInfoService.loadDataList2(filePid, userId, page, pageSize);
        return Result.success(result);
    }


    @GetMapping("/loadByFileIds")
    @GlobalInterceptor(checkParams = true)
    public Result loadByFileIds(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @VerifyParam(required = true) String fileIds,
                                @VerifyParam(required = true) String userId){
        PageBean result = fileInfoService.loadByFileIds(page,pageSize,fileIds,userId);
        return Result.success(result);
    }

    @PostMapping(value = "/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadFile(
            HttpSession session,
            @RequestParam(required = false) String fileId,
            @RequestParam String filePid,
            @RequestParam String fileName,
            @RequestParam String fileMd5,
            @RequestParam Integer chunkIndex,
            @RequestParam Integer chunks,
            @RequestPart MultipartFile file
    ) {
        FileUploadDto dto = new FileUploadDto();
        dto.setFileId(fileId);
        dto.setFilePid(filePid);
        dto.setFileName(fileName);
        dto.setFileMd5(fileMd5);
        dto.setChunkIndex(chunkIndex);
        dto.setChunks(chunks);
        dto.setFile(file);

        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        UploadResultVO result = fileInfoService.uploadFile(dto, sessionWebUserDto.getUserId());
        return Result.success(result);
    }

    @GetMapping("/ts/getVideoInfo")
    @GlobalInterceptor(checkParams = true)
    public void getVideoInfo(@VerifyParam(required = true) String userId,HttpServletResponse response, @VerifyParam(required = true) String fileId){
        super.getFile(response,fileId,userId);
    }

    @GetMapping("/getFile")
    @GlobalInterceptor(checkParams = true)
    public void getFile(@VerifyParam(required = true) String userId,HttpServletResponse response, @VerifyParam(required = true) String fileId){
        super.getFile(response,fileId,userId);
    }

    @PostMapping("/newFolder")
    @GlobalInterceptor(checkParams = true)
    public Result newFolder( HttpSession session,@VerifyParam @RequestBody NewFolderDto dto){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        FileInfo fileInfo = fileInfoService.newFolder(dto.getFilePid(),dto.getFolderName(),sessionWebUserDto.getUserId());
        return Result.success(fileInfo);
    }

    @GetMapping("/getAllFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public Result getFolderInfo(HttpSession session,@VerifyParam(required = true) String filePid){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        return super.getAllFolderInfo(sessionWebUserDto.getUserId(), filePid);
    }

    // 重命名
    @PutMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public Result rename(HttpSession session, @RequestBody FileRenameDto dto){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        FileInfo fileInfo = fileInfoService.renameFile(sessionWebUserDto.getUserId(),dto.getFileId(),dto.getFileName());
        return Result.success(fileInfo);
    }


    // 移动文件时回显的文件夹列表
    @GetMapping("/loadAllFolder")
    public Result loadAllFolder(HttpSession session,
                                String filePid,
                                String currentFileIds){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getUserId,sessionWebUserDto.getUserId()).eq(FileInfo::getFilePid,filePid);
        if (!StringTools.isEmpty(currentFileIds)) {
            String[] ids = currentFileIds.split(",");
            queryWrapper.notIn(FileInfo::getFileId, ids);
        }
        queryWrapper.eq(FileInfo::getDelFlag, DelFlagEnum.NORMAL.getStatus())
                .eq(FileInfo::getFileType, FileFolderTypeEnum.FOLDER)
                .orderByDesc(FileInfo::getCreateTime);
        List<FileInfo> list = fileInfoService.list(queryWrapper);
        return Result.success(list);
    }

    // 修改文件位置
    @PutMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public Result changeFileFolder(HttpSession session,@RequestBody @VerifyParam ChangeFileFolderDto dto){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        fileInfoService.changeFileFolder(dto.getFileIds(),dto.getFilePid(),sessionWebUserDto.getUserId());
        return Result.success("移动成功");
    }

    // 文件下载接口
    @GetMapping("/download")
    @GlobalInterceptor(checkParams = true)
    public void download(HttpSession session,HttpServletRequest request,HttpServletResponse response,String fileIds) throws UnsupportedEncodingException {
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        super.download(request,response,fileIds,sessionWebUserDto.getUserId());
    }

    // 删除文件接口
    @DeleteMapping("/deleteFile")
    @GlobalInterceptor(checkParams = true)
    public Result deleteFile(HttpSession session,@VerifyParam(required = true) String fileIds){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        fileInfoService.removeFile2RecycleBatch(sessionWebUserDto.getUserId(),fileIds);
        return Result.success("删除成功");
    }

}

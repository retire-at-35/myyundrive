package com.pan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.query.MPJLambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.pan.component.RedisComponent;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.mapper.UserInfoMapper;
import com.pan.pojo.dto.FileUploadDto;
import com.pan.pojo.dto.SaveShareToMyselfDto;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.pojo.dto.UserSpaceDto;
import com.pan.pojo.entity.FileInfo;
import com.pan.pojo.entity.UserInfo;
import com.pan.pojo.enums.*;
import com.pan.pojo.properties.AppProperties;
import com.pan.pojo.vo.FileInfoVO;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.UploadResultVO;
import com.pan.service.FileInfoService;
import com.pan.mapper.FileInfoMapper;
import com.pan.service.UserInfoService;
import com.pan.utils.DateUtil;
import com.pan.utils.ProcessUtil;
import com.pan.utils.ScaleFilter;
import com.pan.utils.StringTools;
import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author 31696
* @description 针对表【file_info】的数据库操作Service实现
* @createDate 2025-01-06 17:41:57
*/
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo>
    implements FileInfoService{

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private AppProperties appProperties;

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;
    @Override
    public PageBean loadDataList(String fileName,String filePid, String userId, Integer page, Integer pageSize) {
        Integer status = DelFlagEnum.NORMAL.getStatus();
        Page<FileInfo> fileInfoPage = new Page<>(page,pageSize);
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getDelFlag,status)
                .eq(FileInfo::getFilePid,filePid);
        if (!StringTools.isEmpty(fileName)){
            query.like(FileInfo::getFileName,fileName);
        }

        Page<FileInfo> pageData = fileInfoMapper.selectPage(fileInfoPage, query);
        PageBean pageBean = new PageBean();
        List<FileInfo> records = pageData.getRecords();
        List<FileInfoVO> data = new ArrayList<>();
        for (FileInfo record : records) {
            FileInfoVO fileInfoVO = new FileInfoVO();
            BeanUtils.copyProperties(record,fileInfoVO);
            data.add(fileInfoVO);
        }
        pageBean.setRows(data);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }

   //浏览分享时的文件查询
    @Override
    public PageBean loadByFileIds(Integer page, Integer pageSize, String fileIds,String userId) {
        String[] fileIdArray = fileIds.split(",");
        Integer status = DelFlagEnum.NORMAL.getStatus();
        Page<FileInfo> fileInfoPage = new Page<>(page,pageSize);
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getDelFlag,status)
                .in(FileInfo::getFileId,fileIdArray);
        Page<FileInfo> pageData = fileInfoMapper.selectPage(fileInfoPage, query);
        PageBean pageBean = new PageBean();
        List<FileInfo> records = pageData.getRecords();
        List<FileInfoVO> data = new ArrayList<>();
        for (FileInfo record : records) {
            FileInfoVO fileInfoVO = new FileInfoVO();
            BeanUtils.copyProperties(record,fileInfoVO);
            data.add(fileInfoVO);
        }
        pageBean.setRows(data);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultVO uploadFile( FileUploadDto dto,String userId) {
        UploadResultVO resultVO = new UploadResultVO();
        Boolean uploadSuccess = true;
        File tempFileFolder = null;
        try {
            MultipartFile file = dto.getFile();
            String fileId = dto.getFileId();
            String fileName = dto.getFileName();
            if (StringTools.isEmpty(fileId)){
                fileId = StringTools.getRandomString(10);
            }
            resultVO.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto spaceDto = redisComponent.getUserSpace(userId);
            // 如果在数据库内找到有一样md5值的文件,直接实现秒传
            if (dto.getChunkIndex() == 0){
                LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<FileInfo>()
                        .eq(FileInfo::getFileMd5, dto.getFileMd5())
                        .eq(FileInfo::getStatus, FileStatusEnum.USING.getStatus());
                List<FileInfo> fileInfos = fileInfoMapper.selectList(query);
                if(!fileInfos.isEmpty()){
                    FileInfo dbFile = fileInfos.get(0);
                    // 判断文件大小
                    if (dbFile.getFileSize()+spaceDto.getUserSpace()>spaceDto.getTotalSpace()){
                        throw new BussinessException("空间不足");
                    }
                    // 如果没有空间问题,复制一份就可以
                    dbFile.setFileId(fileId);
                    dbFile.setFilePid(dto.getFilePid());
                    dbFile.setUserId(userId);
                    dbFile.setCreateTime(curDate);
                    dbFile.setLastUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnum.USING.getStatus());
                    dbFile.setDelFlag(DelFlagEnum.NORMAL.getStatus());
                    dbFile.setFileMd5(dto.getFileMd5());
                    // 文件重命名
                    dbFile.setFileName(rename(dto.getFilePid(),userId,fileName));
                    fileInfoMapper.insert(dbFile);
                    resultVO.setStatus(UploadStatusEnum.UPLOAD_SECONDS.getCode());
                    // 更新空间
                    updateUserSpace(userId,dbFile.getFileSize(),null);
                    return resultVO;
                }
            }
            //判断磁盘空间是否满足
            //先获取临时文件的大小
            Long currentTemplateSize = redisComponent.getFileTempSize(userId,fileId);
            if (dto.getFile().getSize()+currentTemplateSize+spaceDto.getUserSpace()>spaceDto.getTotalSpace()){
                throw new BussinessException("空间不足");
            }
            // 暂存临时目录
            String tempFolderName = appProperties.getFolder() + Constant.FILE_TEMP_FOLDER;
            String currentUserFolderName = userId+fileId;
            tempFileFolder = new File(tempFolderName+currentUserFolderName);
            if(!tempFileFolder.exists()){
                tempFileFolder.mkdirs();
            }
            // 文件开始上传
            File newFile = new File(tempFileFolder.getPath() + "/" + dto.getChunkIndex());
            file.transferTo(newFile);
            if (dto.getChunkIndex() < dto.getChunks()-1){
                resultVO.setStatus(UploadStatusEnum.UPLOADING.getCode());
                // 保存临时大小
                redisComponent.saveFileTempSize(userId,fileId,file.getSize());
                return resultVO;
            }
            redisComponent.saveFileTempSize(userId,fileId,file.getSize());
            // 最后一片上传完成,记录数据库,异步合并分片
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
            // 把文件的后缀拿出来
            String fileSuffix = StringTools.getFileSuffix(fileName);
            // 真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnum fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            // 自动重命名
            fileName = rename(dto.getFilePid(),userId,fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilePid(dto.getFilePid());
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(userId);
            fileInfo.setFileMd5(dto.getFileMd5());
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month+"/"+realFileName);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnum.getCategoryEnum().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnum.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnum.FILE.getType());
            fileInfo.setDelFlag(DelFlagEnum.NORMAL.getStatus());
            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(userId, fileId);
            updateUserSpace(userId,totalSize,null);
            resultVO.setStatus(UploadStatusEnum.UPLOAD_FINISH.getCode());
            //  异步处理
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(),userId);
                }
            });
            return resultVO;
        } catch (Exception e) {
            logger.info("文件上传失败",e);
            uploadSuccess = false;
            throw new BussinessException("上传失败");
        } finally {
            // 干掉临时目录
            if (!uploadSuccess && tempFileFolder!=null){
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    logger.info("删除临时目录失败",e);
                }
            }
        }
    }

    // 保存分享信息的我的网盘
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShare2Myself(String userId, SaveShareToMyselfDto saveShareToMyselfDto) {
        String shareCreatedUserId = saveShareToMyselfDto.getUserId();
        UserInfo userInfo = userInfoMapper.selectById(shareCreatedUserId);
        if (null == userInfo){
            throw new BussinessException("不要乱搞哈");
        }
        String targetFileId = saveShareToMyselfDto.getFilePid();
        String[] fileIdArray = saveShareToMyselfDto.getFileIds().split(",");
        // 合法性校验,看看文件id查询出来的数据是否有效以及目的地文件夹是否存在
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,shareCreatedUserId).eq(FileInfo::getFileId,targetFileId);
        FileInfo fileInfo = fileInfoMapper.selectOne(query);
        if (null == fileInfo || fileInfo.getFolderType().equals(FileFolderTypeEnum.FILE.getType())){
            throw new BussinessException("不要乱搞哈");
        }
        query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,shareCreatedUserId).in(FileInfo::getFileId,fileIdArray);
        List<FileInfo> fileInfos = fileInfoMapper.selectList(query);
        if (fileInfos == null || fileInfos.size()!=fileIdArray.length){
            throw new BussinessException("不要乱搞哈");
        }
        // 判断空间是否充足
        // 先计算这个分享文件的空间大小
        List<String> fileIdList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : fileInfos) {
            findAllSubFolderList(fileIdList,userId,item,DelFlagEnum.NORMAL.getStatus());
        }
        Long space = fileInfoMapper.selectSpaceSizeByfileIds(userId,fileIdList);
        UserSpaceDto spaceDto = redisComponent.getUserSpace(userId);
        // 判断文件大小
        if (space+spaceDto.getUserSpace()>spaceDto.getTotalSpace()){
            throw new BussinessException("空间不足");
        }
        // 修改传过来的那几个文件的filePid,也就是最外层的文件的Pid,这几个先插入,在插入的过程中需要
        //把子文件的filePid也修改,这样文件结构才不会乱
        List<FileInfo> childFile = new ArrayList<>();
        List<FileInfo> resultFileList = new ArrayList<>();
        for (FileInfo item : fileInfos) {
            item.setFilePid(targetFileId);
            item.setUserId(userId);
            String newFileId = StringTools.getRandomString(10);
            // 如果是文件夹,需要查找到所有的子文件
            if (item.getFolderType().equals(FileFolderTypeEnum.FOLDER.getType())){
                query = new LambdaQueryWrapper<>();
                query.eq(FileInfo::getUserId,shareCreatedUserId).eq(FileInfo::getFilePid,item.getFileId());
                List<FileInfo> children = fileInfoMapper.selectList(query);
                resetChildFilePid(children,newFileId);
                childFile.addAll(children);
            }
            item.setFileId(newFileId);
            item.setCreateTime(curDate);
            item.setLastUpdateTime(curDate);
            resultFileList.add(item);
        }
        // 接下来处理那些子文件了,需要使用递归来实现
        // 递归处理文件的信息
        for (FileInfo item : childFile) {
            handleChildernFile(null,item,shareCreatedUserId,curDate,userId);
        }
        resultFileList.addAll(childFile);
        for (FileInfo item : resultFileList) {
            String rename = rename(item.getFilePid(), item.getUserId(), item.getFileName());
            item.setFileName(rename);
        }
        // 批量插入
        fileInfoMapper.insertBatch(resultFileList);
        // 修改用户空间
        updateUserSpace(userId,space,null);
    }

    @Override
    public PageBean loadDataList2(String filePid, String userId, Integer page, Integer pageSize) {
        Integer status = DelFlagEnum.NORMAL.getStatus();
        Page<FileInfo> fileInfoPage = new Page<>(page,pageSize);
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getDelFlag,status)
                .eq(FileInfo::getFilePid,filePid);
        Page<FileInfo> pageData = fileInfoMapper.selectPage(fileInfoPage, query);
        PageBean pageBean = new PageBean();
        List<FileInfo> records = pageData.getRecords();
        List<FileInfoVO> data = new ArrayList<>();
        for (FileInfo record : records) {
            FileInfoVO fileInfoVO = new FileInfoVO();
            BeanUtils.copyProperties(record,fileInfoVO);
            data.add(fileInfoVO);
        }
        pageBean.setRows(data);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }

    @Override
    public Long getFolderSpace(String folderId,Integer type) {
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getFileId,folderId);
        FileInfo fileInfo = fileInfoMapper.selectOne(query);
        if (fileInfo == null || (!fileInfo.getFolderType().equals(FileFolderTypeEnum.FOLDER.getType()))){
            throw new BussinessException("未知异常");
        }
        List<String> children = new ArrayList<>();
        query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getFilePid,folderId);
        List<FileInfo> fileInfos = fileInfoMapper.selectList(query);
        for (FileInfo item : fileInfos) {
            findAllSubFolderListWithoutUserId(children,item);
        }
        Long result = fileInfoMapper.selectFolderSize(children,type);
        return result;
    }

    @Override
    public PageBean loadAllFileInfo(Integer page, Integer pageSize,String fileName) {
        Page<FileInfo> fileInfoPage = new Page<>(page,pageSize);


        /*
        select f.* ,u.email from file_info f inner join user_info u on
    f.user_id = u.user_id and f.folder_type = 0;
         */

        // 多表,把文件的所属用户邮箱写进去
        MPJLambdaWrapper<FileInfo> query = new MPJLambdaWrapper<>();
        query.selectAll(FileInfo.class)
                .select(UserInfo::getEmail)
                .innerJoin(UserInfo.class,UserInfo::getUserId,FileInfo::getUserId);
        query.eq(FileInfo::getFolderType,FileFolderTypeEnum.FILE);
        if (!StringTools.isEmpty(fileName)){
            query.like(FileInfo::getFileName,fileName);
        }
        Page<FileInfo> pageData = fileInfoMapper.selectPage(fileInfoPage, query);
        PageBean pageBean = new PageBean();
        List<FileInfo> records = pageData.getRecords();
        List<FileInfoVO> data = new ArrayList<>();
        for (FileInfo record : records) {
            FileInfoVO fileInfoVO = new FileInfoVO();
            BeanUtils.copyProperties(record,fileInfoVO);
            data.add(fileInfoVO);
        }
        pageBean.setRows(data);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }


    // 管理员删除文件
    @Override
    public void deleteFileInfo(String userId, String fileId) {
        // 判断文件是否存在或者是一个文件夹
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,userId).eq(FileInfo::getFileId,fileId);
        FileInfo fileInfo = fileInfoMapper.selectOne(query);
        if (null == fileInfo || (fileInfo.getFolderType().equals(FileFolderTypeEnum.FOLDER))){
            throw new BussinessException("请你走界面");
        }
        // 删除文件
        fileInfoMapper.delete(query);
        // 修改用户的空间信息
        redisComponent.resetUserSpace(userId);
    }

    private void handleChildernFile(String filePid,FileInfo item,String createdShareUserId,Date time,String userId) {
        item.setUserId(userId);
        if (!StringTools.isEmpty(filePid)){
            item.setFilePid(filePid);
        }
        if (item.getFolderType().equals(FileFolderTypeEnum.FOLDER.getType())){
            // 查找自己的孩子结点
            LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
            query.eq(FileInfo::getUserId,createdShareUserId).eq(FileInfo::getFilePid,item.getFileId());
            List<FileInfo> fileInfos = fileInfoMapper.selectList(query);
            String newFileId = StringTools.getRandomString(10);
            for (FileInfo fileInfo : fileInfos) {
                handleChildernFile(newFileId,fileInfo,createdShareUserId,time,userId);
            }
            item.setFileId(newFileId);
        }
        else
        {
            item.setFileId(StringTools.getRandomString(10));
        }
        item.setCreateTime(time);
        item.setLastUpdateTime(time);

    }

    private void resetChildFilePid(List<FileInfo> dbFileInfos,String newFileId) {
        for (FileInfo item : dbFileInfos) {
            item.setFilePid(newFileId);
        }
    }

    @Override
    public FileInfo newFolder(String filePid, String folderName, String userId) {
        // 检查目录名称
        checkFileName(userId,folderName,filePid,FileFolderTypeEnum.FOLDER.getType());
        // 没问题插入
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(10));
        fileInfo.setCreateTime(curDate);
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnum.USING.getStatus());
        fileInfo.setDelFlag(DelFlagEnum.NORMAL.getStatus());
        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }


    @Override
    public FileInfo renameFile(String userId,String fileId, String fileName) {
        FileInfo fileInfo = fileInfoMapper.selectOne(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId).eq(FileInfo::getFileId, fileId));
        if (null == fileInfo){
            throw new BussinessException("文件不存在");
        }
        checkFileName(userId,fileName,fileInfo.getFilePid(),fileInfo.getFolderType());
        // 获取文件后缀
        if (FileFolderTypeEnum.FILE.getType().equals(fileInfo.getFolderType())){
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date curDate = new Date();
        FileInfo dbFileInfo = new FileInfo();
        dbFileInfo.setFileName(fileName);
        dbFileInfo.setLastUpdateTime(curDate);
        fileInfoMapper.update(dbFileInfo,new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId,fileId)
                        .eq(FileInfo::getUserId,userId));
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new BussinessException("不能将目录移动到其自身");
        }

        // 如果不是移动到根目录下,需要判断目标文件夹是否存在
        if (!"0".equals(filePid)) {
            // 查询目标文件夹
            LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getFileId, filePid)
                    .eq(FileInfo::getUserId, userId);
            FileInfo fileInfo = fileInfoMapper.selectOne(query);
            if (fileInfo == null || !DelFlagEnum.NORMAL.getStatus().equals(fileInfo.getDelFlag())
                    || fileInfo.getFolderType() != FileFolderTypeEnum.FOLDER.getType()) {
                throw new BussinessException("目标文件夹不存在或不是文件夹");
            }

            // 检查目标文件夹是否为源文件夹的子目录
            String[] fileIdArray = fileIds.split(",");
            for (String fileId : fileIdArray) {
                query = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId,fileId).eq(FileInfo::getUserId,userId);
                FileInfo sourceFile = fileInfoMapper.selectOne(query);
                if (isChildDirectory(userId, sourceFile.getFileId(), filePid)) {
                    throw new BussinessException("不能将文件夹移动到其子文件夹中");
                }
            }
        }

        // 查询需要移动的文件
        String[] fileIdArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId, userId).in(FileInfo::getFileId, fileIdArray);
        List<FileInfo> fileInfos = fileInfoMapper.selectList(query);

        // 获取目标文件夹内现有的文件列表
        query = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUserId, userId).eq(FileInfo::getFilePid, filePid);
        List<FileInfo> dbFileList = fileInfoMapper.selectList(query);
        Map<String, FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));

        // 重命名并移动文件
        for (FileInfo item : fileInfos) {
            FileInfo rootFileInfo = dbFileNameMap.get(item.getFileName());
            FileInfo updateInfo = new FileInfo();
            if (rootFileInfo != null) {
                // 重命名文件
                String fileName = StringTools.rename(item.getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);

            query = new LambdaQueryWrapper<FileInfo>()
                    .eq(FileInfo::getUserId, userId)
                    .eq(FileInfo::getFileId, item.getFileId());
            fileInfoMapper.update(updateInfo, query);
        }
    }

    /**
     * 检查目标文件夹是否为源文件夹的子文件夹或后代子文件夹
     * @param userId 用户ID
     * @param sourceFileId 源文件夹ID
     * @param targetFileId 目标文件夹ID
     * @return 如果目标文件夹是源文件夹的子文件夹或后代子文件夹，返回 true
     */
    private boolean isChildDirectory(String userId, String sourceFileId, String targetFileId) {
        // 如果目标文件夹等于源文件夹，说明无法移动（不能将文件夹移动到其自身）
        if (sourceFileId.equals(targetFileId)) {
            return true;
        }

        // 递归检查目标文件夹的父文件夹是否是源文件夹
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getFileId, targetFileId).eq(FileInfo::getUserId, userId);
        FileInfo targetFile = fileInfoMapper.selectOne(query);

        // 如果找不到父文件夹，说明目标文件夹已经是根目录
        if (targetFile == null || targetFile.getFilePid().equals("0")) {
            return false;
        }

        // 如果目标文件夹的父文件夹是源文件夹，说明目标文件夹是源文件夹的子文件夹
        if (targetFile.getFilePid().equals(sourceFileId)) {
            return true;
        }

        // 否则继续递归检查目标文件夹的父文件夹
        return isChildDirectory(userId, sourceFileId, targetFile.getFilePid());
    }


    @Override
    public void removeFile2RecycleBatch(String userId,String fileIds) {
        String[] fileArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,userId).in(FileInfo::getFileId,fileArray).eq(FileInfo::getDelFlag,DelFlagEnum.NORMAL.getStatus());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()){
            return;
        }
        List<String> delFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderList(delFileIdList,userId,fileInfo,DelFlagEnum.NORMAL.getStatus());
        }
        // 如果被删的不是空
        if (!delFileIdList.isEmpty()){
            FileInfo updateInfo = new FileInfo();
            updateInfo.setDelFlag(DelFlagEnum.RECYCLE_BIN.getStatus());
            updateInfo.setRecoverTime(new Date());
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getUserId,userId).eq(FileInfo::getDelFlag,DelFlagEnum.NORMAL.getStatus()).in(FileInfo::getFileId,delFileIdList);
            fileInfoMapper.update(updateInfo,queryWrapper);
        }
        // 用户空间更新

    }

    @Override
    public PageBean loadRecycleDataList(String fileName,String userId, Integer page, Integer pageSize) {
        Integer status = DelFlagEnum.RECYCLE_BIN.getStatus();
        Page<FileInfo> fileInfoPage = new Page<>(page,pageSize);
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getDelFlag,status)
                .orderByDesc(FileInfo::getRecoverTime);
        if (!StringTools.isEmpty(fileName)){
            query.like(FileInfo::getFileName,fileName);
        }
        Page<FileInfo> pageData = fileInfoMapper.selectPage(fileInfoPage, query);
        PageBean pageBean = new PageBean();
        List<FileInfo> records = pageData.getRecords();
        List<FileInfoVO> data = new ArrayList<>();
        for (FileInfo record : records) {
            FileInfoVO fileInfoVO = new FileInfoVO();
            BeanUtils.copyProperties(record,fileInfoVO);
            data.add(fileInfoVO);
        }
        pageBean.setRows(data);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }

    // 多选恢复
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,userId).in(FileInfo::getFileId,fileArray).eq(FileInfo::getDelFlag,DelFlagEnum.RECYCLE_BIN.getStatus());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()){
            return;
        }
        if (fileInfoList.size() != fileArray.length){
            throw new BussinessException("参数异常");
        }
        List<String> recycleFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderList(recycleFileIdList,userId,fileInfo,DelFlagEnum.RECYCLE_BIN.getStatus());
        }
       // 找到所有的需要恢复的文件
        query = new LambdaQueryWrapper<FileInfo>();
        query.in(FileInfo::getFileId,recycleFileIdList);
        List<FileInfo> needRecycleList = fileInfoMapper.selectList(query);
        // 检查名字并修改文件状态
        for (FileInfo item : needRecycleList) {
            String name = rename(item.getFilePid(), userId, item.getFileName());
            item.setFileName(name);
            item.setRecoverTime(new Date());
            item.setDelFlag(DelFlagEnum.NORMAL.getStatus());
            query = new LambdaQueryWrapper<>();
            query.eq(FileInfo::getFileId,item.getFileId()).eq(FileInfo::getUserId,item.getUserId());
            fileInfoMapper.update(item,query);
        }
    }

    //真正意义上的删除
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean isAdmin) {
        String[] fileArray = fileIds.split(",");
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getUserId,userId).in(FileInfo::getFileId,fileArray).eq(FileInfo::getDelFlag,DelFlagEnum.RECYCLE_BIN.getStatus());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()){
            return;
        }
        List<String> deleteFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderList(deleteFileIdList,userId,fileInfo,DelFlagEnum.RECYCLE_BIN.getStatus());
        }
        if (!deleteFileIdList.isEmpty()){
            LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FileInfo::getUserId,userId).eq(FileInfo::getDelFlag,DelFlagEnum.RECYCLE_BIN.getStatus()).in(FileInfo::getFileId,deleteFileIdList);
            fileInfoMapper.delete(queryWrapper);
        }
        // 更新用户空间
        Long space = fileInfoMapper.selectUserSpace(userId);
        UserInfo updateInfo = new UserInfo();
        updateInfo.setUserSpace(space);
        updateInfo.setUserId(userId);
        userInfoMapper.updateById(updateInfo);
        // 设置缓存
        UserSpaceDto userSpaceDto = redisComponent.getUserSpace(userId);
        userSpaceDto.setUserSpace(space);
        redisComponent.saveUserSpaceUse(userId,userSpaceDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTimeoutFile() {
        // 查询哪些文件需要删除
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", 1)  // del_flag = 1
                .lt("recover_time", now.minus(5, ChronoUnit.DAYS));
        List<FileInfo> list = fileInfoMapper.selectList(queryWrapper);
        // 把list中每个元素的userId给提取出来,放到一个集合,保证userId没有重复
        Set<String> uniqueUserIds = list.stream()
                .map(FileInfo::getUserId)  // 提取userId
                .collect(Collectors.toSet());
        // 删除过期文件
        fileInfoMapper.delete(queryWrapper);
        // 挨个更新用户的空间
        for (String userId : uniqueUserIds) {
            redisComponent.resetUserSpace(userId);
        }
    }




    // 递归找到所有文件
    private void findAllSubFolderList(List<String> fileIdList,String userId,FileInfo fileInfo,Integer type){
        // 先把自己加进去
        // 判断如果是文件就直接加入,不是的话再去递归
            if (fileInfo.getFolderType().equals(FileFolderTypeEnum.FILE.getType())){
                fileIdList.add(fileInfo.getFileId());
            }
            else {
                fileIdList.add(fileInfo.getFileId());
                LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
                query.eq(FileInfo::getUserId,userId).eq(FileInfo::getFilePid,fileInfo.getFileId());
                List<FileInfo> children = fileInfoMapper.selectList(query);
                for (FileInfo child : children) {
                    findAllSubFolderList(fileIdList,userId,child,type);
                }
            }
    }


    private void findAllSubFolderListWithoutUserId(List<String> fileIdList,FileInfo fileInfo){
        // 先把自己加进去
        // 判断如果是文件就直接加入,不是的话再去递归
        if (fileInfo.getFolderType().equals(FileFolderTypeEnum.FILE.getType())){
            fileIdList.add(fileInfo.getFileId());
        }
        else {
            fileIdList.add(fileInfo.getFileId());
            LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
            query.eq(FileInfo::getFilePid,fileInfo.getFileId());
            List<FileInfo> children = fileInfoMapper.selectList(query);
            for (FileInfo child : children) {
                findAllSubFolderListWithoutUserId(fileIdList,child);
            }
        }
    }







    private String rename(String filePid,String userId,String fileName){
        // 查看是否有同名文件存在
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getFilePid,filePid)
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getFileName,fileName)
                .eq(FileInfo::getDelFlag,DelFlagEnum.NORMAL.getStatus());
        Long count = fileInfoMapper.selectCount(query);
        if(count > 0){
            fileName = StringTools.rename(fileName);
        }
        return fileName;
    }

    // 更新用户空间
    private void updateUserSpace(String userId,Long userSpace,Long totalSize){
        Integer count = userInfoMapper.updateUserSpace(userId, userSpace, totalSize);
        if(count == 0){
            throw new BussinessException("空间不足");
        }
        UserSpaceDto spaceDto = redisComponent.getUserSpace(userId);
        spaceDto.setUserSpace(spaceDto.getUserSpace()+userSpace);
        redisComponent.saveUserSpaceUse(userId,spaceDto);
    }

    // 转码
    @Async
    public void transferFile(String fileId,String userId){
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnum fileTypeEnum = null;
        FileInfo fileInfo = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getFileId,fileId)
                        .eq(FileInfo::getUserId,userId));
        try{
            // 判断这个文件为空或者这个文件不是转换中
            if (fileInfo == null || !FileStatusEnum.TRANSFER.getStatus().equals(fileInfo.getStatus())){
                return;
            }
            // 找到临时目录
            String tempFolderName = appProperties.getFolder()+Constant.FILE_TEMP_FOLDER;
            String currentUserFolderName = userId + fileId;
            File fileFolder = new File(tempFolderName+currentUserFolderName);
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            // 目标目录
            String targetFolderName = appProperties.getFolder()+Constant.FILE_FOLDER;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()){
                targetFolder.mkdirs();
            }
            // 真实的文件名
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath() + "/" +realFileName;
            // 合并文件
            union(fileFolder.getPath(),targetFilePath,fileInfo.getFileName(),true);
            // 视频文件切割
            fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            if(FileTypeEnum.VIDEO == fileTypeEnum)
            {
                cutFile4Video(fileId,targetFilePath);
                // 视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constant.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath),150,new File(coverPath));
            } else if (FileTypeEnum.IMAGE == fileTypeEnum) {
                // 生成缩略图
                cover = month + "/" +realFileName.replace(".","_.");
                String coverPath = targetFolderName + "/" +cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath),150,new File(coverPath),false);
                if (!created){
                    FileUtils.copyFile(new File(targetFilePath),new File(coverPath));
                }
            }
        }
        catch (Exception e){
            logger.error("文件转码失败,文件id{},userId{}",fileId,userId);
            transferSuccess = false;transferSuccess = false;
            throw new BussinessException("文件转码失败");
        }
        finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess?FileStatusEnum.USING.getStatus() : FileStatusEnum.TRANSFER_FAIL.getStatus());
            LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
            query.eq(FileInfo::getFileId,fileId)
                            .eq(FileInfo::getUserId,userId)
                                    .eq(FileInfo::getStatus,FileStatusEnum.TRANSFER.getStatus());
            fileInfoMapper.update(updateInfo,query);

        }
    }

    // 文件合并
    private void union(String dirPath,String toFilePath,String fileName,Boolean delSource){
        File dir = new File(dirPath);
        if (!dir.exists()){
            throw new BussinessException("目录不存在");
        }
        // 读取文件
        File[] files = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;

        try {
            writeFile = new RandomAccessFile(targetFile,"rw");
            byte[] b = new byte[1024*10];
            for (int i = 0; i < files.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath+"/"+i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile,"r");
                    while((len = readFile.read(b)) != -1){
                        writeFile.write(b,0,len);
                    }
                } catch (Exception e) {
                    logger.info("合并分片异常",e);
                }
                finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.info("合并分片{}异常",fileName);
            throw new BussinessException("上传转码失败");
        }
        finally {
            if (null!=writeFile){
                try {
                    writeFile.close();
                } catch (IOException e) {
                    throw new BussinessException("转码失败");
                }
            }
            if (delSource && dir.exists()){
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    throw new BussinessException("转码异常");
                }
            }
        }
    }

    // 视频切割
    private void cutFile4Video(String fileId,String videoFilePath){
        // 创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0,videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()){
            tsFolder.mkdirs();
        }
//        String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -vcodec copy -bsf:v h264_mp4toannexb %s";
//        String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        // 将 MP4 文件转码为 TS 文件
        String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -c:v copy -bsf:v h264_mp4toannexb %s";
        // 将 TS 文件切片为 30 秒的小段
        String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%04d.ts";

        String tsPath = tsFolder + "/" + Constant.TS_NAME;
        try {
            // 生成.ts
            String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
            logger.info("执行了生成ts文件的指令{}",cmd);
            ProcessUtil.executeCommand(cmd);
            // 生成索引文件
            cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constant.M3U8_NAME,tsFolder.getPath(),fileId);
            logger.info("执行了生成索引文件的指令{}",cmd);
            ProcessUtil.executeCommand(cmd);
            // 删除index.ts
            new File(tsPath).delete();
        } catch (IOException e) {
            logger.info("视频切割失败{}",e.getMessage());
            throw new BussinessException("视频切割失败");
        }
    }

    private void checkFileName(String userId,String fileName,String filePid,Integer type){
        LambdaQueryWrapper<FileInfo> query = new LambdaQueryWrapper<>();
        query.eq(FileInfo::getFilePid,filePid)
                .eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getFileName,fileName)
                .eq(FileInfo::getDelFlag,DelFlagEnum.NORMAL.getStatus())
                .eq(FileInfo::getFolderType,type);
        Long count = fileInfoMapper.selectCount(query);
        if (count > 0){
            throw new BussinessException("文件名重复,请修改文件名");
        }
    }
}





package com.pan.service;

import com.pan.pojo.dto.FileUploadDto;
import com.pan.pojo.dto.SaveShareToMyselfDto;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.pojo.entity.FileInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.UploadResultVO;

import java.util.List;

/**
* @author 31696
* @description 针对表【file_info】的数据库操作Service
* @createDate 2025-01-06 17:41:57
*/
public interface FileInfoService extends IService<FileInfo> {


    PageBean loadDataList(String fileName,String filePid, String userId, Integer page, Integer pageSize);

    UploadResultVO uploadFile(FileUploadDto dto,String userId);

    FileInfo newFolder(String filePid, String folderName, String userId);

    FileInfo renameFile(String userId,String fileId, String fileName);

    void changeFileFolder(String fileIds, String filePid,String userId);

    void removeFile2RecycleBatch(String userId,String fileIds);

    PageBean loadRecycleDataList(String fileName,String userId, Integer page, Integer pageSize);

    void recoverFileBatch(String userId, String fileIds);

    void delFileBatch(String userId, String fileIds, Boolean isAdmin);

    void deleteTimeoutFile();

    PageBean loadByFileIds(Integer page, Integer pageSize, String fileIds,String userId);

    void saveShare2Myself(String userId, SaveShareToMyselfDto saveShareToMyselfDto);

    PageBean loadDataList2(String filePid, String userId, Integer page, Integer pageSize);

    Long getFolderSpace(String folderId,Integer type);

    PageBean loadAllFileInfo(Integer page, Integer pageSize,String fileName);

    void deleteFileInfo(String userId, String fileId);
}

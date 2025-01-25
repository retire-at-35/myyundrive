package com.pan.service;

import com.pan.pojo.dto.SaveShareToMyselfDto;
import com.pan.pojo.dto.SessionShareDto;
import com.pan.pojo.dto.CreateShareDto;
import com.pan.pojo.entity.FileShare;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.ShowShareVO;

/**
* @author 31696
* @description 针对表【file_share】的数据库操作Service
* @createDate 2025-01-15 17:27:43
*/
public interface FileShareService extends IService<FileShare> {

    PageBean loadRecycleDataList(String userId, Integer page, Integer pageSize);

    FileShare saveShare(CreateShareDto dto, String userId);

    void deleteFileShareBatch(String userId, String shareIds);

    SessionShareDto checkShareCode(String shareId, String code);

    ShowShareVO getShareInfoByCodeAndShareId(String shareId, String code);


}

package com.pan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.pan.exception.BussinessException;
import com.pan.mapper.FileInfoMapper;
import com.pan.pojo.dto.SaveShareToMyselfDto;
import com.pan.pojo.dto.SessionShareDto;
import com.pan.pojo.dto.CreateShareDto;
import com.pan.pojo.entity.FileInfo;
import com.pan.pojo.entity.FileShare;
import com.pan.pojo.enums.FileFolderTypeEnum;
import com.pan.pojo.enums.ShareValidTypeEnum;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.ShowShareVO;
import com.pan.service.FileInfoService;
import com.pan.service.FileShareService;
import com.pan.mapper.FileShareMapper;
import com.pan.utils.DateUtil;
import com.pan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
* @author 31696
* @description 针对表【file_share】的数据库操作Service实现
* @createDate 2025-01-15 17:27:43
*/
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare>
    implements FileShareService{

    @Resource
    private FileShareMapper fileShareMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Override
    public PageBean loadRecycleDataList(String userId, Integer page, Integer pageSize) {
        Page<FileShare> fileSharePage = new Page<>(page,pageSize);
        MPJLambdaWrapper<FileShare> query = new MPJLambdaWrapper<>();
        query.selectAll(FileShare.class)
                .select(FileInfo::getFileName,FileInfo::getFileCover)
                .leftJoin(FileInfo.class,FileInfo::getFileId,FileShare::getFileId)
                .eq(FileShare::getUserId,userId)
                .orderByDesc(FileShare::getShareTime);

        Page<FileShare> pageData = fileShareMapper.selectPage(fileSharePage, query);
        PageBean pageBean = new PageBean();
        List<FileShare> records = pageData.getRecords();
        pageBean.setRows(records);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }



    @Override
    public FileShare saveShare(CreateShareDto dto, String userId) {
        ShareValidTypeEnum typeEnums = ShareValidTypeEnum.getByType(dto.getValidType());
        if (typeEnums == null){
            throw new RuntimeException("未知异常");
        }
        FileShare fileShare = new FileShare();
        if (ShareValidTypeEnum.FOREVER!=typeEnums){
            fileShare.setExpireTime(DateUtil.getAfterDate(typeEnums.getDays()));
        }
        Date curDate = new Date();
        fileShare.setShareTime(curDate);
        fileShare.setUserId(userId);
        fileShare.setFileId(dto.getFileId());
        fileShare.setShowCount(0);
        if (StringTools.isEmpty(fileShare.getCode())){
            fileShare.setCode(StringTools.getRandomString(5));
        }
        fileShare.setShareId(StringTools.getRandomString(20));
        fileShareMapper.insert(fileShare);
        return fileShare;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String userId, String shareIds) {
        String[] shareIdArray = shareIds.split(",");
        LambdaQueryWrapper<FileShare> query = new LambdaQueryWrapper<>();
        query.eq(FileShare::getUserId,userId).in(FileShare::getShareId,shareIdArray);
        int count = fileShareMapper.delete(query);
        if (count != shareIdArray.length){
            throw new RuntimeException("未知异常");
        }
    }

    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShare share = fileShareMapper.selectById(shareId);
        if (null == share || !(share.getExpireTime()!=null&&new Date().after(share.getExpireTime()))){
            throw new RuntimeException("分享链接不存在或者已经失效");
        }
        if (!share.getCode().equals(code)){
            throw new RuntimeException("提取码错误");
        }
        // 更新浏览次数
        fileShareMapper.updateShareShowCount(shareId);
        SessionShareDto sessionShareDto = new SessionShareDto();
        sessionShareDto.setShareId(shareId);
        sessionShareDto.setShareUserId(share.getUserId());
        sessionShareDto.setFileId(share.getFileId());
        sessionShareDto.setExpireTime(share.getExpireTime());
        return sessionShareDto;
    }

    // 效验提取码并返回文件信息
    @Override
    public ShowShareVO getShareInfoByCodeAndShareId(String shareId, String code) {
        // 通过shareId获取分享信息
        LambdaQueryWrapper<FileShare> query = new LambdaQueryWrapper<>();
        query.eq(FileShare::getShareId,shareId);
        FileShare share = fileShareMapper.selectOne(query);
        if (share == null){
            throw new BussinessException("未知异常");
        }
        // 判断提取码是否正确
        if (!code.equals(share.getCode())){
            throw new BussinessException("提取码错误");
        }
        // 通过分享信息的文件id获取文件信息返回
        ShowShareVO showShareVO = new ShowShareVO();
        showShareVO.setFileIds(share.getFileId());
        showShareVO.setUserId(share.getUserId());
        return showShareVO;

    }


}





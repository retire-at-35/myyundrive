package com.pan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pan.annotation.GlobalInterceptor;
import com.pan.annotation.VerifyParam;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.pojo.dto.SaveShareToMyselfDto;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.pojo.dto.CreateShareDto;
import com.pan.pojo.entity.FileShare;
import com.pan.pojo.entity.UserInfo;
import com.pan.pojo.vo.FileShareVO;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.Result;
import com.pan.pojo.vo.ShowShareVO;
import com.pan.service.FileInfoService;
import com.pan.service.FileShareService;
import com.pan.service.UserInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController
@RequestMapping("/share")
public class ShareController {

    @Resource
    private FileShareService fileShareService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private FileInfoService fileInfoService;

    @GetMapping("/loadShareList")
    @GlobalInterceptor
    public Result loadShareList(HttpSession session,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer pageSize)
    {
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        PageBean result = fileShareService.loadRecycleDataList(dto.getUserId(),page,pageSize);
        return Result.success(result);
    }



    // 获取分享的省略信息
    @GetMapping("/getShareById")
    public Result getShareById(String shareId){
        LambdaQueryWrapper<FileShare> query = new LambdaQueryWrapper<>();
        query.eq(FileShare::getShareId,shareId);
        FileShare share = fileShareService.getOne(query);
        if (null == share || (share.getExpireTime()!=null&&new Date().after(share.getExpireTime()))){
            throw new BussinessException("分享链接不存在或者已经失效");
        }
        UserInfo userInfo = userInfoService.getById(share.getUserId());
        FileShareVO fileShareVO = new FileShareVO();
        BeanUtils.copyProperties(share,fileShareVO);
        fileShareVO.setAvatar(userInfo.getAvatar());
        fileShareVO.setUserId(userInfo.getUserId());
        fileShareVO.setUserName(userInfo.getNickName());
        return Result.success(fileShareVO);
    }

    // 通过提取码获得分享信息接口
    @GetMapping("/getShareInfoByCodeAndShareId")
    @GlobalInterceptor(checkParams = true)
    public Result getShareInfoByCodeAndShareId(@VerifyParam(required = true) String shareId,@VerifyParam(required = true) String code){
        ShowShareVO result = fileShareService.getShareInfoByCodeAndShareId(shareId, code);
        return Result.success(result);
    }


    // 新建分享,code就是分享码
    @PostMapping("shareFile")
    @GlobalInterceptor(checkParams = true)
    public Result shareFile(HttpSession session,
                           @VerifyParam @RequestBody CreateShareDto createShareDto){
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        FileShare share = fileShareService.saveShare(createShareDto, dto.getUserId());
        return Result.success(share);
    }


    // 将分享文件保存到用户网盘
    @PostMapping("/saveShare2Myself")
    @GlobalInterceptor(checkParams = true)
    public Result saveShare2Myself(HttpSession session, @RequestBody @VerifyParam SaveShareToMyselfDto saveShareToMyselfDto){
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        fileInfoService.saveShare2Myself(dto.getUserId(),saveShareToMyselfDto);
        return Result.success("保存成功");
    }

    // 取消分享
    @PostMapping("/cancelShare")
    public Result cancelShare(HttpSession session,String shareIds){
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        fileShareService.deleteFileShareBatch(dto.getUserId(),shareIds);
        return Result.success("操作成功");
    }
}

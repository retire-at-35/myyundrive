package com.pan.controller;

import com.pan.annotation.GlobalInterceptor;
import com.pan.annotation.VerifyParam;
import com.pan.component.RedisComponent;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.pojo.dto.SystemDto;
import com.pan.pojo.dto.UpdateUserSpaceDto;
import com.pan.pojo.dto.UpdateUserStatusDto;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.Result;
import com.pan.service.FileInfoService;
import com.pan.service.UserInfoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoService userInfoService;

    @GetMapping("/loadUserList")
    @GlobalInterceptor(checkAdmin = true)
    public Result loadUserList(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               String email,
                               String nickName){
        PageBean result = userInfoService.pageQuery(page,pageSize,email,nickName);
        return Result.success(result);
    }

    // 更新用户状态
    @PutMapping("/updateUserStatus")
    @GlobalInterceptor(checkAdmin = true)
    public Result updateUserStatus(HttpSession session,@RequestBody UpdateUserStatusDto dto){
        userInfoService.updateStatus(dto.getUserId(),dto.getStatus());
        return Result.success("操作成功");
    }

    // 扩容
    @PutMapping("/updateUserSpace")
    @GlobalInterceptor(checkAdmin = true)
    public Result updateUserSpace(@RequestBody UpdateUserSpaceDto updateUserSpaceDto){
        Long space = updateUserSpaceDto.getChangeSpace();
        if (space<=0){
            throw new BussinessException("请走界面");
        }
        space = space * Constant.MB * 1024L;
        userInfoService.updateUserSpace(updateUserSpaceDto.getUserId(),space);
        return Result.success("操作成功");
    }

    // 查询所有文件列表
    @GetMapping("/loadAllFileInfo")
    @GlobalInterceptor(checkAdmin = true)
    public Result loadAllFileInfo(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String fileName
    ){
        PageBean result = fileInfoService.loadAllFileInfo(page,pageSize,fileName);
        return Result.success(result);
    }


    // 删除文件功能
    @DeleteMapping("/deleteFile")
    @GlobalInterceptor(checkAdmin = true)
    public Result deleteFileInfo(@VerifyParam(required = true)String userId,@VerifyParam(required = true)String fileId){
        fileInfoService.deleteFileInfo(userId,fileId);
        return Result.success("删除成功");
    }



}

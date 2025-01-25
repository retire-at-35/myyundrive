package com.pan.controller;

import com.pan.annotation.GlobalInterceptor;
import com.pan.annotation.VerifyParam;
import com.pan.constant.Constant;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.pojo.entity.FileInfo;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.Result;
import com.pan.service.FileInfoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/recycle")
public class RecycleController {
    @Resource
    private FileInfoService fileInfoService;

    // 获取回收站文件列表
    @GetMapping("/loadRecycleFileList")
    @GlobalInterceptor
    public Result loadRecycleFileList(HttpSession session,
                                      String fileName,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer pageSize){
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        PageBean result = fileInfoService.loadRecycleDataList(fileName,dto.getUserId(),page,pageSize);
        return Result.success(result);
    }

    // 恢复文件接口
    @PutMapping("/recoverFile")
    @GlobalInterceptor(checkParams = true)
    public Result recoverFile(HttpSession session,
                              @VerifyParam(required = true) String fileIds){
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        fileInfoService.recoverFileBatch(dto.getUserId(),fileIds);
        return Result.success("恢复成功");
    }

    @DeleteMapping("/deleteFile")
    @GlobalInterceptor(checkParams = true)
    public Result deleteFile(HttpSession session,@VerifyParam(required = true) String fileIds){
        SessionWebUserDto dto = (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        fileInfoService.delFileBatch(dto.getUserId(),fileIds,dto.getIsAdmin());
        return Result.success("删除成功");
    }

}

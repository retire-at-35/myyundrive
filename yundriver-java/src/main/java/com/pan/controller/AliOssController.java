package com.pan.controller;

import com.pan.annotation.GlobalInterceptor;
import com.pan.constant.Constant;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.pojo.vo.Result;
import com.pan.service.UserInfoService;
import com.pan.utils.AliOssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@RequestMapping("upload")
@RestController
public class AliOssController {
    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private UserInfoService userInfoService;

    private static final Logger logger = LoggerFactory.getLogger(AliOssController.class);
    @PutMapping("updateAvatar")
    @GlobalInterceptor
    public Result updateAvatar(HttpSession session, MultipartFile file){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constant.SESSION_LOGIN_KEY);
        try {
            logger.info("文件信息{}",file.getName());
            String url = aliOssUtil.upload(file);
            userInfoService.updateAvatar(sessionWebUserDto.getUserId(),url);
            return Result.success(url);
        } catch (IOException e) {
            throw new RuntimeException("修改头像失败,请重试");
        }
    }
}

package com.pan.service;

import com.pan.pojo.dto.*;
import com.pan.pojo.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pan.pojo.vo.PageBean;

/**
* @author 31696
* @description 针对表【user_info】的数据库操作Service
* @createDate 2024-12-29 22:56:14
*/
public interface UserInfoService extends IService<UserInfo> {

    void register(RegisterDto registerDto);

    SessionWebUserDto login(LoginDto loginDto);

    void forgetPassword(ResetDto dto);

    void updatePassword(String userId,UpdatePasswordDto dto);

    PageBean pageQuery(Integer page, Integer pageSize,String email, String nickName);

    void updateStatus(String userId, Integer status);

    void updateUserSpace(String userId, Long space);

    void updateAvatar(String userId, String url);
}

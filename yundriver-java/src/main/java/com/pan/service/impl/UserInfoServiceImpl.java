package com.pan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pan.component.RedisComponent;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.mapper.FileInfoMapper;
import com.pan.pojo.dto.*;
import com.pan.pojo.entity.FileInfo;
import com.pan.pojo.entity.UserInfo;
import com.pan.pojo.enums.DelFlagEnum;
import com.pan.pojo.enums.FileFolderTypeEnum;
import com.pan.pojo.enums.UserStatusEnum;
import com.pan.pojo.properties.AdminProperties;
import com.pan.pojo.properties.MessageConfig;
import com.pan.pojo.vo.FileInfoVO;
import com.pan.pojo.vo.PageBean;
import com.pan.pojo.vo.UserInfoVO;
import com.pan.service.EmailCodeService;
import com.pan.service.FileInfoService;
import com.pan.service.UserInfoService;
import com.pan.mapper.UserInfoMapper;
import com.pan.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
* @author 31696
* @description 针对表【user_info】的数据库操作Service实现
* @createDate 2024-12-29 22:56:14
*/
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo>
    implements UserInfoService{

    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;
    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private AdminProperties adminProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDto registerDto) {
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, registerDto.getEmail());
        UserInfo userInfo = userInfoMapper.selectOne(query);
        if (null != userInfo){
            throw new BussinessException("邮箱账号已被使用");
        }
        // 验证码邮箱验证码
        emailCodeService.checkCode(registerDto.getEmail(),registerDto.getEmailCode());
        String userId = StringTools.getRandomString(8);
        userInfo = new UserInfo();
        BeanUtils.copyProperties(registerDto,userInfo);
        userInfo.setUserId(userId);
        userInfo.setPassword(registerDto.getPassword());
        userInfo.setJoinTime(new Date());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setUserSpace(0L);
        userInfo.setTotalSpace(redisComponent.getSystemDto().getInitSpace());
        userInfoMapper.insert(userInfo);
        //
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        fileInfo.setFileId("0");
        fileInfo.setFilePid("-1");
        fileInfo.setFileName("root");
        fileInfo.setUserId(userId);
        fileInfo.setCreateTime(new Date());
        fileInfo.setDelFlag(DelFlagEnum.NORMAL.getStatus());
        fileInfoMapper.insert(fileInfo);
    }

    @Override
    public SessionWebUserDto login(LoginDto loginDto) {
        String email = loginDto.getEmail();
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, email));
        if(null == userInfo || !loginDto.getPassword().equals(userInfo.getPassword())){
            throw new BussinessException("账号或密码错误");
        }
        if( userInfo.getStatus() == 0){
            throw new BussinessException("用户状态异常");
        }
        UserInfo updateUserInfo = new UserInfo();
        updateUserInfo.setLastLoginTime(new Date());
        updateUserInfo.setUserId(userInfo.getUserId());
        userInfoMapper.updateById(updateUserInfo);
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setAvatar(userInfo.getAvatar());
        sessionWebUserDto.setNickName(userInfo.getNickName());
        sessionWebUserDto.setUserId(userInfo.getUserId());
        logger.info("登录账号{}",email);
        String[] adminEmails = adminProperties.getEmail().split(",");
        for (String item : adminEmails) {
            logger.info("管理员账号{}",item);
        }
        if(ArrayUtils.contains(adminEmails,email)){
            sessionWebUserDto.setIsAdmin(true);
        }
        else {
            sessionWebUserDto.setIsAdmin(false);
        }
        Long space = fileInfoMapper.selectUserSpace(userInfo.getUserId());
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        userSpaceDto.setUserSpace(space);
        redisComponent.saveUserSpaceUse(userInfo.getUserId(),userSpaceDto);
        return sessionWebUserDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgetPassword(ResetDto dto) {
        // 查一下是否有这个账号先
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, dto.getEmail()));
        if (null == userInfo){
            throw new BussinessException("邮箱账号不存在");
        }
        emailCodeService.checkCode(dto.getEmail(), dto.getEmailCode());
        // 没有异常就修改密码
        userInfoMapper.forgetPassword(dto.getPassword(),dto.getEmail());
    }

    @Override
    public void updatePassword(String userId,UpdatePasswordDto dto) {
        // 判断密码是否正确
        UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId));
        if(null == userInfo){
            throw new BussinessException("出现异常,重试");
        }
        // todo md5问题
        if(!(dto.getOldPassword().equals(userInfo.getPassword()))){
            throw new BussinessException("原密码错误,修改失败");
        }
        UserInfo updateUserInfo = new UserInfo();
        updateUserInfo.setPassword(dto.getPassword());
        updateUserInfo.setUserId(userId);
        userInfoMapper.updateById(updateUserInfo);
    }

    @Override
    public PageBean pageQuery(Integer page, Integer pageSize,String email, String nickName) {
        Page<UserInfo> userInfoPage = new Page<>(page,pageSize);
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<UserInfo>();
        query.orderByDesc(UserInfo::getJoinTime).notIn(UserInfo::getEmail,adminProperties.getEmail().split(","));
        if (!StringTools.isEmpty(email)){
            query.like(UserInfo::getEmail,email);
        }
        if (!StringTools.isEmpty(nickName)){
            query.like(UserInfo::getNickName,nickName);
        }
        Page<UserInfo> pageData = userInfoMapper.selectPage(userInfoPage,query);
        PageBean pageBean = new PageBean();
        List<UserInfo> records = pageData.getRecords();
        List<UserInfoVO> data = new ArrayList<>();
        for (UserInfo record : records) {
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(record,userInfoVO);
            data.add(userInfoVO);
        }
        pageBean.setRows(data);
        pageBean.setTotal(pageData.getTotal());
        return pageBean;
    }


    @Override
    public void updateStatus(String userId, Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<>();
        query.eq(UserInfo::getUserId,userId);
        userInfoMapper.update(userInfo,query);

    }

    @Override
    public void updateUserSpace(String userId, Long space) {
        userInfoMapper.updateUserSpace(userId,null,space);
        redisComponent.resetUserSpace(userId);
    }

    @Override
    public void updateAvatar(String userId, String url) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setAvatar(url);
        userInfoMapper.updateById(userInfo);
    }
}





package com.pan.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pan.constant.Constant;
import com.pan.mapper.FileInfoMapper;
import com.pan.mapper.UserInfoMapper;
import com.pan.pojo.dto.DownloadFileDto;
import com.pan.pojo.dto.SystemDto;
import com.pan.pojo.dto.UserSpaceDto;
import com.pan.pojo.entity.UserInfo;
import com.pan.utils.RedisUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisComponent {
    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    public SystemDto getSystemDto(){
        SystemDto systemDto = (SystemDto) redisUtils.get(Constant.REDIS_KEY_SYSTEM_SETTING);
        if (null == systemDto){
            systemDto = new SystemDto();
            redisUtils.set(Constant.REDIS_KEY_SYSTEM_SETTING,systemDto);
        }
        return systemDto;
    }

    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constant.REDIS_KEY_USER_SPACE_USE+userId,userSpaceDto,Constant.REDIS_KEY_EXPIRES_DAY);
    }

    public UserSpaceDto getUserSpace(String userId){
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constant.REDIS_KEY_USER_SPACE_USE + userId );
        if(null == userSpaceDto){
            UserSpaceDto spaceDto = new UserSpaceDto();
            UserInfo userInfo = userInfoMapper.selectById(userId);
            spaceDto.setTotalSpace(new SystemDto().getInitSpace());
            Long space = fileInfoMapper.selectUserSpace(userId);
            spaceDto.setUserSpace(space);
            redisUtils.setex(Constant.REDIS_KEY_USER_SPACE_USE+userId,spaceDto,Constant.REDIS_KEY_EXPIRES_DAY);
        }
        return userSpaceDto;
    }

    public void saveFileTempSize(String userId,String fileId,Long fileSize){
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constant.REDIS_KEY_USER_FILE_TEMP_SIZE+userId+fileId,currentSize+fileSize,Constant.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    // 获取临时文件的大小
    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constant.REDIS_KEY_USER_FILE_TEMP_SIZE+userId+fileId);
        return currentSize;
    }

    private Long getFileSizeFromRedis(String key){
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null){
            return 0L;
        }
        if(sizeObj instanceof Integer){
            return (((Integer) sizeObj).longValue());
        }
        else if(sizeObj instanceof Long){
            return (Long)sizeObj;
        }
        return 0L;
    }

    public void saveDownCode(DownloadFileDto dto){
        redisUtils.setex(Constant.REDIS_KEY_DOWNLOAD+dto.getDownloadCode(),dto,Constant.REDIS_KEY_EXPIRES_ONE_MIN*5);
    }

    public DownloadFileDto getDownloadCode(String code) {
       DownloadFileDto dto = (DownloadFileDto)redisUtils.get(code);
       return dto;
    }

    public UserSpaceDto resetUserSpace(String userId) {
        UserSpaceDto spaceDto = new UserSpaceDto();
        Long userSpace = fileInfoMapper.selectUserSpace(userId);
        spaceDto.setUserSpace(userSpace);
        LambdaQueryWrapper<UserInfo> query = new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getUserId, userId);
        UserInfo userInfo = userInfoMapper.selectOne(query);
        spaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisUtils.setex(Constant.REDIS_KEY_USER_SPACE_USE+userId,spaceDto,Constant.REDIS_KEY_EXPIRES_DAY);
        return spaceDto;
    }
}

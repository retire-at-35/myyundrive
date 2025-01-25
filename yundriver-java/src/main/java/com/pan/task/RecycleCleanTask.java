package com.pan.task;

import com.pan.service.FileInfoService;
import com.pan.service.impl.FileInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class RecycleCleanTask {
    private static final Logger logger = LoggerFactory.getLogger(RecycleCleanTask.class);

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 定期清理回收站
     */
    @Scheduled(cron = "0 0 0 * * ?") //每天晚上12点触发一次
    public void deleteTimeoutFile(){
        logger.info("{}清理了回收站过期文件");
        fileInfoService.deleteTimeoutFile();
    }



}
package com.pan.constant;

public class Constant {
    public static final String SESSION_LOGIN_KEY = "session_login_key";
    public static final String REDIS_KEY_SYSTEM_SETTING = "pan:systemsetting:";
    public static final String REDIS_KEY_USER_SPACE_USE = "pan:user:spaceuse:";
    public static final Long MB = 1024*1024L;
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;
    public static final String FILE_TEMP_FOLDER = "/temp/";
    // 临时文件的大小
    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "pan:user:file:temp:";
    public static final Integer REDIS_KEY_EXPIRES_ONE_HOUR = REDIS_KEY_EXPIRES_ONE_MIN * 60;
    public static final String FILE_FOLDER = "/file/";
    public static final String TS_NAME = "index.ts";
    public static final String M3U8_NAME = "index.m3u8";
    public static final String IMAGE_PNG_SUFFIX = ".png";
    public static final String REDIS_KEY_DOWNLOAD = "pan:download:";
    public static String CHECK_CODE = "check_code";
    public static String CHECK_CODE_EMAIL = "check_code_email";


    public static String SESSION_SHARE_KEY = "session_share_key";
}

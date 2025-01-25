package com.pan.pojo.enums;

public enum RegexValidation {

    NO("","空"),
    /**
     * 账号校验
     * 规则：只能包含字母、数字，下划线，长度6-20位
     */
    ACCOUNT("^[a-zA-Z0-9_]{6,20}$", "账号只能包含字母、数字、下划线，长度为6到20位"),

    /**
     * 邮箱校验
     * 规则：支持常见邮箱格式
     */
    EMAIL("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$", "邮箱格式不正确，请输入有效的邮箱地址"),

    /**
     * 手机号码校验
     * 规则：中国大陆手机号码，支持11位，以1开头
     */
    PHONE("^(1[3-9])\\d{9}$", "手机号码格式不正确，需为11位数字且以1开头"),

    /**
     * 密码校验
     * 规则：必须包含字母和数字，长度8-20位，允许特殊字符
     */
    PASSWORD("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{8,20}$", "密码必须包含字母和数字，长度为8到20位，允许特殊字符"),

    /**
     * IP地址校验
     * 规则：IPv4地址格式，例如：192.168.1.1
     */
    IP_ADDRESS("^((25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.|$)){4}$", "IP地址格式不正确，需为有效的IPv4地址"),

    /**
     * URL校验
     * 规则：支持http、https开头的URL
     */
    URL("^(https?:\\/\\/)[a-zA-Z0-9\\-\\.]+(:\\d+)?(\\/.*)?$", "URL格式不正确，需以http或https开头");

    private final String pattern;
    private final String description;

    /**
     * 构造函数
     * 
     * @param pattern     正则表达式
     * @param description 描述信息
     */
    RegexValidation(String pattern, String description) {
        this.pattern = pattern;
        this.description = description;
    }

    /**
     * 获取正则表达式
     * 
     * @return 正则表达式
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * 获取描述信息
     * 
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 校验输入是否符合规则
     * 
     * @param input 要校验的字符串
     * @return 是否匹配
     */
    public boolean validate(String input) {
        return input != null && input.matches(this.pattern);
    }
}

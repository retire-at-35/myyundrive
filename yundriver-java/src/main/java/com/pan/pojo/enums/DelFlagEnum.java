package com.pan.pojo.enums;

public enum DelFlagEnum {
    DELETED(0, "删除"),
    RECYCLE_BIN(1, "回收站"),
    NORMAL(2, "正常");

    private final Integer status;  // 状态值
    private final String description;  // 描述

    // 构造方法
    DelFlagEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    // 获取状态值
    public Integer getStatus() {
        return status;
    }

    // 获取描述
    public String getDescription() {
        return description;
    }

    /**
     * 根据状态值获取对应的枚举
     * @param status 状态值
     * @return 对应的枚举值，如果未找到返回 null
     */
    public static DelFlagEnum getByStatus(Integer status) {
        for (DelFlagEnum value : DelFlagEnum.values()) {
            if (value.getStatus().equals(status)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据状态值获取描述
     * @param status 状态值
     * @return 对应的描述，如果未找到返回 "未知状态"
     */
    public static String getDescriptionByStatus(Integer status) {
        DelFlagEnum flagEnum = getByStatus(status);
        return flagEnum != null ? flagEnum.getDescription() : "未知状态";
    }
}

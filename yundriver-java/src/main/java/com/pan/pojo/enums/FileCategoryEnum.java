package com.pan.pojo.enums;

public enum FileCategoryEnum {
    VIDEO(1, "视频", "video"),
    AUDIO(2, "音频", "audio"),
    IMAGE(3, "图片", "image"),
    DOCUMENT(4, "文档", "document"),
    OTHER(5, "其他", "other");

    private final Integer category; // 分类值
    private final String description; // 对应描述（中文）
    private final String code; // 描述对应的英文值

    // 构造方法
    FileCategoryEnum(Integer category, String description, String code) {
        this.category = category;
        this.description = description;
        this.code = code;
    }

    public static FileCategoryEnum getByCode(String code) {
        for (FileCategoryEnum value : FileCategoryEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    // 获取分类值
    public Integer getCategory() {
        return category;
    }

    // 获取描述
    public String getDescription() {
        return description;
    }

    // 获取英文描述
    public String getCode() {
        return code;
    }

    /**
     * 根据分类值获取对应的枚举
     * @param category 分类值
     * @return 对应的枚举值，如果未找到返回 null
     */
    public static FileCategoryEnum getByCategory(Integer category) {
        for (FileCategoryEnum value : FileCategoryEnum.values()) {
            if (value.getCategory().equals(category)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据分类值获取描述
     * @param category 分类值
     * @return 对应的描述，如果未找到返回 "未知分类"
     */
    public static String getDescriptionByCategory(Integer category) {
        FileCategoryEnum categoryEnum = getByCategory(category);
        return categoryEnum != null ? categoryEnum.getDescription() : "未知分类";
    }

    /**
     * 根据分类值获取英文描述（code）
     * @param category 分类值
     * @return 对应的英文描述，如果未找到返回 "unknown"
     */
    public static String getCodeByCategory(Integer category) {
        FileCategoryEnum categoryEnum = getByCategory(category);
        return categoryEnum != null ? categoryEnum.getCode() : "unknown";
    }
}

package com.pan.pojo.enums;

import org.apache.commons.lang3.ArrayUtils;

public enum FileTypeEnum {
    VIDEO(FileCategoryEnum.VIDEO, 1, new String[]{".mp4", ".avi", ".rmvb", ".mkv", ".mov"}, "视频"),
    AUDIO(FileCategoryEnum.AUDIO, 2, new String[]{".mp3", ".wav", ".aac", ".flac", ".ogg"}, "音频"),
    IMAGE(FileCategoryEnum.IMAGE, 3, new String[]{".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg"}, "图片"),
    PDF(FileCategoryEnum.DOCUMENT, 4, new String[]{".pdf"}, "PDF"),
    DOC(FileCategoryEnum.DOCUMENT, 5, new String[]{".doc", ".docx"}, "Word文档"),
    EXCEL(FileCategoryEnum.DOCUMENT, 6, new String[]{".xls", ".xlsx"}, "Excel表格"),
    TXT(FileCategoryEnum.DOCUMENT, 7, new String[]{".txt"}, "文本文件"),
    CODE(FileCategoryEnum.OTHER, 8, new String[]{".java", ".py", ".c", ".cpp", ".js", ".html", ".css"}, "代码文件"),
    ZIP(FileCategoryEnum.OTHER, 9, new String[]{".zip", ".rar", ".7z", ".tar", ".gz"}, "压缩文件"),
    OTHER(FileCategoryEnum.OTHER, 10, new String[]{}, "其他");

    private FileCategoryEnum categoryEnum;
    private Integer type;
    private String[] suffixs;
    private String desc;

  FileTypeEnum(FileCategoryEnum categoryEnum, Integer type, String[] suffixs, String desc) {
        this.categoryEnum = categoryEnum;
        this.type = type;
        this.suffixs = suffixs;
        this.desc = desc;
  }

    public String[] getSuffixs() {
        return suffixs;
    }

    public FileCategoryEnum getCategoryEnum() {
        return categoryEnum;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static FileTypeEnum getFileTypeBySuffix(String suffix){
      FileTypeEnum[] values = FileTypeEnum.values();
      for (FileTypeEnum item : values) {
          if(ArrayUtils.contains(item.getSuffixs(),suffix)){
              return item;
          }
      }
      return OTHER;
  }

}

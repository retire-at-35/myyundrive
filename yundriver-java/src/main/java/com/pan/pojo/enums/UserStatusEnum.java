package com.pan.pojo.enums;



public enum UserStatusEnum {
    ENABLE(1,"启动"),
    DISABLE(0,"禁用");

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private Integer status;
    private String desc;

    UserStatusEnum(int status, String desc) {
        this.status = status;
        this.desc = desc;
    }


}

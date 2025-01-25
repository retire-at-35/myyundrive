package com.pan.annotation;

import com.pan.pojo.enums.RequestFrequencyTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalInterceptor {
    boolean checkParams() default false;
    boolean checkLogin() default true;
    boolean checkAdmin() default false;
    int requestFrequencyThreshold() default 0;
    RequestFrequencyTypeEnum frequencyType() default RequestFrequencyTypeEnum.NO_LIMIT;
}

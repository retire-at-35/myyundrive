package com.pan.aspect;
import com.pan.annotation.GlobalInterceptor;
import com.pan.annotation.VerifyParam;
import com.pan.constant.Constant;
import com.pan.exception.BussinessException;
import com.pan.pojo.dto.SessionWebUserDto;
import com.pan.utils.RedisUtils;
import com.pan.utils.StringTools;
import com.pan.utils.VerifyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
@Component
@Aspect
public class OperationAspect {
    private final Logger logger = LoggerFactory.getLogger(OperationAspect.class);
    private static final String[] BASE_TYPE_ARRAY = new String[]{"java.lang.String", "java.lang.Integer", "java.lang.Long"};
    @Pointcut("@annotation(com.pan.annotation.GlobalInterceptor)")
    private void requestInterceptor(){

    }

    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint joinPoint){
        Object[] args = joinPoint.getArgs(); // 获取参数列表
        Method method = ((MethodSignature)joinPoint.getSignature()).getMethod();// 获取方法
        GlobalInterceptor globalInterceptor = method.getAnnotation(GlobalInterceptor.class);// 获取这个注解
        // 如果没有就跳过就行了
        if(null == globalInterceptor){
            return;
        }
        if(globalInterceptor.checkLogin()||globalInterceptor.checkAdmin()){
            checkLogin(globalInterceptor.checkAdmin());
        }
        if (globalInterceptor.checkParams()){
            checkParams(method,args);
        }


    }


    private void checkLogin(boolean isAdmin){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        SessionWebUserDto dto= (SessionWebUserDto)session.getAttribute(Constant.SESSION_LOGIN_KEY);
        if(null == dto){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            throw new BussinessException("登录超时");
        }
        if(!dto.getIsAdmin()&&isAdmin){
            throw new BussinessException("权限不足");
        }
    }
    private void checkParams(Method method, Object[] args){
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if(verifyParam == null){
                continue;
            }
            String paramTypeName = parameter.getParameterizedType().getTypeName();
            if(ArrayUtils.contains(BASE_TYPE_ARRAY,paramTypeName)){
                checkValue(arg,verifyParam);
            }
            else{
                checkObjValue(parameter,arg);
            }
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }

        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BussinessException("参数异常");
        }
    }

    private void checkValue(Object value, VerifyParam verifyParam) {
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();
        /**
         * 校验空
         */
        if (isEmpty && verifyParam.required()) {
            throw new BussinessException("参数异常");
        }
        /**
         * 校验长度
         */
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BussinessException("参数异常");
        }

        /**
         * 校验正则
         */
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BussinessException("参数异常");
        }
    }
}

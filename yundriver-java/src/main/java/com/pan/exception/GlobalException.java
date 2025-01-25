package com.pan.exception;


import com.pan.pojo.vo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice//用来做全局异常处理器的
public class GlobalException {


    @ExceptionHandler(BussinessException.class)
    public Result ex(BussinessException e){
        e.printStackTrace();
        return Result.error(e.getMessage());
    }

    @ExceptionHandler({Exception.class,Throwable.class})
    public Result ex(Exception e)
    {
        e.printStackTrace();
        return Result.error("未知异常,请及时联系管理员");
    }

}

package com.example.demo.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice  //会在所有controller中进行异常处理
public class GlobalExceptionHandler {
    //处理所有的异常
    @ExceptionHandler(Exception.class)
    public ResultVO handleException(Exception e){
        return new ResultVO(500,"操作失败",e.getMessage());
    }
    //处理参数错误的异常
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVO handleIllegalArgumentException(IllegalArgumentException e){
        return new ResultVO(400,"参数错误",e.getMessage());
    }
}

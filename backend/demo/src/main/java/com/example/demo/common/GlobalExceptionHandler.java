package com.example.demo.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，会在所有controller中进行异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：直接把友好提示返回给前端
    @ExceptionHandler(BusinessException.class)
    public ResultVO handleBusinessException(BusinessException e) {
        return new ResultVO(e.getCode(), e.getMessage());
    }

    // 处理所有的异常
    @ExceptionHandler(Exception.class)
    public ResultVO handleException(Exception e) {
        log.error("系统异常", e);
        return new ResultVO(500, "操作失败", e.getMessage());
    }

    // 处理参数错误的异常
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultVO handleIllegalArgumentException(IllegalArgumentException e) {
        return new ResultVO(400, "参数错误", e.getMessage());
    }
}

package com.example.demo.common;

import java.io.Serializable;

/**
 * 统一响应结果
 * 能够灵活的返回结果，包括成功、失败、异常等
 */
@lombok.AllArgsConstructor
@lombok.Data
@lombok.NoArgsConstructor
public class ResultVO<E> implements Serializable {

    private Integer code;   // 状态码 200成功 400参数错误 500服务器错误
    private String message; // 状态信息
    private E data;         // 数据

    public ResultVO(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> ResultVO<T> ok(T data) {
        return new ResultVO<>(200, "操作成功", data);
    }

    public static <T> ResultVO<T> ok(String message, T data) {
        return new ResultVO<>(200, message, data);
    }

    public static <T> ResultVO<T> fail(int code, String message) {
        return new ResultVO<>(code, message);
    }
}

package com.example.demo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class ResultVO<E> implements Serializable {
    //能够灵活的返回结果，包括成功、失败、异常等
    private Integer code;  //状态码
    private String message; //状态信息
    private E data; //数据

    public ResultVO(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}

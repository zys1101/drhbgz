package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 公众监督员
 * 任何一个具有中国公民资格的人员都可通过NEPS端注册成为公众监督员，手机号作为身份唯一识别
 */
@Data
@TableName("supervisor")
public class Supervisor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号（身份唯一识别） */
    @TableId(value = "tel_id", type = IdType.INPUT)
    private String telId;

    /** 登录密码（sha256加盐） */
    private String password;

    /** 真实姓名（便于联系） */
    private String realName;

    /** 年龄 */
    private Integer age;

    /** 性别 */
    private String gender;

    /** 绑定省编号 */
    private Integer provinceId;

    /** 绑定市编号 */
    private Integer cityId;

    /** 观测具体地址 */
    private String address;

    /** 注册日期 */
    private String registerDate;

    /** 注册时间 */
    private String registerTime;

    @TableField(exist = false)
    private String provinceName;

    @TableField(exist = false)
    private String cityName;
}

package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 网格员实测AQI数据（与反馈信息一对一）
 * AQI = MAX（SO2AQI，COAQI，PM2.5AQI）
 */
@Data
@TableName("aqi_data")
public class AqiData implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATE_WAIT_CONFIRM = 0; // 待确认
    public static final int STATE_CONFIRMED = 1;    // 已确认（纳入统计）
    public static final int STATE_REJECTED = 2;     // 已退回

    /** 实测数据编号 */
    @TableId(value = "data_id", type = IdType.AUTO)
    private Integer dataId;

    /** 对应反馈信息编号（一对一） */
    private Integer afId;

    /** SO2二氧化硫AQI浓度等级(1-6) */
    private Integer so2Grade;

    /** CO一氧化碳AQI浓度等级(1-6) */
    private Integer coGrade;

    /** PM2.5悬浮颗粒物AQI浓度等级(1-6) */
    private Integer pm25Grade;

    /** AQI等级 = MAX(SO2,CO,PM2.5) */
    private Integer aqiGrade;

    /** 检测网格员编号 */
    private Integer empId;

    /** 检测网格员登录编码（冗余） */
    private String gridCode;

    /** 提交日期 */
    private String submitDate;

    /** 提交时间 */
    private String submitTime;

    /** 状态: 0待确认 1已确认 2已退回 */
    private Integer state;

    // ---------------- 关联展示字段 ----------------
    @TableField(exist = false)
    private Integer provinceId;
    @TableField(exist = false)
    private Integer cityId;
    @TableField(exist = false)
    private String provinceName;
    @TableField(exist = false)
    private String cityName;
    @TableField(exist = false)
    private String address;
    @TableField(exist = false)
    private String telId;
    @TableField(exist = false)
    private Integer estimatedGrade;
    @TableField(exist = false)
    private String information;
}

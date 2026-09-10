package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 *
 * </p>
 *
 * @author laohan
 * @since 2026-09-03
 */
@Getter
@Setter
@TableName("aqi_feedback")
@ToString
public class AqiFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 空气质量公众监督反馈信息编号
     */
    @TableId(value = "af_id", type = IdType.AUTO)
    private Integer afId;

    /**
     * 所属公众监督员编号(即手机号码)
     */
    private String telId;

    /**
     * 反馈信息所在省区域编号
     */
    private Integer provinceId;

    /**
     * 反馈信息所在市区域编号
     */
    private Integer cityId;

    /**
     * 反馈信息所在区域详细地址
     */
    private String address;

    /**
     * 反馈信息描述
     */
    private String information;

    /**
     * 反馈者对空气质量指数级别的预估等级
     */
    private Integer estimatedGrade;

    /**
     * 反馈日期
     */
    private String afDate;

    /**
     * 反馈时间
     */
    private String afTime;

    /**
     * 指派网格员编号
     */
    private Integer gmId;

    /**
     * 指派日期
     */
    private String assignDate;

    /**
     * 指派时间
     */
    private String assignTime;

    /**
     * 信息状态 0:未指派1:已指派 2:已确认
     */
    /**
     * 状态: 0待指派 1已指派 2已提交实测待确认 3已确认(已完成)
     */
    public static final int STATE_UNASSIGNED = 0;
    public static final int STATE_ASSIGNED = 1;
    public static final int STATE_MEASURED = 2;
    public static final int STATE_CONFIRMED = 3;

    private Integer state;

    /**
     * 备注
     */
    private String remarks;

    // ---------------- 关联展示字段 ----------------
    @TableField(exist = false)
    private String provinceName;
    @TableField(exist = false)
    private String cityName;
    /** 指派网格员姓名 */
    @TableField(exist = false)
    private String gridName;
    /** 指派网格员登录编码 */
    @TableField(exist = false)
    private String gridCode;
}

package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 网格员增员请求。
 *
 * 业务规则：管理员指派时若该反馈所在网格区域**没有可工作的本地网格员**，
 * 不允许异地指派，而是生成一条增员请求，供管理员跟进、供决策者判断是否需要增员。
 */
@Data
@TableName("grid_demand")
public class GridDemand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待处理 */
    public static final int STATE_PENDING = 0;
    /** 已处理（已增员/已指派） */
    public static final int STATE_HANDLED = 1;
    /** 已忽略 */
    public static final int STATE_IGNORED = 2;

    /** 增员请求编号 */
    @TableId(value = "demand_id", type = IdType.AUTO)
    private Integer demandId;

    /** 缺员省编号 */
    private Integer provinceId;

    /** 缺员市编号 */
    private Integer cityId;

    /** 来源反馈编号（可空） */
    private Integer afId;

    /** 缺员说明 */
    private String reason;

    /** 状态: 0待处理 1已处理 2已忽略 */
    private Integer state;

    /** 申请日期 */
    private String applyDate;

    /** 申请时间 */
    private String applyTime;

    /** 处理日期 */
    private String handleDate;

    /** 处理时间 */
    private String handleTime;

    /** 处理说明 */
    private String handleRemark;

    // ---------------- 关联展示字段 ----------------
    /** 省名称 */
    @TableField(exist = false)
    private String provinceName;

    /** 市名称 */
    @TableField(exist = false)
    private String cityName;
}

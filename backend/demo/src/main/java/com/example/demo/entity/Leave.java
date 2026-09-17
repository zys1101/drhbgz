package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 请假表（人员管理：网格员请假申请与管理员审批）
 *
 * 注意：leave 是 MySQL 保留字，表名必须加反引号，否则 MyBatis-Plus 生成的
 * SQL 会变成 `... FROM leave WHERE ...` 而报语法错误（1149/1064），
 * 表现为“网格员管理”页提示“操作失败”。
 */
@Data
@TableName("`leave`")
public class Leave implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请假状态：待审批 */
    public static final int STATE_PENDING = 0;
    /** 请假状态：已同意（请假中） */
    public static final int STATE_APPROVED = 1;
    /** 请假状态：已驳回 */
    public static final int STATE_REJECTED = 2;
    /** 请假状态：已销假 */
    public static final int STATE_BACK = 3;

    /** 请假编号 */
    @TableId(value = "leave_id", type = IdType.AUTO)
    private Integer leaveId;

    /** 请假网格员编号(employee.emp_id) */
    private Integer empId;

    /** 请假事由 */
    private String reason;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;

    /** 状态: 0待审批 1已同意(请假中) 2已驳回 3已销假 */
    private Integer state;

    /** 申请日期 */
    private String applyDate;

    /** 申请时间 */
    private String applyTime;

    /** 审批/销假日期 */
    private String approveDate;

    /** 审批/销假时间 */
    private String approveTime;

    // ---------------- 关联展示字段 ----------------
    /** 请假网格员登录编码 */
    @TableField(exist = false)
    private String empCode;

    /** 请假网格员姓名 */
    @TableField(exist = false)
    private String gridName;
}

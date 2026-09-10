package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 员工表（AQI检测网格员/系统管理员/决策者）
 * 网格员与管理员属于公司员工，不能自己注册，由“东软HR系统”统一管理并同步至本系统
 */
@Data
@TableName("employee")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ROLE_GRID = "grid";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_VIEWER = "viewer";

    /** 员工编号 */
    @TableId(value = "emp_id", type = IdType.AUTO)
    private Integer empId;

    /** 登录编码 */
    private String empCode;

    /** 登录密码（sha256加盐） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色: grid网格员 / admin管理员 / viewer决策者 */
    private String role;

    /** 负责省编号 */
    private Integer provinceId;

    /** 负责市编号 */
    private Integer cityId;

    /** 是否工作状态（由东软HR系统管理）: 0否 1是 */
    private Integer working;

    @TableField(exist = false)
    private String provinceName;

    @TableField(exist = false)
    private String cityName;
}

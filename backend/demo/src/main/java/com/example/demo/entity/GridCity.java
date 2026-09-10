package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 城市行政区（网格-市，最小网格单位=大城市）
 */
@Data
@TableName("grid_city")
public class GridCity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 市区域编号 */
    @TableId(value = "city_id", type = IdType.AUTO)
    private Integer cityId;

    /** 市名称 */
    private String cityName;

    /** 所属省编号 */
    private Integer provinceId;

    /** 城市规模档次（超大城市/特大城市/Ⅰ型大城市/Ⅱ型大城市） */
    private String cityTier;

    /** 是否大城市（2022年106个大城市名单） */
    private Integer isBigCity;

    @TableField(exist = false)
    private String provinceName;
}

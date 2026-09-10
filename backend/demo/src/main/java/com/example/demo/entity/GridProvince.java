package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 省级行政区（网格-省）
 */
@Data
@TableName("grid_province")
public class GridProvince implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 省区域编号 */
    @TableId(value = "province_id", type = IdType.AUTO)
    private Integer provinceId;

    /** 省名称 */
    private String provinceName;
}

package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 空气质量指数（AQI）范围及相应类别表（附录 1.3）
 */
@Data
@TableName("aqi")
public class AqiLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** AQI级别编号(1-6) */
    @TableId(value = "aqi_id", type = IdType.AUTO)
    private Integer aqiId;

    /** 级别（一级~六级） */
    private String chineseExplain;

    /** 类别（优/良/轻度污染...） */
    private String aqiExplain;

    /** AQI指数范围 */
    private String aqiRange;

    /** 类别颜色 */
    private String color;

    /** SO2浓度下限(μg/m³) */
    private Integer so2Min;

    /** SO2浓度上限(μg/m³) */
    private Integer so2Max;

    /** CO浓度下限(mg/m³) */
    private Integer coMin;

    /** CO浓度上限(mg/m³) */
    private Integer coMax;

    /** PM2.5浓度下限(μg/m³) */
    private Integer spmMin;

    /** PM2.5浓度上限(μg/m³) */
    private Integer spmMax;

    /** 对健康影响 */
    private String healthImpact;

    /** 建议采取措施 */
    private String takeSteps;
}

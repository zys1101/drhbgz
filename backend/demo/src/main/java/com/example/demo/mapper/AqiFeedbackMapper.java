package com.example.demo.mapper;

import com.example.demo.entity.AqiFeedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author laohan
 * @since 2026-09-03
 */
@Mapper
public interface AqiFeedbackMapper extends BaseMapper<AqiFeedback> {
    //制作增加不做修改
    //默认只有传统的基础的CRUD的方法
    //如果我想要特殊的方法，需要你在这里自己写

    @Select("SELECT aqi.*,p.province_name,c.city_name FROM `aqi_feedback` aqi\n" +
            "  ,grid_province p ,grid_city c\n" +
            "  where aqi.province_id = p.province_id  \n" +
            "  and aqi.city_id = c.city_id")
    public List<AqiFeedback> findAll();
}

package com.example.demo;

import com.example.demo.entity.AqiFeedback;
import com.example.demo.service.IAqiFeedbackService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @Resource
    private IAqiFeedbackService aqiFeedbackService;

    @Test
    void testselect(){
        System.out.println("测试");
        List<AqiFeedback> list = aqiFeedbackService.list();
        for (AqiFeedback aqiFeedback : list) {
            System.out.println(aqiFeedback);
        }
    }
    @Test
    void testadd(){
        System.out.println("测试增加");
        AqiFeedback aqiFeedback = new AqiFeedback();
        aqiFeedback.setTelId("13800000000");
        aqiFeedback.setProvinceId(1);
        aqiFeedback.setCityId(1);
        aqiFeedback.setAddress("河北理工");
        aqiFeedback.setInformation("今天天气白天转多云");
        aqiFeedback.setEstimatedGrade(1);
        aqiFeedback.setAfDate("2026-09-03");
        aqiFeedback.setAfTime("10:00:00");
        aqiFeedbackService.save(aqiFeedback);
    }

    @Test
    void testfindById(){
        System.out.println("测试查询");
        AqiFeedback feedback = aqiFeedbackService.getById(1);
        System.out.println(feedback);
    }

    @Test
    void testdelete(){
        System.out.println("测试删除");
        aqiFeedbackService.removeById(1);
    }

    @Test
    void testUpdate(){
        System.out.println("测试修改");
        AqiFeedback feedback = aqiFeedbackService.getById(8);
        feedback.setAddress("河北理工真好");
        aqiFeedbackService.updateById( feedback);
    }
}

package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiFeedback;
import com.example.demo.entity.Userinfo;
import com.example.demo.service.IAqiFeedbackService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author laohan
 * @since 2026-09-03
 */
@RestController
@RequestMapping("/aqiFeedback")
@AllArgsConstructor
@Slf4j
public class AqiFeedbackController {

    private  final IAqiFeedbackService aqiFeedbackService;
    /**
     * 查询所有反馈
     * @return
     */
    @GetMapping("/select")
//    @Cacheable("aqifeedback")  //仅仅对当前这个方法的返回结果加了缓存 不代表其他方法
    @CrossOrigin
    public ResultVO select(){
        List list = aqiFeedbackService.findAll() ;
        log.info("查询所有反馈信息,级别为info：{}",list);
        return new ResultVO(200,"查询成功",list);
    }
    /**
     * 根据id查询反馈信息
     * @param afId
     * @return
     */
    @GetMapping("/find/{afId}")  //RestFul 风格  、/find/1
//    @Cacheable(value="aqifeedback",key = "#afId")
    @CrossOrigin
    public ResultVO findById(@PathVariable Integer afId ){
        AqiFeedback feedback = aqiFeedbackService.getById(afId);
        log.debug("查询反馈信息,级别为debug：{}",feedback);
        return new ResultVO(200,"查询成功",feedback);
    }

    @GetMapping("/delete/{afId}")  //RestFul 风格  、/find/1
    @CrossOrigin
    public ResultVO delete(@PathVariable Integer afId ){
        boolean success = aqiFeedbackService.removeById(afId);
        return new ResultVO(200,"删除成功",success);
    }

    @PostMapping("/save")
    @ResponseBody
    @CrossOrigin
    public ResultVO save(@RequestBody AqiFeedback feedback){
        boolean success =   aqiFeedbackService.save( feedback);
        return new ResultVO(200,"保存成功",success);
    }

    @PostMapping("/update")
    @ResponseBody
    @CrossOrigin
    public ResultVO update(@RequestBody AqiFeedback feedback){
        boolean success = aqiFeedbackService.updateById( feedback);
        return new ResultVO(200,"更新成功",success);
    }
}

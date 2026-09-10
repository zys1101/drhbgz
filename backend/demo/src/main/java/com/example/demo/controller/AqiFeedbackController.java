package com.example.demo.controller;

import com.example.demo.common.BusinessException;
import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiFeedback;
import com.example.demo.entity.Supervisor;
import com.example.demo.service.IAqiFeedbackService;
import com.example.demo.service.ISupervisorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 公众监督反馈信息接口
 * NEPS提交/历史反馈 + NEPM公众监督数据管理
 */
@Tag(name = "公众监督反馈")
@RestController
@RequestMapping("/aqiFeedback")
@AllArgsConstructor
@Slf4j
public class AqiFeedbackController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final IAqiFeedbackService aqiFeedbackService;
    private final ISupervisorService supervisorService;

    /**
     * 查询所有反馈（NEPM公众监督数据列表，带省/市名称，按时间倒序）
     */
    @GetMapping("/select")
    @Operation(summary = "查询所有反馈")
    public ResultVO select() {
        List<AqiFeedback> list = aqiFeedbackService.findAll();
        return new ResultVO(200, "查询成功", list);
    }

    /**
     * 条件查询（NEPM列表条件查询 / NEPS历史反馈：传 telId 即可）
     * 参数：telId、provinceId、cityId、grade、state、dateFrom、dateTo、keyword
     */
    @GetMapping("/query")
    @Operation(summary = "条件查询反馈")
    public ResultVO query(@RequestParam(required = false) String telId,
                          @RequestParam(required = false) Integer provinceId,
                          @RequestParam(required = false) Integer cityId,
                          @RequestParam(required = false) Integer grade,
                          @RequestParam(required = false) Integer state,
                          @RequestParam(required = false) String dateFrom,
                          @RequestParam(required = false) String dateTo,
                          @RequestParam(required = false) String keyword) {
        List<AqiFeedback> list = aqiFeedbackService.findByCond(
                telId, provinceId, cityId, grade, state, dateFrom, dateTo, keyword);
        return new ResultVO(200, "查询成功", list);
    }

    /**
     * 根据id查询反馈信息
     */
    @GetMapping("/find/{afId}")
    @Operation(summary = "反馈详情")
    public ResultVO findById(@PathVariable Integer afId) {
        AqiFeedback feedback = aqiFeedbackService.getById(afId);
        return new ResultVO(200, "查询成功", feedback);
    }

    /**
     * NEPS公众监督员提交空气质量监督信息（用例3-4）
     * 服务端记录提交时间并置状态为“待指派”
     */
    @PostMapping("/save")
    @Operation(summary = "提交空气质量监督信息")
    public ResultVO save(@RequestBody AqiFeedback feedback) {
        String telId = feedback.getTelId() == null ? "" : feedback.getTelId().trim();
        Supervisor sup = supervisorService.getById(telId);
        if (sup == null) {
            throw new BusinessException("反馈者身份无效，请重新登录");
        }
        if (feedback.getProvinceId() == null || feedback.getCityId() == null) {
            throw new BusinessException("请选择完整网格区域（省、市）");
        }
        if (feedback.getAddress() == null || feedback.getAddress().isBlank()
                || feedback.getAddress().length() > 100) {
            throw new BusinessException("请填写有效地址");
        }
        if (feedback.getEstimatedGrade() == null
                || feedback.getEstimatedGrade() < 1 || feedback.getEstimatedGrade() > 6) {
            throw new BusinessException("请选择预估AQI等级");
        }
        if (feedback.getInformation() == null || feedback.getInformation().isBlank()) {
            throw new BusinessException("请填写空气质量描述");
        }
        // 服务端统一记录提交时间；新反馈状态=待指派
        LocalDateTime now = LocalDateTime.now();
        feedback.setAfId(null);
        feedback.setTelId(telId);
        if (feedback.getAfDate() == null || feedback.getAfDate().isBlank()) {
            feedback.setAfDate(now.format(DATE_FMT));
        }
        if (feedback.getAfTime() == null || feedback.getAfTime().isBlank()) {
            feedback.setAfTime(now.format(TIME_FMT));
        }
        feedback.setState(AqiFeedback.STATE_UNASSIGNED);
        feedback.setGmId(null);
        boolean success = aqiFeedbackService.save(feedback);
        log.info("监督员{}提交反馈，预估等级{}", telId, feedback.getEstimatedGrade());
        return new ResultVO(200, "提交成功", success);
    }

    @PostMapping("/update")
    @Operation(summary = "更新反馈")
    public ResultVO update(@RequestBody AqiFeedback feedback) {
        boolean success = aqiFeedbackService.updateById(feedback);
        return new ResultVO(200, "更新成功", success);
    }

    @DeleteMapping("/delete/{afId}")
    @Operation(summary = "删除反馈")
    public ResultVO delete(@PathVariable Integer afId) {
        boolean success = aqiFeedbackService.removeById(afId);
        return new ResultVO(200, "删除成功", success);
    }
}

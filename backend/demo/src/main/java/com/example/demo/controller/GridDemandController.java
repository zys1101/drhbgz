package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ResultVO;
import com.example.demo.entity.AqiFeedback;
import com.example.demo.entity.Employee;
import com.example.demo.entity.GridDemand;
import com.example.demo.mapper.AqiFeedbackMapper;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.mapper.GridCityMapper;
import com.example.demo.mapper.GridDemandMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 网格员增员请求接口。
 *
 * 业务规则：反馈所在网格区域没有可工作的本地网格员时，不允许异地指派，
 * 由管理员发起“增员请求”；该请求同时作为决策者判断“是否需要增加网格员”的依据。
 */
@Tag(name = "网格员增员请求")
@RestController
@RequestMapping("/gridDemand")
@AllArgsConstructor
@Slf4j
public class GridDemandController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GridDemandMapper demandMapper;
    private final AqiFeedbackMapper feedbackMapper;
    private final EmployeeMapper employeeMapper;
    private final GridCityMapper gridCityMapper;

    /**
     * 发起增员请求（本地无可用网格员时）
     */
    @PostMapping("/apply")
    @Operation(summary = "发起增员请求")
    public ResultVO apply(@RequestBody Map<String, Object> body) {
        Integer afId = body.get("afId") == null ? null : Integer.parseInt(String.valueOf(body.get("afId")));
        if (afId == null) {
            throw new BusinessException("缺少反馈编号");
        }
        AqiFeedback fb = feedbackMapper.selectById(afId);
        if (fb == null) {
            throw new BusinessException("反馈数据不存在");
        }
        // 若该区域已有可工作的本地网格员，应直接本地指派，不需要增员
        if (hasLocalWorker(fb.getProvinceId(), fb.getCityId())) {
            throw new BusinessException("该网格区域已有可工作的网格员，请直接本地指派");
        }
        // 同一区域已有待处理请求则复用，避免重复申请
        GridDemand exist = demandMapper.selectOne(new LambdaQueryWrapper<GridDemand>()
                .eq(GridDemand::getState, GridDemand.STATE_PENDING)
                .eq(GridDemand::getProvinceId, fb.getProvinceId())
                .eq(GridDemand::getCityId, fb.getCityId())
                .last("LIMIT 1"));
        if (exist != null) {
            return new ResultVO(200, "该网格区域已有待处理的增员请求（编号 " + exist.getDemandId() + "）", exist);
        }

        LocalDateTime now = LocalDateTime.now();
        String cityName = cityName(fb.getCityId());
        GridDemand d = new GridDemand();
        d.setProvinceId(fb.getProvinceId());
        d.setCityId(fb.getCityId());
        d.setAfId(afId);
        d.setReason(body.get("reason") == null
                ? "网格区域【" + cityName + "】无可工作的本地网格员，反馈任务无法指派，申请增加网格员"
                : String.valueOf(body.get("reason")));
        d.setState(GridDemand.STATE_PENDING);
        d.setApplyDate(now.format(DATE_FMT));
        d.setApplyTime(now.format(TIME_FMT));
        demandMapper.insert(d);
        log.info("反馈{}所在区域（{}-{}）无可工作网格员，已生成增员请求{}",
                afId, fb.getProvinceId(), fb.getCityId(), d.getDemandId());
        return new ResultVO(200, "已提交增员请求，等待决策者/管理员处理", d);
    }

    /** 增员请求列表（管理员跟进 / 决策者查看） */
    @GetMapping("/list")
    @Operation(summary = "增员请求列表")
    public ResultVO list(@RequestParam(required = false) Integer state,
                         @RequestParam(required = false) Integer cityId) {
        return new ResultVO(200, "查询成功", demandMapper.list(state, cityId));
    }

    /** 处理增员请求（已增员/已忽略） */
    @PostMapping("/handle")
    @Operation(summary = "处理增员请求")
    public ResultVO handle(@RequestBody Map<String, Object> body) {
        Integer demandId = body.get("demandId") == null ? null : Integer.parseInt(String.valueOf(body.get("demandId")));
        if (demandId == null) {
            throw new BusinessException("缺少请求编号");
        }
        GridDemand d = demandMapper.selectById(demandId);
        if (d == null) {
            throw new BusinessException("增员请求不存在");
        }
        if (d.getState() != GridDemand.STATE_PENDING) {
            throw new BusinessException("该请求已处理，不能重复处理");
        }
        Integer state = body.get("state") == null ? GridDemand.STATE_HANDLED
                : Integer.parseInt(String.valueOf(body.get("state")));
        if (state != GridDemand.STATE_HANDLED && state != GridDemand.STATE_IGNORED) {
            throw new BusinessException("处理结果只能是“已处理”或“已忽略”");
        }
        LocalDateTime now = LocalDateTime.now();
        d.setState(state);
        d.setHandleDate(now.format(DATE_FMT));
        d.setHandleTime(now.format(TIME_FMT));
        d.setHandleRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        demandMapper.updateById(d);
        return new ResultVO(200, state == GridDemand.STATE_HANDLED ? "已标记为已处理" : "已忽略该请求", null);
    }

    /** 该网格区域是否存在可工作的本地网格员 */
    private boolean hasLocalWorker(Integer provinceId, Integer cityId) {
        return employeeMapper.selectCount(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getRole, Employee.ROLE_GRID)
                .eq(Employee::getProvinceId, provinceId)
                .eq(Employee::getCityId, cityId)
                .eq(Employee::getWorking, 1)) > 0;
    }

    private String cityName(Integer cityId) {
        if (cityId == null) {
            return "未知区域";
        }
        var city = gridCityMapper.selectById(cityId);
        return city == null || city.getCityName() == null ? String.valueOf(cityId) : city.getCityName();
    }
}

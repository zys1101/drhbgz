package com.example.demo.controller;

import com.example.demo.common.ResultVO;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Leave;
import com.example.demo.entity.Supervisor;
import com.example.demo.service.IEmployeeService;
import com.example.demo.service.ILeaveService;
import com.example.demo.service.ISupervisorService;
import com.example.demo.util.SecretUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 人员管理（HR）接口
 * 覆盖原先由“东软HR系统”承担的职能：网格员账号注册与信息维护（地区/工作状态）、
 * 网格员请假申请与审批/销假、公众监督员信息管理
 */
@Tag(name = "人员管理（HR）")
@RestController
@AllArgsConstructor
@CrossOrigin
public class HrController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final IEmployeeService employeeService;
    private final ILeaveService leaveService;
    private final ISupervisorService supervisorService;

    /** 网格员列表（账号/姓名/负责省市/工作状态） */
    @GetMapping("/employee/list")
    @Operation(summary = "网格员列表")
    public ResultVO employeeList() {
        return new ResultVO(200, "查询成功", employeeService.selectGridWorkers());
    }

    /** 新增网格员账号（人员管理注册端口），密码按 nep$sha256 加密存储 */
    @PostMapping("/employee/save")
    @Operation(summary = "新增网格员账号")
    public ResultVO employeeSave(@RequestBody Employee body) {
        String code = body.getEmpCode() == null ? "" : body.getEmpCode().trim();
        if (code.isEmpty()) {
            return new ResultVO(400, "请输入登录编码", null);
        }
        if (body.getRealName() == null || body.getRealName().trim().isEmpty()) {
            return new ResultVO(400, "请输入真实姓名", null);
        }
        if (body.getPassword() == null || body.getPassword().length() < 6) {
            return new ResultVO(400, "初始密码不能少于6位", null);
        }
        if (body.getProvinceId() == null) {
            return new ResultVO(400, "请选择负责省份", null);
        }
        if (employeeService.count(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getEmpCode, code)) > 0) {
            return new ResultVO(400, "该登录编码已存在", null);
        }
        Employee emp = new Employee();
        emp.setEmpCode(code);
        emp.setRealName(body.getRealName().trim());
        emp.setPassword(SecretUtil.encrypt(body.getPassword()));
        emp.setRole(Employee.ROLE_GRID);
        emp.setProvinceId(body.getProvinceId());
        emp.setCityId(body.getCityId());
        emp.setWorking(body.getWorking() == null || body.getWorking() == 1 ? 1 : 0);
        employeeService.save(emp);
        return new ResultVO(200, "网格员账号创建成功", emp.getEmpId());
    }

    /** 修改网格员（姓名/负责地区/工作状态） */
    @PostMapping("/employee/update")
    @Operation(summary = "修改网格员")
    public ResultVO employeeUpdate(@RequestBody Employee body) {
        if (body.getEmpId() == null) {
            return new ResultVO(400, "缺少网格员编号", null);
        }
        Employee emp = employeeService.getById(body.getEmpId());
        if (emp == null) {
            return new ResultVO(400, "网格员不存在", null);
        }
        if (body.getRealName() != null && !body.getRealName().trim().isEmpty()) {
            emp.setRealName(body.getRealName().trim());
        }
        if (body.getProvinceId() != null) {
            emp.setProvinceId(body.getProvinceId());
        }
        if (body.getCityId() != null) {
            emp.setCityId(body.getCityId());
        }
        if (body.getWorking() != null) {
            emp.setWorking(body.getWorking());
        }
        employeeService.updateById(emp);
        return new ResultVO(200, "更新成功", null);
    }

    /** 网格员发起请假申请 */
    @PostMapping("/leave/apply")
    @Operation(summary = "网格员请假申请")
    public ResultVO leaveApply(@RequestBody Leave body) {
        String code = body.getEmpCode() == null ? "" : body.getEmpCode().trim();
        Employee emp = code.isEmpty() ? null : employeeService.getOne(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getEmpCode, code)
                .eq(Employee::getRole, Employee.ROLE_GRID));
        if (emp == null) {
            return new ResultVO(400, "网格员不存在", null);
        }
        String reason = body.getReason() == null ? "" : body.getReason().trim();
        if (reason.isEmpty()) {
            return new ResultVO(400, "请填写请假事由", null);
        }
        String start = body.getStartDate();
        String end = body.getEndDate();
        boolean dateOk = start != null && start.matches("\\d{4}-\\d{2}-\\d{2}")
                && end != null && end.matches("\\d{4}-\\d{2}-\\d{2}")
                && start.compareTo(end) <= 0;
        if (!dateOk) {
            return new ResultVO(400, "请选择正确的起止日期", null);
        }
        LocalDateTime now = LocalDateTime.now();
        Leave lv = new Leave();
        lv.setEmpId(emp.getEmpId());
        lv.setReason(reason);
        lv.setStartDate(start);
        lv.setEndDate(end);
        lv.setState(Leave.STATE_PENDING);
        lv.setApplyDate(now.format(DATE_FMT));
        lv.setApplyTime(now.format(TIME_FMT));
        leaveService.save(lv);
        return new ResultVO(200, "请假申请已提交，等待管理员审批", lv.getLeaveId());
    }

    /** 请假记录列表（管理员按人/按状态筛选；网格员查询本人记录） */
    @GetMapping("/leave/list")
    @Operation(summary = "请假记录列表")
    public ResultVO leaveList(@RequestParam(required = false) String empCode,
                              @RequestParam(required = false) Integer state) {
        return new ResultVO(200, "查询成功", leaveService.listLeave(empCode, state));
    }

    /** 审批请假：同意(进入请假状态=非工作)或驳回 */
    @PostMapping("/leave/approve")
    @Operation(summary = "请假审批")
    public ResultVO leaveApprove(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("leaveId");
        if (idObj == null) {
            return new ResultVO(400, "缺少请假编号", null);
        }
        Integer leaveId = Integer.valueOf(String.valueOf(idObj));
        Leave lv = leaveService.getById(leaveId);
        if (lv == null) {
            return new ResultVO(400, "请假申请不存在", null);
        }
        if (lv.getState() != Leave.STATE_PENDING) {
            return new ResultVO(400, "该申请已审批，不能重复处理", null);
        }
        boolean agree = Boolean.TRUE.equals(body.get("agree"))
                || "true".equalsIgnoreCase(String.valueOf(body.get("agree")));
        LocalDateTime now = LocalDateTime.now();
        if (agree) {
            lv.setState(Leave.STATE_APPROVED);
            Employee emp = employeeService.getById(lv.getEmpId());
            if (emp != null) {
                emp.setWorking(0);
                employeeService.updateById(emp);
            }
        } else {
            lv.setState(Leave.STATE_REJECTED);
        }
        lv.setApproveDate(now.format(DATE_FMT));
        lv.setApproveTime(now.format(TIME_FMT));
        leaveService.updateById(lv);
        return new ResultVO(200, agree ? "已同意请假，该网格员进入请假状态" : "已驳回请假申请", null);
    }

    /** 销假：网格员恢复工作状态 */
    @PostMapping("/leave/back")
    @Operation(summary = "销假")
    public ResultVO leaveBack(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("leaveId");
        if (idObj == null) {
            return new ResultVO(400, "缺少请假编号", null);
        }
        Integer leaveId = Integer.valueOf(String.valueOf(idObj));
        Leave lv = leaveService.getById(leaveId);
        if (lv == null || lv.getState() != Leave.STATE_APPROVED) {
            return new ResultVO(400, "仅已同意的请假可以销假", null);
        }
        LocalDateTime now = LocalDateTime.now();
        lv.setState(Leave.STATE_BACK);
        lv.setApproveDate(now.format(DATE_FMT));
        lv.setApproveTime(now.format(TIME_FMT));
        Employee emp = employeeService.getById(lv.getEmpId());
        if (emp != null) {
            emp.setWorking(1);
            employeeService.updateById(emp);
        }
        leaveService.updateById(lv);
        return new ResultVO(200, "已销假，网格员恢复工作状态", null);
    }

    /** 公众监督员列表（手机号/姓名/年龄/性别/绑定地区/地址/注册时间） */
    @GetMapping("/supervisor/list")
    @Operation(summary = "公众监督员列表")
    public ResultVO supervisorList() {
        return new ResultVO(200, "查询成功", supervisorService.selectAllWithNames());
    }

    /** 修改公众监督员信息（姓名/年龄/性别/绑定地区/地址） */
    @PostMapping("/supervisor/update")
    @Operation(summary = "修改公众监督员信息")
    public ResultVO supervisorUpdate(@RequestBody Supervisor body) {
        if (body.getTelId() == null) {
            return new ResultVO(400, "缺少监督员手机号", null);
        }
        Supervisor sup = supervisorService.getById(body.getTelId());
        if (sup == null) {
            return new ResultVO(400, "公众监督员不存在", null);
        }
        if (body.getRealName() != null && !body.getRealName().trim().isEmpty()) {
            sup.setRealName(body.getRealName().trim());
        }
        if (body.getAge() != null) {
            sup.setAge(body.getAge());
        }
        if (body.getGender() != null) {
            sup.setGender(body.getGender());
        }
        if (body.getProvinceId() != null) {
            sup.setProvinceId(body.getProvinceId());
        }
        if (body.getCityId() != null) {
            sup.setCityId(body.getCityId());
        }
        if (body.getAddress() != null) {
            sup.setAddress(body.getAddress().trim());
        }
        supervisorService.updateById(sup);
        return new ResultVO(200, "更新成功", null);
    }
}

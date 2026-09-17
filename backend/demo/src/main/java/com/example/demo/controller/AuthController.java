package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.BusinessException;
import com.example.demo.common.ResultVO;
import com.example.demo.entity.Employee;
import com.example.demo.entity.Supervisor;
import com.example.demo.service.IEmployeeService;
import com.example.demo.service.ISupervisorService;
import com.example.demo.util.SecretUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 认证接口：公众监督员注册/登录、员工（网格员/管理员/决策者）登录、监督员地址档案
 * 员工账号由“东软HR系统”统一管理，不能在本系统注册
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@CrossOrigin
@Slf4j
public class AuthController {

    private static final Pattern TEL_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Resource
    private ISupervisorService supervisorService;

    @Resource
    private IEmployeeService employeeService;

    /**
     * 公众监督员注册（用例3-1）
     * 注册信息：手机号码（身份唯一识别）、登录密码、真实姓名、年龄、性别
     */
    @PostMapping("/register")
    @Operation(summary = "公众监督员注册")
    public ResultVO register(@RequestBody Supervisor form) {
        String telId = form.getTelId() == null ? "" : form.getTelId().trim();
        if (!TEL_PATTERN.matcher(telId).matches()) {
            throw new BusinessException("请输入正确的11位手机号");
        }
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            throw new BusinessException("密码不能少于6位");
        }
        if (form.getRealName() == null || form.getRealName().isBlank()) {
            throw new BusinessException("请输入真实姓名");
        }
        if (form.getAge() == null || form.getAge() < 1 || form.getAge() > 120) {
            throw new BusinessException("请输入有效年龄");
        }
        if (form.getGender() == null || !(form.getGender().equals("男") || form.getGender().equals("女"))) {
            throw new BusinessException("请选择性别");
        }
        Long exists = supervisorService.count(
                new LambdaQueryWrapper<Supervisor>().eq(Supervisor::getTelId, telId));
        if (exists != null && exists > 0) {
            throw new BusinessException("该手机号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        Supervisor sup = new Supervisor();
        sup.setTelId(telId);
        sup.setPassword(SecretUtil.encrypt(form.getPassword()));
        sup.setRealName(form.getRealName().trim());
        sup.setAge(form.getAge());
        sup.setGender(form.getGender());
        sup.setRegisterDate(now.format(DATE_FMT));
        sup.setRegisterTime(now.format(TIME_FMT));
        supervisorService.save(sup);
        log.info("公众监督员注册成功：{}", telId);
        return new ResultVO(200, "注册成功", true);
    }

    /**
     * 登录（用例3-2/3-6）
     * 公众监督员使用手机号登录；网格员/管理员/决策者使用登录编码登录
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public ResultVO login(@RequestBody Map<String, String> body) {
        String account = body.getOrDefault("account", "").trim();
        String password = body.getOrDefault("password", "");
        String role = body.getOrDefault("role", "");

        if (account.isEmpty() || password.isEmpty()) {
            throw new BusinessException("请输入账号和密码");
        }

        String realRole;
        String realName;
        if ("supervisor".equals(role)) {
            Supervisor sup = supervisorService.getOne(
                    new LambdaQueryWrapper<Supervisor>().eq(Supervisor::getTelId, account));
            if (sup == null) {
                throw new BusinessException("该用户不存在，请先注册");
            }
            if (!SecretUtil.verify(password, sup.getPassword())) {
                throw new BusinessException("手机号或密码错误");
            }
            realRole = "supervisor";
            realName = sup.getRealName();
        } else {
            Employee emp = employeeService.getOne(
                    new LambdaQueryWrapper<Employee>().eq(Employee::getEmpCode, account));
            if (emp == null) {
                throw new BusinessException("账号或密码错误");
            }
            if (!SecretUtil.verify(password, emp.getPassword())) {
                throw new BusinessException("账号或密码错误");
            }
            if (emp.getWorking() == null || emp.getWorking() != 1) {
                throw new BusinessException("账号不可用，请联系管理员（账号状态由人员管理维护）");
            }
            realRole = emp.getRole();
            realName = emp.getRealName();
            if (role != null && !role.isBlank() && !role.equals(realRole)) {
                throw new BusinessException("该账号不属于当前选择的用户类型");
            }
        }

        Map<String, Object> user = new HashMap<>();
        user.put("account", account);
        user.put("role", realRole);
        user.put("username", realName);
        log.info("用户{}登录成功，角色：{}", account, realRole);
        return new ResultVO(200, "登录成功", user);
    }

    /**
     * 查询公众监督员档案（含绑定的网格地址）
     */
    @GetMapping("/profile")
    @Operation(summary = "查询监督员档案")
    public ResultVO profile(@RequestParam String account) {
        Supervisor sup = supervisorService.selectWithNames(account);
        if (sup == null) {
            throw new BusinessException("用户不存在");
        }
        return new ResultVO(200, "查询成功", sup);
    }

    /**
     * 保存公众监督员绑定的网格地址（用例3-3 选择网格地址）
     */
    @PostMapping("/profile")
    @Operation(summary = "保存监督员网格地址")
    public ResultVO saveProfile(@RequestBody Map<String, Object> body) {
        String account = String.valueOf(body.getOrDefault("account", ""));
        Integer provinceId = toInt(body.get("provinceId"));
        Integer cityId = toInt(body.get("cityId"));
        String address = body.get("address") == null ? "" : String.valueOf(body.get("address")).trim();

        Supervisor sup = supervisorService.getById(account);
        if (sup == null) {
            throw new BusinessException("用户不存在");
        }
        if (provinceId == null || cityId == null) {
            throw new BusinessException("请选择完整网格区域");
        }
        if (address.isEmpty() || address.length() > 100) {
            throw new BusinessException("请填写有效地址");
        }
        sup.setProvinceId(provinceId);
        sup.setCityId(cityId);
        sup.setAddress(address);
        supervisorService.updateById(sup);
        return new ResultVO(200, "地址保存成功", supervisorService.selectWithNames(account));
    }

    private Integer toInt(Object v) {
        if (v == null || "".equals(v)) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

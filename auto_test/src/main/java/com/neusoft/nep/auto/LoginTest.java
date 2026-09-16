package com.neusoft.nep.auto;

/**
 * 用例 1：用户登录自动化测试
 *
 * 覆盖登录页（/login）的用户类型切换与账号密码校验逻辑，包含 4 类角色的
 * 成功登录与各类失败场景，验证点：
 *  1. 成功登录：页面跳转到对应角色首页，并出现“登录成功，欢迎回来，xxx”提示
 *  2. 失败登录：停留登录页，出现对应错误提示（ElMessage.error）
 *  3. 空输入拦截：不输入账号密码直接点击登录，出现“请输入账号和密码”警告
 *
 * 演示账号（见 sql/nep_system.sql 种子数据，密码均为 123456）：
 *  公众监督员 13800001111 / 网格员 grid001、grid005（非工作）/ 管理员 admin / 决策者 viewer
 */
public class LoginTest extends BaseTest {

    /**
     * 测试数据矩阵：
     * {用户类型页签, 账号, 密码, 期望结果(success/fail), 期望提示关键字或落地页}
     */
    private static final String[][] CASES = {
            // ---------- 成功场景（4 类角色） ----------
            {"公众监督员", "13800001111", "123456", "success", "登录成功"},
            {"网格员",     "grid001",     "123456", "success", "登录成功"},
            {"管理员",     "admin",       "123456", "success", "登录成功"},
            {"决策者",     "viewer",      "123456", "success", "登录成功"},
            // ---------- 失败场景 ----------
            {"公众监督员", "13800001111", "wrongpwd", "fail", "手机号或密码错误"},
            {"公众监督员", "13900000000", "123456",   "fail", "该用户不存在，请先注册"},
            {"管理员",     "admin",       "wrongpwd", "fail", "账号或密码错误"},
            {"管理员",     "nobody",      "123456",   "fail", "账号或密码错误"},
            {"决策者",     "grid001",     "123456",   "fail", "该账号不属于当前选择的用户类型"},
            {"网格员",     "grid005",     "123456",   "fail", "账号不可用"}
    };

    public static void main(String[] args) {
        openBrowser();
        setSuiteName("LoginTest");
        try {
            for (String[] c : CASES) {
                String role = c[0], account = c[1], password = c[2], expect = c[3], expectText = c[4];
                System.out.println("---- 登录用例：" + role + " / " + account
                        + (password.equals("123456") ? "" : "（错误密码）") + " 期望：" + expect + " ----");
                try {
                    login(role, account, password);
                    sleep(1200);

                    String url = driver.getCurrentUrl();
                    String msg = getLastAuthMessage();
                    if ("success".equals(expect)) {
                        check("URL 已离开登录页", !url.contains("/login"), url);
                        check("提示包含“" + expectText + "”", msg.contains(expectText), msg);
                        check("落地页为角色首页", matchHome(role, url), url);
                    } else {
                        check("URL 仍停留在登录页", url.contains("/login"), url);
                        check("错误提示包含“" + expectText + "”", msg.contains(expectText), msg);
                    }
                    screenshot("Login_" + role + "_" + account + (expect.equals("success") ? "_success" : "_fail"));
                } catch (Exception e) {
                    check("用例执行未抛出异常", false, e.getMessage());
                }
            }

            // ---------- 空输入拦截 ----------
            System.out.println("---- 登录用例：空输入直接点击登录（期望警告提示） ----");
            try {
                login("管理员", "", "");
                sleep(1200);
                String msg = getLastAuthMessage();
                check("空输入出现“请输入账号和密码”警告", msg.contains("请输入账号和密码"), msg);
                check("仍停留在登录页", driver.getCurrentUrl().contains("/login"), driver.getCurrentUrl());
                screenshot("Login_empty");
            } catch (Exception e) {
                check("空输入用例执行未抛出异常", false, e.getMessage());
            }

            // ---------- 未登录访问受保护页面应重定向回登录页 ----------
            System.out.println("---- 登录用例：未登录直接访问 /admin/feedback（期望重定向到登录页） ----");
            driver.get(BASE_URL + "/admin/feedback");
            sleep(1500);
            check("被重定向到登录页", driver.getCurrentUrl().contains("/login"), driver.getCurrentUrl());

            summary("用户登录自动化测试");
        } finally {
            closeBrowser();
        }
    }

    /** 各角色登录成功后的首页路由（与 front/src/constants/aqi.js 的 ROLES.home 一致） */
    private static boolean matchHome(String role, String url) {
        switch (role) {
            case "公众监督员": return url.contains("/sf/submit");
            case "网格员":     return url.contains("/gw/tasks");
            case "决策者":     return url.contains("/screen");
            case "管理员":     return url.endsWith("/");
            default:           return true;
        }
    }
}

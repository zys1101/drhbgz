package com.neusoft.nep.auto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * 用例 2：公众监督员「注册 + 提交监督反馈」自动化测试
 *
 * 业务背景：公众监督员是数据源头角色，注册绑定手机号后可提交空气质量反馈，
 * 提交成功后进入管理员“待指派”队列，并可在“历史反馈”中查看本人记录。
 *
 * 覆盖：
 *  1. 注册页（/register）：新手机号注册成功并跳转登录页
 *  2. 新账号登录成功
 *  3. 已绑定网格地址的监督员（13800002222 李娜/大连）提交反馈成功
 *  4. “历史反馈”页面能看到刚提交的记录
 */
public class SupervisorFeedbackTest extends BaseTest {

    /** 本次运行的唯一手机号（137 + 8 位随机数，保证可重复执行） */
    private static String newPhone() {
        long n = 10000000L + (long) (Math.random() * 90000000);
        return "137" + n;
    }

    /** 本次运行的唯一反馈描述（便于在列表/历史页中检索定位） */
    private static String marker() {
        return "自动化测试标识AT" + (System.currentTimeMillis() % 1000000);
    }

    public static void main(String[] args) throws Exception {
        openBrowser();
        setSuiteName("SupervisorFeedbackTest");
        String phone = newPhone();
        String mark = marker();
        try {
            // ---------- 1. 注册新公众监督员 ----------
            System.out.println("---- 注册用例：新手机号 " + phone + " ----");
            driver.get(BASE_URL + "/register");
            wait.until(webDriver -> "complete".equals(
                    ((org.openqa.selenium.JavascriptExecutor) webDriver).executeScript("return document.readyState")));
            fillInput("请输入11位手机号", phone);
            fillInput("便于工作人员联系", "自动化测试监督员");
            // 年龄默认 25，性别默认“男”，无需修改
            fillInput("至少6位", "123456");
            fillInput("请再次输入密码", "123456");
            clickButtonByClass("register-btn");
            sleep(1500);
            check("注册成功提示", getLastMessage().contains("注册成功"), getLastMessage());
            check("注册后跳转登录页", driver.getCurrentUrl().contains("/login"), driver.getCurrentUrl());
            screenshot("Register_success");

            // ---------- 2. 新账号登录 ----------
            System.out.println("---- 登录用例：新注册账号 " + phone + " 登录 ----");
            login("公众监督员", phone, "123456");
            check("新账号登录成功", getLastAuthMessage().contains("登录成功"), getLastAuthMessage());

            // ---------- 3. 已绑定地址的监督员提交反馈 ----------
            System.out.println("---- 反馈提交用例：13800002222 提交含标识【" + mark + "】的反馈 ----");
            login("公众监督员", "13800002222", "123456");
            // 登录成功自动进入 /sf/submit
            sleep(1000);
            check("进入提交反馈页", driver.getCurrentUrl().contains("/sf/submit"), driver.getCurrentUrl());

            // 选择预估空气等级：定位“预估空气等级”字段内的 el-select
            WebElement field = wait.until(org.openqa.selenium.support.ui.ExpectedConditions
                    .visibilityOfElementLocated(By.xpath(
                            "//div[contains(@class,'app-field') and .//label[contains(text(),'预估空气等级')]]")));
            openSelect(field);
            chooseDropdownItem("二级（良）", field);
            // 填写空气质量描述
            fillTextarea("描述您观测到的空气情况", mark + "：建筑工地围挡缺失，扬尘较大，建议洒水降尘。");
            // 提交
            // 提交按钮：点击后校验是否出现成功提示，未生效则回退 JS 点击
            // （本环境原生点击偶发“已送达但 Vue 无响应”，静默不提交会让后续断言全部失败）
            clickVerified(findButtonByContainsText("提交反馈"),
                    () -> getLastMessage().contains("提交成功"), "提交反馈");
            sleep(1200);
            check("提交成功提示", getLastMessage().contains("提交成功"), getLastMessage());
            screenshot("Feedback_submit_success");

            // ---------- 4. 历史反馈中可见（详情弹窗包含反馈描述） ----------
            System.out.println("---- 历史反馈用例：核对刚提交的记录 ----");
            openMenu("历史反馈");
            sleep(1200);
            List<WebElement> cards = driver.findElements(By.cssSelector(".feed-card"));
            check("历史反馈列表非空", !cards.isEmpty(), "暂无反馈记录");
            if (!cards.isEmpty()) {
                // 卡片按提交时间倒序，正常情况第一条就是刚提交的记录；
                // 但同一天可能存在更晚的种子数据，因此按顺序最多核对 5 条，
                // 找到含本次唯一标识的那条即通过。
                boolean found = false;
                int limit = Math.min(cards.size(), 5);
                for (int i = 0; i < limit && !found; i++) {
                    List<WebElement> fresh = driver.findElements(By.cssSelector(".feed-card"));
                    if (i >= fresh.size()) {
                        break;
                    }
                    // 卡片点击在本环境偶发丢失，点击后校验详情弹窗已弹出，未生效则 JS 点击
                    clickVerified(fresh.get(i), () -> {
                        for (WebElement d : driver.findElements(By.cssSelector(".el-dialog"))) {
                            if (d.isDisplayed()) {
                                return true;
                            }
                        }
                        return false;
                    }, "打开反馈详情弹窗");
                    sleep(600);
                    for (WebElement d : driver.findElements(By.cssSelector(".el-dialog"))) {
                        if (d.isDisplayed() && d.getText().contains(mark)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // 关闭当前弹窗，继续核对下一条
                        for (WebElement btn : driver.findElements(
                                By.cssSelector(".el-dialog__footer button"))) {
                            if (btn.isDisplayed()) {
                                btn.click();
                                break;
                            }
                        }
                        sleep(600);
                    }
                }
                check("历史反馈详情弹窗包含本次反馈描述（唯一标识）", found,
                        "已核对前 " + limit + " 条记录，未找到标识 " + mark);
                screenshot("Feedback_history");
            }

            summary("公众监督员注册与反馈提交自动化测试");
        } catch (Exception e) {
            exception("用例执行中断", e);
        } finally {
            closeBrowser();
        }
    }
}

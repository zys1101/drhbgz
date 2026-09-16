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
            check("新账号登录成功", getLastMessage().contains("登录成功"), getLastMessage());

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
            selectClickable(field).click();
            sleep(500);
            clickVisibleDropdownItem("二级（良）");
            // 填写空气质量描述
            fillTextarea("描述您观测到的空气情况", mark + "：建筑工地围挡缺失，扬尘较大，建议洒水降尘。");
            // 提交
            clickButtonByContainsText("提交反馈");
            sleep(1500);
            check("提交成功提示", getLastMessage().contains("提交成功"), getLastMessage());
            screenshot("Feedback_submit_success");

            // ---------- 4. 历史反馈中可见（详情弹窗包含反馈描述） ----------
            System.out.println("---- 历史反馈用例：核对刚提交的记录 ----");
            openMenu("历史反馈");
            sleep(1200);
            // 点击第一条反馈卡片（按提交时间倒序，最新在前）
            List<WebElement> cards = driver.findElements(By.cssSelector(".feed-card"));
            check("历史反馈列表非空", !cards.isEmpty(), "暂无反馈记录");
            if (!cards.isEmpty()) {
                cards.get(0).click();
                sleep(1000);
                WebElement dialog = driver.findElement(By.cssSelector(".el-dialog"));
                check("详情弹窗包含本次反馈描述（唯一标识）",
                        dialog.isDisplayed() && dialog.getText().contains(mark),
                        "详情弹窗未找到标识 " + mark);
                screenshot("Feedback_history");
            }

            summary("公众监督员注册与反馈提交自动化测试");
        } finally {
            closeBrowser();
        }
    }

    /** 在所有已显示的下拉选项中点击包含指定文字的一项 */
    private static void clickVisibleDropdownItem(String optionText) {
        for (WebElement item : driver.findElements(By.cssSelector(".el-select-dropdown__item"))) {
            if (item.isDisplayed() && item.getText().contains(optionText)) {
                item.click();
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("下拉选项不存在：" + optionText);
    }
}

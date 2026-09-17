package com.neusoft.nep.auto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * 用例 5：系统管理员「网格员管理（增/改）+ 请假审批」自动化测试
 *
 * 人员管理是系统的账号来源：网格员/管理员/决策者账号均由 HR 模块维护，
 * “同意请假”后账号进入非工作状态，将无法登录系统。
 *
 * 覆盖：
 *  1. 新增网格员（登录编码/姓名/初始密码/负责省+城市）→ 列表出现新账号
 *  2. 编辑该网格员姓名 → 列表同步更新
 *  3. 该网格员登录系统并提交请假申请
 *  4. 管理员在“请假审批”页同意申请 → 提示进入请假状态
 *  5. 网格员工列表中该账号“工作状态”变为“请假中”
 */
public class HrGridTest extends BaseTest {

    /** 本次运行创建的唯一登录编码（grid + 4 位数字，避免与种子数据 grid001~grid010 冲突） */
    private static final String NEW_CODE = "grid" + String.format("%04d", System.currentTimeMillis() % 10000);
    private static final String NEW_NAME = "自动化测试网格员";
    private static final String EDIT_NAME = "自动化测试改名";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) throws Exception {
        openBrowser();
        setSuiteName("HrGridTest");
        try {
            login("管理员", "admin", "123456");
            sleep(800);
            openMenu("网格员管理");
            sleep(800);

            // ---------- 1. 新增网格员 ----------
            System.out.println("---- 新增用例：创建网格员 " + NEW_CODE + " ----");
            clickButtonByContainsText("新增网格员");
            sleep(1000);
            WebElement dialog = driver.findElement(By.cssSelector(".el-dialog"));
            check("新增弹窗已打开",
                    wait.until(ExpectedConditions.visibilityOf(dialog)).isDisplayed(),
                    "弹窗未出现");
            fillInput("如 grid011", NEW_CODE);
            fillInput("真实姓名", NEW_NAME);
            fillInput("不少于6位", PASSWORD);
            selectOption("选择省份", "辽宁省");
            sleep(1200); // 等待城市列表异步加载完成
            selectOption("选择城市", "沈阳市");
            clickButtonByText("保存");
            sleep(1500);
            check("新增成功提示", getLastMessage().contains("成功"), getLastMessage());

            // 列表中出现新账号（默认每页 10 条，新账号可能不在第一页，直接按编码检索表格）
            boolean rowAppeared = findEmployeeRow(NEW_CODE) != null;
            check("网格员工列表包含新账号 " + NEW_CODE, rowAppeared, "表格中未找到该编码");
            screenshot("Hr_add_success");

            // ---------- 2. 编辑姓名 ----------
            System.out.println("---- 编辑用例：修改 " + NEW_CODE + " 姓名为“" + EDIT_NAME + "” ----");
            WebElement row = findEmployeeRow(NEW_CODE);
            if (row != null) {
                row.findElement(By.xpath(".//button[.//span[normalize-space()='编辑']]")).click();
                sleep(1000);
                WebElement nameInput = driver.findElement(By.xpath("//input[@placeholder='真实姓名']"));
                clearInput(nameInput);
                nameInput.sendKeys(EDIT_NAME);
                clickButtonByText("保存");
                sleep(1500);
                check("编辑成功提示", getLastMessage().contains("成功"), getLastMessage());
                WebElement rowAfter = findEmployeeRow(NEW_CODE);
                check("列表姓名已更新", rowAfter != null && rowAfter.getText().contains(EDIT_NAME),
                        rowAfter == null ? "行不存在" : rowAfter.getText());
                screenshot("Hr_edit_success");
            }

            // ---------- 3. 新网格员登录并提交请假 ----------
            System.out.println("---- 请假用例：" + NEW_CODE + " 登录并提交请假申请 ----");
            login("网格员", NEW_CODE, PASSWORD);
            sleep(1000);
            check("新账号可登录（进入我的任务）", driver.getCurrentUrl().contains("/gw/tasks"),
                    driver.getCurrentUrl());
            openMenu("请假申请");
            sleep(800);
            fillTextarea("请填写请假原因", "自动化测试请假：家庭事务");
            fillDateRange("开始日期", "结束日期", "2026-09-21", "2026-09-22");
            clickButtonByContainsText("提交申请");
            sleep(1500);
            check("请假申请提交成功", getLastMessage().contains("请假申请已提交"), getLastMessage());
            screenshot("Leave_apply_success");

            // ---------- 4. 管理员审批同意 ----------
            System.out.println("---- 审批用例：管理员同意 " + NEW_CODE + " 的请假申请 ----");
            login("管理员", "admin", "123456");
            sleep(800);
            openMenu("网格员管理");
            sleep(800);
            // 切换到“请假审批”页签
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//div[contains(@class,'el-tabs__item') and normalize-space(.)='请假审批']"))).click();
            sleep(1000);
            WebElement leaveRow = null;
            // 只用本次运行唯一的登录编码定位，避免命中历史遗留数据：
            // EDIT_NAME 是固定常量，上一轮运行残留的“已同意·请假中”记录会被误选。
            for (WebElement r : driver.findElements(By.xpath(
                    "//div[contains(@class,'el-table__body-wrapper')]//tbody/tr"))) {
                if (r.getText().contains(NEW_CODE)) {
                    leaveRow = r;
                    break;
                }
            }
            check("请假审批列表包含本次申请", leaveRow != null, "未找到对应请假记录");
            if (leaveRow != null) {
                check("申请状态为“待审批”", leaveRow.getText().contains("待审批"), leaveRow.getText());
                leaveRow.findElement(By.xpath(".//button[.//span[normalize-space()='同意']]")).click();
                sleep(800);
                confirmMessageBox();
                sleep(1500);
                check("审批同意提示（进入请假状态）", getLastMessage().contains("已同意请假"), getLastMessage());
                screenshot("Leave_approve_success");
            }

            // ---------- 5. 工作状态校验 ----------
            System.out.println("---- 状态用例：校验 " + NEW_CODE + " 工作状态为“请假中” ----");
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                    "//div[contains(@class,'el-tabs__item') and normalize-space(.)='网格员管理']"))).click();
            sleep(1000);
            WebElement finalRow = findEmployeeRow(NEW_CODE);
            check("账号状态变为“请假中”", finalRow != null && finalRow.getText().contains("请假中"),
                    finalRow == null ? "行不存在" : finalRow.getText());

            // ---------- 6. 请假中账号无法登录（状态联动验证） ----------
            System.out.println("---- 联动用例：请假中账号 " + NEW_CODE + " 再次登录应被拒绝 ----");
            login("网格员", NEW_CODE, PASSWORD);
            check("登录被拒绝（账号不可用）", getLastAuthMessage().contains("账号不可用"), getLastAuthMessage());
            check("停留在登录页", driver.getCurrentUrl().contains("/login"), driver.getCurrentUrl());
            screenshot("Leave_login_blocked");

            summary("网格员管理与请假审批自动化测试");
        } catch (Exception e) {
            exception("用例执行中断", e);
        } finally {
            closeBrowser();
        }
    }

    /** 在网格员工表格中按登录编码定位整行 */
    private static WebElement findEmployeeRow(String code) {
        for (WebElement r : driver.findElements(By.xpath(
                "//div[contains(@class,'el-table__body-wrapper')]//tbody/tr"))) {
            if (r.getText().contains(code)) {
                return r;
            }
        }
        return null;
    }
}

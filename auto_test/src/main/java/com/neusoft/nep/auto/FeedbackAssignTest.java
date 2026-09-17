package com.neusoft.nep.auto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * 用例 3：系统管理员「监督数据查询 + 指派网格员 + 删除反馈」自动化测试
 *
 * 业务流程：公众监督员提交反馈（状态=待指派）→ 管理员在“监督数据列表”中
 * 按条件查询 → 点击“指派”选择**本地**网格员 → 确认后反馈状态变为“已指派”，
 * 同时进入网格员“我的任务”；管理员可删除反馈数据。
 *
 * 业务规则变更说明：现在**只允许本地指派**（网格员负责的省+市须与反馈一致）。
 * 若某反馈所在区域没有可工作的本地网格员，指派弹窗不会提供可选项，
 * 而应改为发起“增员请求”。因此本用例改为逐行尝试，找到一条确实可本地指派的反馈。
 *
 * 覆盖：
 *  1. 按状态“待指派”过滤反馈列表
 *  2. 指派弹窗：选择可用的本地网格员 → 确认指派 → 成功提示与状态流转
 *  3. 删除反馈：确认弹窗 → 删除成功提示
 */
public class FeedbackAssignTest extends BaseTest {

    public static void main(String[] args) throws Exception {
        openBrowser();
        setSuiteName("FeedbackAssignTest");
        try {
            login("管理员", "admin", "123456");
            sleep(800);
            openMenu("监督数据列表");
            sleep(800);

            // ---------- 1. 按状态“待指派”过滤 ----------
            System.out.println("---- 查询用例：按状态=待指派过滤反馈列表 ----");
            selectOption("状态", "待指派");
            clickButtonByContainsText("查询");
            sleep(1500);
            List<WebElement> rows = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.cssSelector(".el-table__body-wrapper tbody tr")));
            check("过滤出待指派反馈（数量>0）", !rows.isEmpty(), "列表为空");
            check("首行状态列为“待指派”",
                    !rows.isEmpty() && rows.get(0).getText().contains("待指派"),
                    rows.isEmpty() ? "列表为空" : rows.get(0).getText());
            screenshot("Feedback_filter_待指派");

            // ---------- 2. 指派本地网格员 ----------
            System.out.println("---- 指派用例：找一条可本地指派的反馈并指派 ----");
            WebElement chosen = null;
            String workerCode = "";
            int tried = 0;
            int limit = Math.min(rows.size(), 8);
            for (int i = 0; i < limit && chosen == null; i++) {
                List<WebElement> fresh = driver.findElements(
                        By.cssSelector(".el-table__body-wrapper tbody tr"));
                if (i >= fresh.size()) {
                    break;
                }
                tried++;
                // 打开指派弹窗（点击后校验弹窗出现，未生效则回退 JS 点击）
                clickVerified(fresh.get(i).findElement(
                                By.xpath(".//button[.//span[normalize-space()='指派']]")),
                        () -> {
                            for (WebElement d : driver.findElements(By.cssSelector(".el-dialog"))) {
                                if (d.isDisplayed()) {
                                    return true;
                                }
                            }
                            return false;
                        }, "打开指派弹窗");
                sleep(800);

                // 本地有在职网格员时弹窗才会给出可选项
                for (WebElement r : driver.findElements(By.cssSelector("input.worker-radio"))) {
                    if (r.isEnabled()) {
                        chosen = r;
                        workerCode = r.getAttribute("value");
                        break;
                    }
                }
                if (chosen == null) {
                    closeAssignDialog();   // 该区域无本地在职网格员，换下一条
                    sleep(500);
                }
            }
            check("找到可本地指派的待指派反馈（尝试 " + tried + " 行）", chosen != null,
                    "前 " + limit + " 行所在区域都没有可工作的本地网格员");

            if (chosen != null) {
                System.out.println("     选中本地网格员：" + workerCode);
                chosen.click();
                sleep(500);
                clickButtonByText("确认指派");
                sleep(1500);
                check("指派成功提示", getLastMessage().contains("指派成功"), getLastMessage());

                // 状态流转验证：重新按“已指派”过滤，应能看到刚才指派的结果
                selectOption("状态", "已指派");
                clickButtonByContainsText("查询");
                sleep(1500);
                List<WebElement> assignedRows = driver.findElements(
                        By.cssSelector(".el-table__body-wrapper tbody tr"));
                check("按“已指派”过滤后有数据", !assignedRows.isEmpty(), "列表为空");
                if (!assignedRows.isEmpty()) {
                    check("首行状态为“已指派”", assignedRows.get(0).getText().contains("已指派"),
                            assignedRows.get(0).getText());
                }
                screenshot("Feedback_assign_success");
            }

            // ---------- 3. 删除反馈 ----------
            System.out.println("---- 删除用例：删除一条待指派反馈 ----");
            selectOption("状态", "待指派");
            clickButtonByContainsText("查询");
            sleep(1500);
            List<WebElement> remain = driver.findElements(By.cssSelector(".el-table__body-wrapper tbody tr"));
            if (!remain.isEmpty()) {
                String firstId = remain.get(0).getText().trim().split("\\s+")[0];
                clickVerified(remain.get(0).findElement(
                                By.xpath(".//button[.//span[normalize-space()='删除']]")),
                        () -> {
                            for (WebElement box : driver.findElements(By.cssSelector(".el-message-box"))) {
                                if (box.isDisplayed()) {
                                    return true;
                                }
                            }
                            return false;
                        }, "打开删除确认框");
                confirmMessageBox();
                sleep(1500);
                check("删除成功提示", getLastMessage().contains("删除成功"), getLastMessage());

                List<WebElement> after = driver.findElements(By.cssSelector(".el-table__body-wrapper tbody tr"));
                boolean stillFirst = !after.isEmpty()
                        && after.get(0).getText().trim().split("\\s+")[0].equals(firstId);
                check("被删除的反馈已不在列表首行", !stillFirst, "编号 " + firstId + " 仍在首行");
                screenshot("Feedback_delete_success");
            } else {
                System.out.println("  [跳过] 当前没有待指派反馈可供删除");
            }

            summary("管理员监督数据查询/指派/删除自动化测试");
        } catch (Exception e) {
            exception("用例执行中断", e);
        } finally {
            closeBrowser();
        }
    }

    /** 关闭指派弹窗（点击“取消”），用于跳过没有本地可选网格员的反馈 */
    private static void closeAssignDialog() {
        for (WebElement b : driver.findElements(By.cssSelector(".el-dialog__footer button"))) {
            if (b.isDisplayed() && "取消".equals(b.getText().trim())) {
                b.click();
                return;
            }
        }
    }
}

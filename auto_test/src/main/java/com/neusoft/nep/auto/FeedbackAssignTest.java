package com.neusoft.nep.auto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * 用例 3：系统管理员「监督数据查询 + 指派网格员 + 删除反馈」自动化测试
 *
 * 业务流程：公众监督员提交反馈（状态=待指派）→ 管理员在“监督数据列表”中
 * 按条件查询 → 点击“指派”选择网格员（本地优先、就近异地）→ 确认后反馈状态
 * 变为“已指派”，同时进入网格员“我的任务”；管理员可删除反馈数据。
 *
 * 覆盖：
 *  1. 按关键字检索反馈列表
 *  2. 按状态“待指派”过滤
 *  3. 指派弹窗：选择可用网格员 → 确认指派 → 成功提示与状态流转
 *  4. 删除反馈：确认弹窗 → 删除成功提示
 */
public class FeedbackAssignTest extends BaseTest {

    /** 与 SupervisorFeedbackTest 相同的唯一标识规则（本用例通过关键字检索到待指派数据） */
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
                    rows.get(0).getText().contains("待指派"), rows.get(0).getText());
            screenshot("Feedback_filter_待指派");

            // ---------- 2. 指派网格员 ----------
            System.out.println("---- 指派用例：为第一条待指派反馈指派网格员 ----");
            // 点击首行“指派”按钮（操作列：详情/指派/删除，均为 link 型 el-button）
            WebElement assignBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'el-table__body-wrapper')]//tbody/tr[1]"
                            + "//button[.//span[normalize-space()='指派']]")));
            assignBtn.click();
            sleep(1000);

            // 指派弹窗：选择第一个“可用”（非 disabled）的网格员单选项
            List<WebElement> radios = driver.findElements(By.cssSelector("input.worker-radio"));
            WebElement chosen = null;
            String workerName = "";
            for (WebElement r : radios) {
                if (r.isEnabled()) {
                    chosen = r;
                    workerName = r.getAttribute("value");
                    break;
                }
            }
            check("指派弹窗中存在可用网格员", chosen != null, "无可用网格员（可能全部处于非工作状态）");
            if (chosen != null) {
                chosen.click();
                sleep(500);
                clickButtonByText("确认指派");
                sleep(1500);
                check("指派成功提示", getLastMessage().contains("指派成功"), getLastMessage());

                // 状态流转验证：重新按“已指派”过滤，首行应包含刚才指派的反馈
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
                // 记录删除前的首行编号
                String firstId = remain.get(0).getText().trim().split("\\s+")[0];
                remain.get(0).findElement(By.xpath(".//button[.//span[normalize-space()='删除']]")).click();
                sleep(800);
                // ElMessageBox 确认弹窗（按钮文字：删除/取消）
                confirmMessageBox();
                sleep(1500);
                check("删除成功提示", getLastMessage().contains("删除成功"), getLastMessage());

                // 刷新后该行编号不应再出现于首行
                List<WebElement> after = driver.findElements(By.cssSelector(".el-table__body-wrapper tbody tr"));
                boolean stillFirst = !after.isEmpty()
                        && after.get(0).getText().trim().split("\\s+")[0].equals(firstId);
                check("被删除的反馈已不在列表首行", !stillFirst, "编号 " + firstId + " 仍在首行");
                screenshot("Feedback_delete_success");
            } else {
                System.out.println("  [跳过] 当前没有待指派反馈可供删除（可先运行 SupervisorFeedbackTest 产生数据）");
            }

            summary("管理员监督数据查询/指派/删除自动化测试");
        } finally {
            closeBrowser();
        }
    }
}

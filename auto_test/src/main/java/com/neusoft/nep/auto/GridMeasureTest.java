package com.neusoft.nep.auto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * 用例 4：端到端业务链路自动化测试
 * 「监督员提交反馈 → 管理员指派网格员 → 网格员录入实测数据 → 管理员确认AQI数据」
 *
 * 该用例串联四类角色中的三类（公众监督员/管理员/网格员），验证系统核心业务闭环：
 *  1. 监督员 13800004444（赵秀英/哈尔滨）提交含唯一标识的反馈
 *  2. 管理员按关键字检索到该反馈（待指派），指派给 grid001（王铁柱）
 *  3. grid001 在“我的任务”中找到该任务，录入三项污染物等级：
 *     SO₂=二级（良）、CO=二级（良）、PM2.5=三级（轻度污染）
 *     系统按 AQI = MAX(SO2AQI, COAQI, PM2.5AQI) 自动计算为“三级（轻度污染）”
 *  4. 管理员在“确认AQI数据”中检索该条实测数据（待确认）并确认，
 *     反馈任务最终状态流转为“已确认”，数据纳入统计。
 */
public class GridMeasureTest extends BaseTest {

    /** 唯一标识：用于跨页面检索本次测试产生的数据 */
    private static final String MARK = "自动化链路标识E2E" + (System.currentTimeMillis() % 1000000);

    /** 所用监督员的绑定地址（用于在“确认AQI数据”页按地址检索） */
    private static final String SUP_ADDRESS = "松北区世纪大道1号";

    public static void main(String[] args) throws Exception {
        openBrowser();
        setSuiteName("GridMeasureTest");
        try {
            // ========== 1. 监督员提交反馈 ==========
            System.out.println("---- 步骤1：监督员 13800004444 提交反馈【" + MARK + "】 ----");
            login("公众监督员", "13800004444", "123456");
            sleep(1000);
            WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                    "//div[contains(@class,'app-field') and .//label[contains(text(),'预估空气等级')]]")));
            openSelect(field);
            chooseDropdownItem("三级（轻度污染）", field);
            fillTextarea("描述您观测到的空气情况", MARK + "：燃煤电厂厂区周边有刺鼻气味，疑似 SO₂ 超标，请核实。");
            clickButtonByContainsText("提交反馈");
            sleep(1500);
            check("反馈提交成功", getLastMessage().contains("提交成功"), getLastMessage());

            // ========== 2. 管理员检索并指派给 grid001 ==========
            System.out.println("---- 步骤2：管理员按标识检索并指派给 grid001 ----");
            login("管理员", "admin", "123456");
            sleep(800);
            openMenu("监督数据列表");
            sleep(800);
            fillInput("地区 / 地址 / 手机号 / 描述关键字", MARK);
            clickButtonByContainsText("查询");
            sleep(1500);
            List<WebElement> rows = driver.findElements(By.xpath(
                    "//div[contains(@class,'el-table__body-wrapper')]//tbody/tr"));
            check("检索到本次反馈（1 行）", rows.size() == 1, "行数=" + rows.size());
            if (rows.size() == 1) {
                check("反馈状态为“待指派”", rows.get(0).getText().contains("待指派"), rows.get(0).getText());
                rows.get(0).findElement(By.xpath(".//button[.//span[normalize-space()='指派']]")).click();
                sleep(1000);

                // 在指派弹窗中选择 grid001
                boolean found = false;
                for (WebElement r : driver.findElements(By.cssSelector("input.worker-radio"))) {
                    if ("grid001".equals(r.getAttribute("value")) && r.isEnabled()) {
                        r.click();
                        found = true;
                        break;
                    }
                }
                check("指派弹窗中选中 grid001", found, "grid001 不存在或不可用");
                if (found) {
                    clickButtonByText("确认指派");
                    sleep(1500);
                    check("指派成功提示", getLastMessage().contains("指派成功"), getLastMessage());
                    screenshot("E2E_assign_success");
                }
            }

            // ========== 3. grid001 录入实测数据 ==========
            System.out.println("---- 步骤3：grid001 录入实测数据（SO₂=良, CO=良, PM2.5=轻度污染） ----");
            login("网格员", "grid001", "123456");
            sleep(1000);
            check("进入“我的任务”", driver.getCurrentUrl().contains("/gw/tasks"), driver.getCurrentUrl());
            // 找到包含标识的任务卡片，点击其“录入实测数据”
            WebElement taskCard = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//div[contains(@class,'task-card')][.//p[contains(text(),'" + MARK + "')]]")));
            // 该按钮触发路由跳转：点击后校验 URL，未生效则回退 JS 点击
            clickVerified(taskCard.findElement(By.xpath(".//button[contains(.,'录入实测数据')]")),
                    () -> driver.getCurrentUrl().contains("/gw/measure"), "录入实测数据");
            sleep(600);
            check("进入实测录入页", driver.getCurrentUrl().contains("/gw/measure"), driver.getCurrentUrl());

            // 依次选择三个污染物等级（DOM 顺序：SO₂、CO、PM2.5）
            // 注意：不能用 "//div[contains(@class,'el-select')]" 计数——EP 的 select 内部
            // 还有 el-select__wrapper / __selection / __placeholder 等大量同前缀 div，
            // 一个下拉框会被数成 6 个。这里按 .measure-item 录入项计数。
            List<WebElement> items = driver.findElements(By.cssSelector(".measure-item"));
            check("实测录入表单包含 3 个污染物录入项", items.size() == 3, "数量=" + items.size());
            if (items.size() == 3) {
                // 按 DOM 顺序选择三个污染物的等级（.measure-item 内每行一个下拉框）
                selectOptionInScope(".measure-item", 0, "二级（良）");
                selectOptionInScope(".measure-item", 1, "二级（良）");
                selectOptionInScope(".measure-item", 2, "三级（轻度污染）");
                sleep(500);
                // AQI = MAX(2,2,3) = 3 → 页面应显示“三级（轻度污染）”
                String bodyText = driver.findElement(By.tagName("body")).getText();
                check("系统自动计算 AQI 等级为“三级（轻度污染）”",
                        bodyText.contains("当前网格区域 AQI 等级") && bodyText.contains("三级（轻度污染）"),
                        "结果区文案不符合预期");
                screenshot("E2E_measure_form");
                clickButtonByContainsText("提交实测数据");
                sleep(1500);
                check("实测数据提交成功", getLastMessage().contains("提交成功"), getLastMessage());
            }

            // ========== 4. 管理员确认 AQI 数据 ==========
            System.out.println("---- 步骤4：管理员确认该条实测 AQI 数据 ----");
            login("管理员", "admin", "123456");
            sleep(800);
            openMenu("确认AQI数据");
            sleep(800);
            fillInput("地区 / 地址 / 网格员编号", SUP_ADDRESS);
            selectOption("状态", "待确认");
            clickButtonByContainsText("查询");
            sleep(1500);
            List<WebElement> dataRows = driver.findElements(By.xpath(
                    "//div[contains(@class,'el-table__body-wrapper')]//tbody/tr"));
            // 说明：关键词已按地址过滤到本次实测数据；表格行内不展示“具体地址”（只显示 省·市），
            // 因此按网格员编码 grid001 定位行即可，AQI 等级列由 GradeTag 渲染为“三级·轻度”。
            WebElement targetRow = null;
            for (WebElement row : dataRows) {
                if (row.getText().contains("grid001")) {
                    targetRow = row;
                    break;
                }
            }
            check("检索到本次实测数据（网格员 grid001）", targetRow != null, "共 " + dataRows.size() + " 行未匹配");
            if (targetRow != null) {
                check("数据状态为“待确认”", targetRow.getText().contains("待确认"), targetRow.getText());
                check("系统计算 AQI 等级列为“轻度污染”", targetRow.getText().contains("轻度"), targetRow.getText());
                // 确认按钮直接生效（无二次确认弹窗；“退回”才有确认弹窗）
                targetRow.findElement(By.xpath(".//button[.//span[normalize-space()='确认']]")).click();
                sleep(1500);
                check("确认成功提示（纳入统计范围）",
                        getLastMessage().contains("确认") && getLastMessage().contains("统计"), getLastMessage());
                screenshot("E2E_confirm_success");
            }

            summary("端到端业务链路自动化测试（反馈→指派→实测→确认）");
        } catch (Exception e) {
            exception("链路用例执行中断", e);
        } finally {
            closeBrowser();
        }
    }
}

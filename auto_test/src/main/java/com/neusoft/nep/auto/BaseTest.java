package com.neusoft.nep.auto;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自动化测试基类（东软环保公众监督系统 NEP）
 *
 * 职责：
 *  1. Chrome 浏览器生命周期管理（默认 1280x800，支持无头模式）
 *  2. 系统登录/切换用户（系统登录态保存在浏览器 localStorage）
 *  3. Element Plus 组件通用操作：按钮、输入框、下拉框、日期区间
 *  4. 测试断言统计（通过/失败计数 + 结果输出）
 *  5. 失败截图统一保存到 auto_test/screenshots 目录
 *
 * 运行参数（JVM 参数或 IDE 运行配置 Program arguments 之外的 -D 参数）：
 *  -DbaseUrl=http://localhost:8080   被测前端地址（默认 http://localhost:8080）
 *  -Dheadless=true                   无头模式（默认有界面）
 */
public class BaseTest {

    /** 被测系统前端地址（前端 devServer 已把 /api 代理到后端 9000 端口） */
    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    /** 是否无头模式运行 */
    public static final boolean HEADLESS = Boolean.getBoolean("headless");

    /** 显式等待超时（秒），与模板保持一致取 20 秒 */
    protected static final int TIMEOUT = 20;

    protected static WebDriver driver;
    protected static WebDriverWait wait;

    /** 断言统计 */
    protected static int passCount = 0;
    protected static int failCount = 0;
    private static String suiteName = "";

    // ============================ 浏览器管理 ============================

    protected static void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,900");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--lang=zh-CN");
        if (HEADLESS) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
        }
        // Selenium Manager 会自动下载与本机 Chrome 匹配的 chromedriver，无需手工配置驱动路径
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        wait = new WebDriverWait(driver, TIMEOUT);
        System.out.println("[环境] 被测地址：" + BASE_URL + (HEADLESS ? "（无头模式）" : "（有界面模式）"));
    }

    protected static void closeBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    // ============================ 登录 / 切换用户 ============================

    /**
     * 登录系统。
     * @param roleLabel 登录页用户类型页签文字：公众监督员 / 网格员 / 管理员 / 决策者
     * @param account   登录编码（监督员为 11 位手机号，员工为登录编码如 admin/grid001）
     * @param password  密码（演示账号密码均为 123456）
     */
    protected static void login(String roleLabel, String account, String password) throws Exception {
        // 清理上一轮登录态：前端登录态保存在 localStorage，先清除再进入登录页
        driver.get(BASE_URL + "/login");
        wait.until(webDriver -> "complete".equals(
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState")));
        ((JavascriptExecutor) driver).executeScript("localStorage.clear()");
        driver.get(BASE_URL + "/login");

        switchRole(roleLabel);

        // 填写账号密码（监督员页签 placeholder 为“请输入手机号”，其余为“请输入登录编码”）
        String accountPlaceholder = "公众监督员".equals(roleLabel) ? "请输入手机号" : "请输入登录编码";
        fillInput(accountPlaceholder, account);
        fillInput("请输入密码", password);

        clickButtonByClass("login-btn");
        // 等待登录结果（成功跳转或错误提示）
        Thread.sleep(1500);
    }

    /** 点击登录页用户类型页签 */
    protected static void switchRole(String roleLabel) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'role-tab')][normalize-space(.)='" + roleLabel + "']")
        )).click();
    }

    /**
     * 退出登录（点击头像按钮 -> 确认“确定要退出登录吗？”弹窗）
     */
    protected static void logout() {
        WebElement logoutBtn = driver.findElement(By.xpath("//button[@title='退出登录']"));
        logoutBtn.click();
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(By.cssSelector(".el-message-box"))
        ));
        driver.findElement(By.cssSelector(".el-message-box__btns .el-button--primary")).click();
        Thread.sleep(800);
    }

    // ============================ Element Plus 通用操作 ============================

    /** 按 placeholder 定位输入框并输入（先清空旧值） */
    protected static void fillInput(String placeholder, String value) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='" + placeholder + "']")));
        input.clear();
        input.sendKeys(value);
    }

    /** 按 placeholder 前缀定位多行文本域并输入 */
    protected static void fillTextarea(String placeholderPrefix, String value) {
        WebElement textarea = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("textarea[placeholder^='" + placeholderPrefix + "']")));
        textarea.clear();
        textarea.sendKeys(value);
    }

    /** 按按钮 class 点击（如 login-btn / register-btn / app-btn-primary） */
    protected static void clickButtonByClass(String classFragment) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'" + classFragment + "')]")
        )).click();
    }

    /** 按按钮文字点击（Element Plus 按钮文字可能带图标，故用 normalize-space 全匹配） */
    protected static void clickButtonByText(String text) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space(.)='" + text + "']")
        )).click();
    }

    /** 按按钮文字“包含”点击（用于含图标的按钮，如“查询”“提交申请”） */
    protected static void clickButtonByContainsText(String text) {
        List<WebElement> buttons = driver.findElements(By.xpath("//button[contains(.,'" + text + "')]"));
        for (WebElement b : buttons) {
            if (b.isDisplayed() && b.isEnabled()) {
                b.click();
                return;
            }
        }
        throw new NoSuchElementException("找不到可点击的按钮：" + text);
    }

    /**
     * 操作 el-select 下拉框：点击指定 select，再点击指定文字选项。
     *
     * 兼容 Element Plus 2.4+ 的新版 DOM（空值时占位文字渲染为 span.el-select__placeholder，
     * 点击事件挂在 div.el-select__wrapper 上），定位策略按顺序尝试：
     *   1. 按 input placeholder 定位（filterable / 旧版 DOM）
     *   2. 按占位文字 span 定位（EP 2.4+ 空值 select）
     *   3. 兜底：遍历页面上所有可见 select，逐个打开并检查其下拉项是否包含目标选项
     * （Element Plus 下拉面板挂载在 body 下，故在所有已显示的 dropdown 项中按文字匹配）
     */
    protected static void selectOption(String selectPlaceholder, String optionText) throws InterruptedException {
        List<WebElement> candidates = new java.util.ArrayList<>();
        for (WebElement e : driver.findElements(By.xpath(
                "//div[contains(@class,'el-select') and .//input[@placeholder='" + selectPlaceholder + "']]"))) {
            candidates.add(e);
        }
        for (WebElement e : driver.findElements(By.xpath(
                "//div[contains(@class,'el-select') and .//span[contains(@class,'el-select__placeholder')]"
                        + " and .//span[contains(@class,'el-select__placeholder')][normalize-space(.)='" + selectPlaceholder + "']]"))) {
            if (!candidates.contains(e)) {
                candidates.add(e);
            }
        }
        for (WebElement sel : candidates) {
            if (!sel.isDisplayed()) {
                continue;
            }
            sel.click();
            sleep(400);
            WebElement opt = findVisibleDropdownItem(optionText);
            if (opt != null) {
                opt.click();
                return;
            }
            closeOpenDropdown();
        }
        // 兜底：遍历所有可见 select（select 中已选值替换占位文字时仍可按选项内容命中）
        for (WebElement sel : visibleSelects()) {
            if (candidates.contains(sel)) {
                continue;
            }
            sel.click();
            sleep(400);
            WebElement opt = findVisibleDropdownItem(optionText);
            if (opt != null) {
                opt.click();
                return;
            }
            closeOpenDropdown();
        }
        throw new NoSuchElementException("下拉框【" + selectPlaceholder + "】找不到选项：" + optionText);
    }

    /** 在指定 CSS 作用域内（如 .measure-item 列表）操作第 index 个 el-select */
    protected static void selectOptionInScope(String scopeCss, int index, String optionText) {
        List<WebElement> selects = driver.findElements(By.cssSelector(scopeCss + " .el-select__wrapper"));
        if (selects.size() <= index) {
            selects = driver.findElements(By.cssSelector(scopeCss + " .el-select"));
        }
        if (selects.size() <= index) {
            throw new NoSuchElementException("作用域 " + scopeCss + " 内找不到第 " + (index + 1) + " 个下拉框");
        }
        selects.get(index).click();
        sleep(400);
        WebElement opt = findVisibleDropdownItem(optionText);
        if (opt == null) {
            throw new NoSuchElementException("下拉框找不到选项：" + optionText);
        }
        opt.click();
    }

    /** 在元素范围内定位 el-select 的可点击层（EP 2.4+ 的 wrapper，兼容旧版根节点） */
    protected static WebElement selectClickable(WebElement scope) {
        List<WebElement> wraps = scope.findElements(By.cssSelector(".el-select__wrapper"));
        if (!wraps.isEmpty()) {
            return wraps.get(0);
        }
        List<WebElement> roots = scope.findElements(By.cssSelector(".el-select"));
        if (!roots.isEmpty()) {
            return roots.get(0);
        }
        throw new NoSuchElementException("范围内未找到 el-select");
    }

    /** 页面上所有可见的可点击 el-select（EP 2.4+ 优先 wrapper 层） */
    protected static List<WebElement> visibleSelects() {
        List<WebElement> out = new java.util.ArrayList<>();
        for (WebElement w : driver.findElements(By.cssSelector(".el-select__wrapper"))) {
            if (w.isDisplayed()) {
                out.add(w);
            }
        }
        if (out.isEmpty()) {
            for (WebElement s : driver.findElements(By.cssSelector(".el-select"))) {
                if (s.isDisplayed()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /** 在所有已显示的下拉项中查找包含指定文字的选项，找不到返回 null */
    protected static WebElement findVisibleDropdownItem(String optionText) {
        for (WebElement item : driver.findElements(By.cssSelector(".el-select-dropdown__item"))) {
            if (item.isDisplayed() && item.getText().contains(optionText)) {
                return item;
            }
        }
        return null;
    }

    /** 关闭已打开的下拉面板（点击页面空白处） */
    protected static void closeOpenDropdown() {
        driver.findElement(By.tagName("body")).click();
        sleep(300);
    }

    /**
     * 操作 el-date-picker 日期区间：在起止输入框中键入日期并回车确认
     */
    protected static void fillDateRange(String startPlaceholder, String endPlaceholder,
                                        String startDate, String endDate) {
        WebElement startInput = driver.findElement(By.xpath("//input[@placeholder='" + startPlaceholder + "']"));
        startInput.clear();
        startInput.sendKeys(startDate);
        startInput.sendKeys(Keys.ENTER);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ignored) {
        }
        WebElement endInput = driver.findElement(By.xpath("//input[@placeholder='" + endPlaceholder + "']"));
        endInput.clear();
        endInput.sendKeys(endDate);
        endInput.sendKeys(Keys.ENTER);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * 点击左侧/顶部菜单（按菜单文字）。
     * 菜单项为 router-link 渲染的 a 元素，class 为 nav-item（移动端布局）或 menu-item（后台布局）。
     */
    protected static void openMenu(String menuTitle) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'nav-item') or contains(@class,'menu-item')]//span[normalize-space(.)='" + menuTitle + "']")
        )).click();
        sleep(800);
    }

    // ============================ 提示信息 ============================

    /** 获取最近一条 el-message 提示文字（成功/警告/错误） */
    protected static String getLastMessage() {
        try {
            List<WebElement> messages = driver.findElements(By.cssSelector(".el-message__content"));
            for (int i = messages.size() - 1; i >= 0; i--) {
                WebElement m = messages.get(i);
                if (m.isDisplayed()) {
                    return m.getText();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** 获取最近一条 el-message-box（确认弹窗）中的按钮文字集合 */
    protected static void confirmMessageBox() {
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector(".el-message-box"))));
        driver.findElement(By.cssSelector(".el-message-box__btns .el-button--primary")).click();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
    }

    // ============================ 断言统计 ============================

    /**
     * 断言并统计结果
     * @param name    用例步骤名称
     * @param cond    是否满足预期
     * @param detail  失败时的补充信息（当前实际值）
     */
    protected static void check(String name, boolean cond, String detail) {
        if (cond) {
            passCount++;
            System.out.println("  [通过] " + name);
        } else {
            failCount++;
            System.out.println("  [失败] " + name + "  实际：" + detail);
            try {
                screenshot("FAIL_" + suiteName + "_" + name);
            } catch (Exception ignored) {
            }
        }
    }

    protected static void setSuiteName(String name) {
        suiteName = name;
    }

    protected static void summary(String suite) {
        System.out.println("==================================================");
        System.out.println("【" + suite + "】执行结束：通过 " + passCount + " 项，失败 " + failCount + " 项");
        System.out.println("==================================================");
    }

    // ============================ 截图 ============================

    /** 截图保存到 auto_test/screenshots（按用例名+时间戳命名） */
    protected static void screenshot(String actionName) {
        try {
            File dir = new File("screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dest = new File(dir, actionName + "_" + timestamp + ".png");
            Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("  [截图] " + dest.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("  [截图失败] " + e.getMessage());
        }
    }

    /** 休眠小工具（避免大量 Thread.sleep 的受检异常样板代码） */
    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}

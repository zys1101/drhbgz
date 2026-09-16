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
import java.time.Duration;

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

    /** 结果文件写入器（test-results.tsv，UTF-8，便于脚本解析） */
    private static java.io.PrintWriter resultWriter;

    /** 最近一次登录产生的提示（ElMessage 约 3 秒后自动消失，故在登录返回前先记住） */
    private static String lastAuthMessage = "";

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
        // 可选：指定浏览器可执行文件（本机 Chrome 版本与驱动不匹配、或需使用自带 Chromium 时）
        // 用法：-DchromeBinary="C:/path/to/chrome.exe"；Selenium Manager 会据此匹配对应驱动版本
        String chromeBinary = System.getProperty("chromeBinary", "");
        if (!chromeBinary.isEmpty()) {
            options.setBinary(chromeBinary);
            System.out.println("[环境] 使用指定浏览器：" + chromeBinary);
        }
        // Selenium Manager 会自动下载与浏览器匹配的 chromedriver，无需手工配置驱动路径
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT));
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

        // 填写账号密码（监督员页签 placeholder 为“请输入手机号”，其余为“请输入登录编码”）
        String accountPlaceholder = "公众监督员".equals(roleLabel) ? "请输入手机号" : "请输入登录编码";

        // 角色页签切换后，账号输入框的 placeholder 会随之改变，可用它确认“切换已生效”。
        // 页签点击偶发未生效，因此点击后校验 active 状态，未生效时重试。
        switchRole(roleLabel);
        for (int attempt = 0; attempt < 3 && !isRoleActive(roleLabel); attempt++) {
            switchRole(roleLabel);
        }
        if (!placeholderAppears(accountPlaceholder, TIMEOUT)) {
            StringBuilder sb = new StringBuilder();
            for (WebElement e : driver.findElements(By.tagName("input"))) {
                sb.append("[").append(e.getAttribute("placeholder"))
                        .append(" visible=").append(e.isDisplayed()).append("]");
            }
            String activeTab = "";
            List<WebElement> actives = driver.findElements(By.cssSelector("button.role-tab.active"));
            if (!actives.isEmpty()) {
                activeTab = actives.get(0).getText();
            }
            throw new IllegalStateException("切换用户类型【" + roleLabel + "】后未出现账号输入框（"
                    + accountPlaceholder + "）；URL=" + driver.getCurrentUrl()
                    + "；当前激活页签=" + activeTab
                    + "；页面 input=" + sb);
        }

        fillInput(accountPlaceholder, account);
        fillInput("请输入密码", password);

        // 登录按钮：原生点击在本环境偶发“已送达但无响应”，点击后按“是否离开登录页 / 是否出现提示”
        // 判断是否生效，未生效则回退 JS 点击。
        WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'login-btn')]")));
        loginBtn.click();
        sleep(1500);
        if (driver.getCurrentUrl().contains("/login") && getLastMessage().isEmpty()) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
            sleep(1500);
        }
        if (driver.getCurrentUrl().contains("/login") && getLastMessage().isEmpty()) {
            writeResult("PAGE", "登录点击未生效",
                    "role=" + roleLabel + " account=" + account
                            + " url=" + driver.getCurrentUrl()
                            + " btn=" + loginBtn.getText() + " enabled=" + loginBtn.isEnabled()
                            + " accountValue=" + driver.findElement(
                                    By.xpath("//input[@placeholder='" + accountPlaceholder + "']")).getAttribute("value"));
        }
        // 等待登录结果（成功跳转或错误提示）
        Thread.sleep(500);

        // 记住本次登录提示：ElMessage 约 3 秒后自动消失，用例稍后再读就取不到了，
        // 因此在这里轮询记录，供 getLastAuthMessage() 使用。
        lastAuthMessage = "";
        for (int i = 0; i < 8; i++) {
            String m = getLastMessage();
            if (!m.isEmpty()) {
                lastAuthMessage = m;
            }
            sleep(200);
        }
    }

    /** 最近一次登录产生的提示（即使 ElMessage 已经自动消失也能取到） */
    protected static String getLastAuthMessage() {
        return lastAuthMessage == null ? "" : lastAuthMessage;
    }

    /** 在给定秒数内等待某 placeholder 的输入框出现，用于判断登录页角色切换是否生效 */
    private static boolean placeholderAppears(String placeholder, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//input[@placeholder='" + placeholder + "']")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * 点击登录页用户类型页签，并确认切换已生效。
     *
     * Selenium 的原生 click 在这种纯 CSS 页签上偶发“点击已送达但 Vue 未响应”
     * （表现为 active 页签仍是原角色，进而找不到另一个 placeholder 的账号输入框），
     * 因此点击后校验 active 状态，未生效则回退为 JS 点击。
     */
    protected static void switchRole(String roleLabel) {
        WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'role-tab')][normalize-space(.)='" + roleLabel + "']")));
        tab.click();
        sleep(400);
        if (!isRoleActive(roleLabel)) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", tab);
            sleep(400);
        }
    }

    /** 判断某个用户类型页签是否处于选中（active）状态 */
    protected static boolean isRoleActive(String roleLabel) {
        for (WebElement t : driver.findElements(By.cssSelector("button.role-tab.active"))) {
            if (t.getText().trim().equals(roleLabel)) {
                return true;
            }
        }
        return false;
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
        sleep(800);
    }

    // ============================ Element Plus 通用操作 ============================

    /** 按 placeholder 定位输入框并输入（先清空旧值，并回读校验是否真的写进去了） */
    protected static void fillInput(String placeholder, String value) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='" + placeholder + "']")));
        clearInput(input);
        input.sendKeys(value);
        String got = input.getAttribute("value");
        if (got == null || !got.contains(value)) {
            // 本环境下 Selenium 的 sendKeys 在页面跳转后偶发“无输入效果”（元素存在但不接收按键），
            // 回退为 JS 设值并派发 input 事件，保证 Vue 的 v-model 同步更新。
            writeResult("PAGE", "输入未生效（已回退 JS 赋值）",
                    "placeholder=" + placeholder + " 期望=" + value + " 实际=" + got
                            + " enabled=" + input.isEnabled());
            setInputValue(input, value);
        }
    }

    /**
     * 清空输入框。
     * 注意：若用 JS 直接改 value 而不派发 input 事件，Vue 的 v-model 不会同步，
     * 会出现“输入框看起来有值、但提交时校验说为空”的假象，因此 JS 兜底必须一并派发 input 事件。
     */
    protected static void clearInput(WebElement input) {
        try {
            input.clear();
            String v = input.getAttribute("value");
            if (v != null && !v.isEmpty()) {
                ((JavascriptExecutor) driver).executeScript(
                        "var e = arguments[0]; e.value = '';"
                                + "e.dispatchEvent(new Event('input', { bubbles: true }));",
                        input);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 点击当前已展开的 el-select 下拉面板中包含指定文字的选项。
     * 带显式等待：点击 select 到面板渲染出选项之间存在延迟，立刻查询会误判为“选项不存在”。
     */
    protected static void clickDropdownItem(String optionText) {
        WebElement item = wait.until(d -> {
            for (WebElement e : d.findElements(By.cssSelector(".el-select-dropdown__item"))) {
                if (e.isDisplayed() && e.getText().contains(optionText)) {
                    return e;
                }
            }
            return null;
        });
        item.click();
        sleep(300);
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

    // ============ 交互可靠性：点击/输入“校验 + JS 兜底” ============
    // 本环境下 Selenium 的原生点击/键入偶发“已送达但页面无任何反应”（元素存在、可点击、
    // 不抛异常，但 Vue 未收到事件），表现为：下拉框打不开、按钮点了不跳转、输入框看似有值
    // 而提交时报空。以下封装在“原生操作未产生预期效果”时统一回退为 JS 触发。

    /** 是否有 el-select 下拉面板处于展开状态 */
    protected static boolean isDropdownOpen() {
        for (WebElement e : driver.findElements(By.cssSelector(".el-select-dropdown__item"))) {
            if (e.isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    /** 点击元素；若预期效果未出现则回退 JS 点击，并记录现场 */
    protected static void clickVerified(WebElement el, java.util.function.BooleanSupplier effect, String what) {
        try {
            el.click();
        } catch (Exception ignored) {
        }
        sleep(600);
        if (effect.getAsBoolean()) {
            return;
        }
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        } catch (Exception ignored) {
        }
        sleep(800);
        if (!effect.getAsBoolean()) {
            writeResult("PAGE", "点击未生效", what + " url=" + driver.getCurrentUrl());
        }
    }

    /** 打开 el-select 下拉面板（带 JS 兜底） */
    protected static void openSelect(WebElement selectRoot) {
        clickVerified(selectClickable(selectRoot), BaseTest::isDropdownOpen, "打开下拉框");
    }

    /** 在已展开的下拉面板中选择选项，并校验 select 已显示所选文字 */
    protected static void chooseDropdownItem(String optionText, WebElement selectRoot) {
        WebElement item = wait.until(d -> {
            for (WebElement e : d.findElements(By.cssSelector(".el-select-dropdown__item"))) {
                if (e.isDisplayed() && e.getText().contains(optionText)) {
                    return e;
                }
            }
            return null;
        });
        item.click();
        sleep(400);
        if (!selectShowsText(selectRoot, optionText)) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", item);
            sleep(400);
        }
    }

    /** select 当前显示的文字是否包含指定内容 */
    protected static boolean selectShowsText(WebElement selectRoot, String text) {
        try {
            return selectRoot.getText().contains(text);
        } catch (Exception e) {
            return false;
        }
    }

    /** 尝试在某个 select 上选择指定选项：打开→找到则选，未找到则收起并返回 false */
    private static boolean tryChoose(WebElement sel, String optionText) {
        openSelect(sel);
        if (findVisibleDropdownItem(optionText) == null) {
            closeDropdownBy(sel);
            return false;
        }
        chooseDropdownItem(optionText, sel);
        return true;
    }

    /**
     * 操作 el-select 下拉框：点击指定 select，再点击指定文字选项。
     *
     * 兼容 Element Plus 2.4+ 的新版 DOM（空值时占位文字渲染为 span.el-select__placeholder，
     * 点击事件挂在 div.el-select__wrapper 上），定位策略按顺序尝试：
     *   1. 按 input placeholder 定位（filterable / 旧版 DOM）
     *   2. 按占位文字节点定位（EP 2.14 实际渲染为 div.el-select__placeholder，兼容 span）
     *   3. 兜底：遍历页面上所有可见 select，逐个打开并检查其下拉项是否包含目标选项
     * （Element Plus 下拉面板挂载在 body 下，故在所有已显示的 dropdown 项中按文字匹配）
     *
     * 注意：关闭误开的下拉框时**再次点击该 select 自身**，不能点击 body——
     * el-dialog 默认 close-on-click-modal，点 body 会直接把弹窗关掉。
     */
    protected static void selectOption(String selectPlaceholder, String optionText) throws InterruptedException {
        List<WebElement> candidates = new java.util.ArrayList<>();
        for (WebElement e : driver.findElements(By.xpath(
                "//div[contains(@class,'el-select') and .//input[@placeholder='" + selectPlaceholder + "']]"))) {
            candidates.add(e);
        }
        // EP 2.14：占位文字在 div.el-select__selected-item.el-select__placeholder 中（旧版为 span），故不限定标签名
        for (WebElement e : driver.findElements(By.xpath(
                "//div[contains(@class,'el-select') and .//*[contains(@class,'el-select__placeholder')]"
                        + " and .//*[contains(@class,'el-select__placeholder')][normalize-space(.)='"
                        + selectPlaceholder + "']]"))) {
            if (!candidates.contains(e)) {
                candidates.add(e);
            }
        }
        for (WebElement sel : candidates) {
            if (!sel.isDisplayed()) {
                continue;
            }
            if (tryChoose(sel, optionText)) {
                return;
            }
        }
        // 兜底：遍历所有可见 select（select 中已选值替换占位文字时仍可按选项内容命中）
        for (WebElement sel : visibleSelects()) {
            if (candidates.contains(sel)) {
                continue;
            }
            if (tryChoose(sel, optionText)) {
                return;
            }
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
        WebElement sel = selects.get(index);
        openSelect(sel);
        if (findVisibleDropdownItem(optionText) == null) {
            throw new NoSuchElementException("下拉框找不到选项：" + optionText);
        }
        chooseDropdownItem(optionText, sel);
    }

    /** 在元素范围内定位 el-select 的可点击层（EP 2.4+ 的 wrapper，兼容旧版根节点） */
    protected static WebElement selectClickable(WebElement scope) {
        // 允许直接把 wrapper 传进来
        String cls = scope.getAttribute("class");
        if (cls != null && cls.contains("el-select__wrapper")) {
            return scope;
        }
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

    /**
     * 关闭误开的下拉面板：再次点击该 select 自身即可收起。
     * 不要点 body —— el-dialog 默认 close-on-click-modal，点 body 会把弹窗一起关掉。
     */
    protected static void closeDropdownBy(WebElement selectElement) {
        try {
            selectElement.click();
        } catch (Exception ignored) {
        }
        sleep(250);
    }

    /** 关闭已打开的下拉面板（点击页面空白处，仅用于非弹窗场景） */
    protected static void closeOpenDropdown() {
        driver.findElement(By.tagName("body")).click();
        sleep(300);
    }

    /**
     * 操作 el-date-picker 日期区间（type=daterange）。
     *
     * 实测（Element Plus 2.14）：
     *  - 直接用 sendKeys 逐字键入是不可靠的：键入“开始日期”后焦点不会移到结束框，
     *    第二次键入会拼到同一个输入框里（值变成 2026-09-212026-09-22），v-model 仍为 null，
     *    页面提交时报“请选择请假期间”；
     *  - 可靠做法：先点击输入框打开面板，再用 JS 设置输入框的值并派发 input 事件
     *    （让 EP 解析该日期），最后回车确认。两个输入框分别处理后 v-model 正确提交。
     */
    protected static void fillDateRange(String startPlaceholder, String endPlaceholder,
                                        String startDate, String endDate) {
        WebElement startInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@placeholder='" + startPlaceholder + "']")));
        startInput.click();                 // 打开日期面板（不打开面板时输入不会被提交）
        sleep(500);
        setInputValue(startInput, startDate);
        sleep(200);
        startInput.sendKeys(Keys.ENTER);    // 确认开始日期
        sleep(500);

        WebElement endInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@placeholder='" + endPlaceholder + "']")));
        setInputValue(endInput, endDate);
        sleep(200);
        endInput.sendKeys(Keys.ENTER);      // 确认结束日期，面板收起并提交 v-model
        sleep(600);
    }

    /**
     * 用 JS 给输入框赋值并派发 input 事件。
     * 用于 Element Plus 日期框等“由 input 事件驱动解析”的组件，
     * 比 Selenium 真实逐字键入更稳定。
     */
    protected static void setInputValue(WebElement input, String value) {
        ((JavascriptExecutor) driver).executeScript(
                "var e = arguments[0], v = arguments[1];"
                        + "e.focus(); e.value = v;"
                        + "e.dispatchEvent(new Event('input', { bubbles: true }));",
                input, value);
    }

    /**
     * 点击左侧/顶部菜单（按菜单文字）。
     * 菜单项为 router-link 渲染的 a 元素，class 为 nav-item（移动端布局）或 menu-item（后台布局）。
     */
    protected static void openMenu(String menuTitle) {
        WebElement span = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'nav-item') or contains(@class,'menu-item')]//span[normalize-space(.)='"
                        + menuTitle + "']")));
        // 点击事件在 <a> 上，点 span 有时不冒泡生效；直接点 <a> 并校验路由已切换
        WebElement anchor = span.findElement(By.xpath("./ancestor::a[1]"));
        String before = driver.getCurrentUrl();
        clickVerified(anchor, () -> !driver.getCurrentUrl().equals(before), "打开菜单 " + menuTitle);
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
            writeResult("PASS", name, "");
        } else {
            failCount++;
            System.out.println("  [失败] " + name + "  实际：" + detail);
            writeResult("FAIL", name, detail);
            try {
                screenshot("FAIL_" + suiteName + "_" + name);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 记录一次“用例中断/未预期异常”的失败项。
     * 用例主体若抛出异常会直接结束，若不记录，结果文件里就只剩半截 PASS，
     * 无法判断失败原因，故在 finally 前统一调用本方法。
     */
    protected static void exception(String name, Throwable t) {
        failCount++;
        String msg = String.valueOf(t);
        System.out.println("  [失败] " + name + "  实际：" + msg);
        writeResult("FAIL", name, msg);
        // 中断类失败没有断言现场，这里补充截图与页面快照，便于定位
        try {
            screenshot("FAIL_" + suiteName + "_" + name);
        } catch (Exception ignored) {
        }
        try {
            String body = driver.findElement(By.tagName("body")).getText().replace('\n', ' ');
            writeResult("PAGE", name, driver.getCurrentUrl() + " || " + body);
        } catch (Exception ignored) {
        }
    }

    protected static void setSuiteName(String name) {
        suiteName = name;
        // 每次进入新用例时重置统计，保证多用例在同一 JVM 内顺序执行（RunAllTest）时
        // 各用例的“通过/失败”汇总互不串扰
        passCount = 0;
        failCount = 0;
        writeResult("SUITE", name, "");
    }

    protected static void summary(String suite) {
        System.out.println("==================================================");
        System.out.println("【" + suite + "】执行结束：通过 " + passCount + " 项，失败 " + failCount + " 项");
        System.out.println("==================================================");
        writeResult("SUMMARY", suite, "pass=" + passCount + " fail=" + failCount);
    }

    // ==================== 结果文件（UTF-8，便于机器解析） ====================

    /**
     * 逐条把断言结果写入 test-results.tsv（UTF-8）。
     * 控制台在部分环境（Windows 中文 GBK 控制台 + PowerShell 重定向）下会乱码，
     * 该文件保证结果可被脚本稳定读取。
     */
    private static synchronized void writeResult(String type, String name, String detail) {
        try {
            if (resultWriter == null) {
                resultWriter = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream("test-results.tsv", true),
                        java.nio.charset.StandardCharsets.UTF_8), true);
                // 每次 JVM 运行写入分隔头，便于脚本只取最近一次运行的结果
                resultWriter.println("RUN\t" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                        + "\t" + BASE_URL + (HEADLESS ? "\theadless" : "\theaded"));
            }
            resultWriter.println(type + "\t" + name + "\t" + (detail == null ? "" : detail.replace('\t', ' ').replace('\n', ' ')));
        } catch (Exception ignored) {
        }
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

    // ============ 通过页面上下文调用后端接口 ============
    // 用例需要“按后端真实数据”决定操作对象（例如本地指派要求网格员与反馈同区域），
    // 直接读接口比在页面上猜更可靠。这里借助页面同源 fetch + 前端 /api 代理取 JSON。

    /** 在页面上下文同步等待 fetch 完成并返回响应文本（跨域/CORS 由同源代理规避） */
    protected static String apiGetRaw(String path) {
        Object r = ((JavascriptExecutor) driver).executeAsyncScript(
                "var cb = arguments[arguments.length - 1];"
                        + "fetch(arguments[0], {headers: {'Accept': 'application/json'}})"
                        + "  .then(function (res) { return res.text(); })"
                        + "  .then(function (t) { cb(t); })"
                        + "  .catch(function (e) { cb(''); });",
                path);
        return r == null ? "" : String.valueOf(r);
    }

    /** 调用 GET 接口并按 ResultVO 结构解析为 Map（需页面已加载被测系统，保证同源） */
    protected static java.util.Map<String, Object> apiGet(String path) {
        String text = apiGetRaw(path);
        if (text == null || text.isEmpty()) {
            throw new IllegalStateException("接口无响应：" + path + "（页面是否已打开被测系统？）");
        }
        return new org.openqa.selenium.json.Json().toType(text, java.util.Map.class);
    }

    /** 调用 GET 接口并返回 data 数组（元素为 Map） */
    @SuppressWarnings("unchecked")
    protected static List<java.util.Map<String, Object>> apiList(String path) {
        Object data = apiGet(path).get("data");
        return data instanceof List ? (List<java.util.Map<String, Object>>) data : new java.util.ArrayList<>();
    }
}

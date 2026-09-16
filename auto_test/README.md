# NEP 东软环保公众监督系统 · 自动化测试工程

基于 **Java 17 + Selenium 4 + Chrome** 的 Web 端 UI 自动化测试，与
《doc/自动化测试报告-东软环保公众监督系统.md》中的关键代码一一对应。

## 目录结构

```
auto_test/
├── pom.xml                          # Maven 工程（仅依赖 selenium-java，驱动由 Selenium Manager 自动下载）
├── src/main/java/com/neusoft/nep/auto/
│   ├── BaseTest.java                # 基类：浏览器管理 / 登录 / Element Plus 组件操作 / 断言统计 / 截图
│   ├── LoginTest.java               # 用例1：4 类角色登录（成功+失败+空输入+未登录拦截）
│   ├── SupervisorFeedbackTest.java  # 用例2：监督员注册 + 提交监督反馈 + 历史反馈核对
│   ├── FeedbackAssignTest.java      # 用例3：管理员反馈查询 / 指派网格员 / 删除反馈
│   ├── GridMeasureTest.java         # 用例4：端到端链路 反馈→指派→实测录入→确认AQI数据
│   └── HrGridTest.java              # 用例5：网格员增改 + 请假申请/审批 + 请假状态联动
└── screenshots/                     # 运行后自动生成的截图（按 用例名_时间戳 命名）
```

## 运行前提

1. **被测系统已启动**（前后端均可用）：
   - 前端：`cd front && npm install && npm run serve` → http://localhost:8080
     （devServer 已将 `/api` 代理到后端 9000 端口）
   - 后端：IDEA 运行 `backend/demo` 的 `DemoApplication`（MySQL 需导入 `sql/nep_system.sql`）
2. **本机已安装 Chrome 浏览器**（chromedriver 由 Selenium Manager 自动匹配下载，无需手工配置）。
3. Java 17 + Maven 3.6+（IDEA 中直接打开 `auto_test` 作为 Maven 工程导入）。

## 运行方式

### 方式一：IDEA（推荐）

直接运行各测试类的 `main` 方法即可。如需无头模式或在其他端口启动前端，
在 Run Configuration 的 VM options 中添加：

```
-Dheadless=true -DbaseUrl=http://localhost:8080
```

### 方式二：命令行

```bash
cd auto_test
mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.LoginTest
mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.SupervisorFeedbackTest
mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.FeedbackAssignTest
mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.GridMeasureTest
mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.HrGridTest
```

### 方式三：一键运行全部用例

```bash
cd auto_test
mvn -q compile exec:java -Dexec.mainClass=com.neusoft.nep.auto.RunAllTest
```

依次执行 5 个用例并累计失败项；单个用例运行异常不会中断其余用例。

### 附：接口层核对（无需浏览器，可离线运行）

`tools/api_flow_check.py` 用标准库直接调用后端接口，逐项核对 5 个 UI 用例所依赖的
业务流程与提示文案（登录矩阵、注册、提交反馈、指派/删除、反馈→指派→实测→确认、人员管理+请假审批+状态联动）：

```bash
python auto_test/tools/api_flow_check.py     # 需后端已在 9000 端口运行
```

适合在**无法启动浏览器/无外网下载驱动**的环境下先行确认"后端行为与用例预期一致"。

## 执行顺序建议

| 顺序 | 用例 | 说明 |
| --- | --- | --- |
| 1 | LoginTest | 不产生数据变更，可反复执行 |
| 2 | SupervisorFeedbackTest | 注册新监督员并产生一条“待指派”反馈 |
| 3 | FeedbackAssignTest | 依赖“待指派”反馈（用例2 或种子数据均可提供） |
| 4 | GridMeasureTest | 自包含：自己产生反馈并走完全链路 |
| 5 | HrGridTest | 自包含：新建账号→请假→审批→验证禁用 |

## 演示账号（密码均为 123456）

| 角色 | 账号 | 说明 |
| --- | --- | --- |
| 公众监督员 | 13800001111 / 13800002222 / 13800004444 | 已绑定网格地址 |
| 网格员 | grid001 ~ grid004、grid006 ~ grid010 | grid005 为非工作（请假中） |
| 管理员 | admin | |
| 决策者 | viewer | 仅可视化大屏 |

## 结果说明

- 控制台逐项输出 `[通过]` / `[失败]`，失败时自动截图到 `screenshots/`；
- 每个用例结束时输出该用例的通过/失败统计；
- **同时写入 `test-results.tsv`（UTF-8，制表符分隔）**，便于脚本解析与留档：
  `RUN`（运行头）/ `SUITE`（用例开始）/ `PASS`/`FAIL`（逐条断言，含实际值）/ `PAGE`（点击或输入未生效时的现场）/ `SUMMARY`（通过失败汇总）。
  中文控制台在部分环境（Windows GBK 控制台 + 重定向）会乱码，该文件不受影响。
- 自动化断言基于页面提示（ElMessage）+ URL 跳转 + 表格数据三重校验，
  与《软件测试报告》中的手工用例编号相互印证。

## 交互可靠性说明（Element Plus 2.14 + 本机环境实测）

基类对以下三类“原生操作已送达但页面无反应”的情况做了**校验 + JS 兜底**，
避免用例随机失败（这些坑均已在真实浏览器中复现并修正）：

1. **点击**：角色页签、菜单项、卡片、按钮偶发点了不生效 → 点击后校验预期效果（active 状态 / URL 变化 / 弹窗出现），未生效则回退 JS 点击；
2. **输入**：页面跳转后 `sendKeys` 偶发无输入效果 → 写入后回读 `value`，不一致则用 JS 赋值并派发 `input` 事件（保证 v-model 同步）；
3. **下拉框**：EP 2.14 的占位文字是 `div.el-select__placeholder`（不是 `span`），且展开面板需等待渲染；收起误开的面板必须再次点击该 select 自身，**不能点 body**（`el-dialog` 默认 `close-on-click-modal`，点 body 会把弹窗关掉）。

另外：`el-date-picker`（daterange）不能用 `sendKeys` 逐字键入（焦点不切换，值会拼接成一个输入框），
必须先点开面板、再对起止输入框分别赋值并回车；登录提示 `ElMessage` 约 3 秒自动消失，
基类在登录返回前先记录（`getLastAuthMessage()`），避免断言读不到提示。


## 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| `mvn compile` 报 `未报告的异常错误 java.lang.InterruptedException` | 基类中误用了原生 `Thread.sleep`；统一改用 `BaseTest.sleep()`（本仓库已修复） |
| 运行时报 `NoSuchSessionException` / 一直卡在打开浏览器 | Selenium Manager 需联网下载与本机 Chrome 版本匹配的 chromedriver；离线环境请自备同版本驱动并指定 `-Dwebdriver.chrome.driver=<路径>` |
| 提示 `chromedriver` 版本与浏览器不匹配 | 升级/降级驱动，或在 `pom.xml` 中调整 `selenium.version` 后重新编译 |
| 用例全部失败且提示网络异常 | 后端未启动：前端 8080 仅做 `/api` 代理，需先启动 `backend/demo`（或 `python preview/nep_preview_server.py`） |
| 无外网、也无法装浏览器驱动 | 先用 `tools/api_flow_check.py` 在接口层核对业务行为，再在具备驱动的机器上执行 UI 用例 |


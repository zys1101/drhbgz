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
- 自动化断言基于页面提示（ElMessage）+ URL 跳转 + 表格数据三重校验，
  与《软件测试报告》中的手工用例编号相互印证。

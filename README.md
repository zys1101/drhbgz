# 东软环保公众监督系统（NEP）

依据《东软环保公众监督系统需求说明书 1.0.0-0.0.0》实现的完整系统：
汇总公众监督员的空气质量反馈 → 系统管理员指派网格员实地检测 → 实测 AQI 数据确认入库 →
面向决策者的五项统计与可视化大屏。地区采用网格化管理，最小网格单位为大城市（2022 年发布的 106 个大城市名单）。

## 技术栈（对应需求 2.1 开发环境）

| 分层 | 技术 |
| --- | --- |
| 数据库 | MySQL（脚本见 `sql/nep_system.sql`，可用 Navicat 直接导入） |
| 后端 | SpringBoot 3.2.8 + MyBatis-Plus 3.5.12 + springdoc-openapi（Maven 工程，位于 `backend/demo`） |
| 前端 | Vue3 + Vue CLI + Axios + Element Plus + Font Awesome + ECharts（工程位于 `front/`） |

前端采用统一的“生态绿”设计系统（`front/src/assets/theme.css` 定制 Element Plus 主题变量）：
深绿渐变侧边栏 + Font Awesome 图标，登录/注册为左右分屏品牌页，表格/表单/弹窗/消息提示
全部基于 Element Plus 组件，AQI 等级与任务状态使用统一彩色标签（`GradeTag` / `StateTag`）。

## 目录结构

```
├── sql/
│   ├── nep_system.sql        # MySQL 建库脚本（结构 + 34省/106大城市/AQI级别表/演示数据）
│   ├── seed_data.json        # 种子数据（单一数据源）
│   └── generate_seed.py      # 种子生成器：python3 sql/generate_seed.py 重新生成上面两个文件
├── backend/demo/             # SpringBoot 后端（正式后端，端口 9000，上下文 /api）
├── front/                    # Vue3 前端（devServer 端口 8080，/api 代理到 9000）
├── auto_test/                # UI 自动化测试工程（Java 17 + Selenium 4 + Chrome，见 README.md）
├── doc/                      # 测试文档（软件测试报告 / 自动化测试报告，.md + .docx）
└── preview/
    └── nep_preview_server.py # 沙箱在线预览服务（Python 标准库 + SQLite，接口与后端完全一致）
```

## 升级注意（已部署过的旧环境）

旧库升级到人员管理模块：若你之前导入过早期版本的 `sql/nep_system.sql`（无 `leave` 请假表），
进入“人员管理-网格员管理”会因 `/api/leave/list` 查询失败而提示“操作失败”。修复方式二选一：

1. **增量升级（推荐，保留现有数据）**：对 `nep_system` 库执行 `sql/hr_upgrade.sql`（幂等补建 `leave` 表并载入示例请假），然后重新编译并重启后端。
2. **整体重导**：重新导入最新的 `sql/nep_system.sql`（会重建库，含 leave 表与种子，注意清空现有演示数据）。

无论哪种方式，请确保后端已重新编译（IDEA `mvn clean package` 或重新 Run）后再访问；
若使用预览服务（preview），重启 `python preview/nep_preview_server.py` 即可自动幂等补表。

## 快速开始（本机 IDEA / VSCode 环境）

### 1. 初始化数据库
- 安装 MySQL，使用 Navicat（或 mysql 命令行）执行 `sql/nep_system.sql`。
- 脚本会自动创建 `nep_system` 库、7 张表以及演示数据（近 12 个月反馈 + 实测数据）。

### 2. 启动后端
- 用 IDEA 打开 `backend/demo`，等待 Maven 依赖下载完成，运行 `DemoApplication`。
- 数据库连接配置在 `backend/demo/src/main/resources/application.yaml`（默认 root / As.123456，请按需修改）。
- 验证：浏览器打开 http://localhost:9000/api/swagger-ui.html 查看全部接口文档。

### 3. 启动前端
```bash
cd front
npm install       # 若国内网络慢可先: npm config set registry https://registry.npmmirror.com
npm run serve
```
- 访问 http://localhost:8080 （devServer 已配置 /api 代理到 http://127.0.0.1:9000）。

### 4. 仅在线预览（无法运行 SpringBoot 的环境）
```bash
cd front && npm run build   # 先构建前端
cd .. && python3 preview/nep_preview_server.py
```
- 预览服务监听 9000，**同时托管前端页面（front/dist）与 /api 接口**，
  打开 http://localhost:9000 即可体验完整系统（同源，无需 8080 dev server）。
- 首次启动自动建 SQLite 库（preview/nep_preview.db）并载入种子数据。

## 演示账号（密码均为 123456）

| 端 | 角色 | 账号 | 说明 |
| --- | --- | --- | --- |
| NEPS | 公众监督员 | 13800001111（张伟） | 也可自行注册，手机号即身份唯一识别 |
| NEPG | AQI检测网格员 | grid001（王铁柱，沈阳市） | 另有 grid002~grid010；账号由管理员在“人员管理”中注册维护，grid005 处于请假状态 |
| NEPM | 系统管理员 | admin | 可在“人员管理”中注册网格员账号、维护信息（地区/工作状态/请假审批），并管理公众监督员信息 |
| NEPV | 决策者 | viewer | 可视化大屏 |

## 四端功能与需求的对应

### NEPS 公众监督员端（/sf/*）
- 注册（3-1）：手机号唯一校验、密码≥6位、真实姓名、年龄、性别 → `POST /api/auth/register`
- 登录（3-2）→ `POST /api/auth/login`
- 选择网格地址（3-3）：省/市二级联动 + 具体地址（≤100字）→ `POST /api/auth/profile`
- 反馈空气质量（3-4）：预估 AQI 等级（1优~6严重污染）+ 描述信息 → `POST /api/aqiFeedback/save`
- 浏览历史反馈（3-5）：仅本人、按时间倒序 → `GET /api/aqiFeedback/query?telId=`

### NEPG 网格员端（/gw/*）
- 登录（3-6）：登录编码，校验工作状态（请假/人员管理维护）
- 请假申请（新增）：填写事由与起止日期，由管理员审批；同意后进入请假（非工作）状态，无法登录与接单，销假后恢复
- 浏览指派任务（3-7）→ `GET /api/task/list/{gridCode}`
- 输入并提交实测 AQI 数据（3-8）：SO2/CO/PM2.5 三项浓度等级必填，
  **AQI = MAX（SO2AQI，COAQI，PM2.5AQI）** → `POST /api/task/measure`

### NEPM 系统管理端（/ 与 /admin/*）
- 公众监督数据列表（3-9）：条件查询（地区/等级/时间段/状态/关键字）→ `GET /api/aqiFeedback/query`
- 指派网格员（3-10）：本地指派优先（当前网格区域有在岗网格员），否则就近异地指派 → `POST /api/task/assign`
- 确认 AQI 数据（3-11）：列表/查询/详情/确认纳入统计/退回重新检测 → `GET /api/aqiData/list|confirm/{id}|reject/{id}`
- 统计数据管理（五项）→ `GET /api/stats/province|distribution|trend|realtime|coverage`
- 人员管理（HR，新增）：
  · 网格员管理（/admin/hr/grid）：注册网格员账号（编码/姓名/初始密码/负责省-市）、维护地区与工作状态
  · 请假审批（/admin/hr/grid 请假审批页）：审批网格员请假（同意→请假中/驳回），销假恢复工作状态
  · 公众监督员管理（/admin/hr/supervisor）：查看档案（手机号/姓名/年龄/性别/绑定地区/地址），编辑基本信息 → `GET/POST /api/employee|leave|supervisor/*`
- AQI 级别表维护（附录数据）→ `/api/aqi/*`

### NEPV 决策者端（/screen）
- 可视化大屏：省分组超标统计、AQI 指数分布、12 个月趋势、检测数量实时统计、全国网格覆盖率
  （覆盖口径：有网格员或有反馈记录的网格城市 / 34 省与 106 大城市）

## 状态机

```
反馈: 0待指派 ──指派──▶ 1已指派 ──提交实测──▶ 2待确认 ──确认──▶ 3已确认(完成)
                        ▲                    │
                        ├────退回(重新指派)────┘  （同时实测数据 state=2 已退回）
                        │
                        └── 超时自动回收：指派后 24 小时未提交实测数据
                            （配置 nep.task.repool-hours，定时扫描
                              nep.task.repool-scan-interval 默认 5 分钟；
                              preview 服务无调度线程，改为每个请求入口顺带扫描）
实测数据: 0待确认 → 1已确认(纳入统计) / 2已退回
```

> 说明：超时回收会清空该反馈的指派信息（gm_id/assign_date/assign_time）并回到 0待指派，
> 回收说明**追加**到 remarks（不覆盖原备注）。

### 指派规则：只允许本地指派

- 管理员指派时，网格员负责的**省+市必须与反馈一致**；后端 `POST /api/task/assign`
  对异地指派直接拒绝（提示"不允许异地指派"），前端弹窗也只列出本地网格员。
- 若该网格区域**没有可工作的本地网格员**（无人 / 全部请假中），不允许异地抽调，
  改为发起**增员请求**：`POST /api/gridDemand/apply`，在
  `GET /api/gridDemand/list` 中可查（管理员在"人员管理 → 增员请求"页签跟进处理，
  决策者据此判断是否需要增加网格员）。同一区域已有待处理请求时复用，不重复生成。
- 前端注意：后端业务校验失败是 **HTTP 200 + body.code=400**，axios 不会抛异常，
  调用处必须显式判断 `res.data.code`（`front/src/api/task.js` 的 `assignTask` 已按此修正，
  否则被拒绝的指派会显示成"指派成功"）。
```

## 密码加密（需求 3.3.3 数据保密性）

- 后端 `com.example.demo.util.SecretUtil`：`salt$sha256(salt+明文)` 加盐存储，登录时校验。
- 种子账号密码均为 123456（已按该算法预生成）。

## 测试与文档

- **`doc/软件测试报告-东软环保公众监督系统.md`**（及 .docx）：依据《Web管理系统软件测试模板》编制的功能/非功能测试报告，含 54 条功能用例（登录注册、反馈、指派、实测、确认、人员管理、统计大屏、AQI级别）与兼容性/易用性/安全/可靠性/性能测试结果。
- **`doc/自动化测试报告-东软环保公众监督系统.md`**（及 .docx）：依据《Web管理系统自动化测试模板》编制的 UI 自动化测试报告，含 5 个测试类的关键代码与执行结果。
- **`auto_test/`**：可运行的 UI 自动化工程（Java 17 + Selenium 4 + Chrome，Maven 管理，chromedriver 由 Selenium Manager 自动下载）。5 个测试类：`LoginTest`（4 角色登录+异常分支）、`SupervisorFeedbackTest`（注册+提交反馈+历史回显）、`FeedbackAssignTest`（查询/指派/删除）、`GridMeasureTest`（反馈→指派→实测→确认端到端闭环）、`HrGridTest`（网格员增改+请假审批+状态联动）。运行方式见 `auto_test/README.md`。

## 沙箱预览服务说明

`preview/nep_preview_server.py` 与 SpringBoot 版接口契约完全一致（同一套 URL、同一套
ResultVO 返回结构、同一套业务规则），供无法访问 Maven 中央仓库的沙箱环境在线体验使用；
本地开发请使用 `backend/demo` 的正式 SpringBoot 后端。

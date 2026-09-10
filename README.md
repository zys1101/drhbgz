# 东软环保公众监督系统（NEP）

依据《东软环保公众监督系统需求说明书 1.0.0-0.0.0》实现的完整系统：
汇总公众监督员的空气质量反馈 → 系统管理员指派网格员实地检测 → 实测 AQI 数据确认入库 →
面向决策者的五项统计与可视化大屏。地区采用网格化管理，最小网格单位为大城市（2022 年发布的 106 个大城市名单）。

## 技术栈（对应需求 2.1 开发环境）

| 分层 | 技术 |
| --- | --- |
| 数据库 | MySQL（脚本见 `sql/nep_system.sql`，可用 Navicat 直接导入） |
| 后端 | SpringBoot 3.2.8 + MyBatis-Plus 3.5.12 + springdoc-openapi（Maven 工程，位于 `backend/demo`） |
| 前端 | Vue3 + Vue CLI + Axios + ECharts（工程位于 `front/`） |

## 目录结构

```
├── sql/
│   ├── nep_system.sql        # MySQL 建库脚本（结构 + 34省/106大城市/AQI级别表/演示数据）
│   ├── seed_data.json        # 种子数据（单一数据源）
│   └── generate_seed.py      # 种子生成器：python3 sql/generate_seed.py 重新生成上面两个文件
├── backend/demo/             # SpringBoot 后端（正式后端，端口 9000，上下文 /api）
├── front/                    # Vue3 前端（devServer 端口 8080，/api 代理到 9000）
└── preview/
    └── nep_preview_server.py # 沙箱在线预览服务（Python 标准库 + SQLite，接口与后端完全一致）
```

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
python3 preview/nep_preview_server.py   # 监听 9000，首次启动自动建 SQLite 库并载入种子数据
```

## 演示账号（密码均为 123456）

| 端 | 角色 | 账号 | 说明 |
| --- | --- | --- | --- |
| NEPS | 公众监督员 | 13800001111（张伟） | 也可自行注册，手机号即身份唯一识别 |
| NEPG | AQI检测网格员 | grid001（王铁柱，沈阳市） | 另有 grid002~grid010；grid005 为非工作状态（HR系统管理） |
| NEPM | 系统管理员 | admin | 网格员/管理员/决策者账号由“东软HR系统”统一管理，不可注册 |
| NEPV | 决策者 | viewer | 可视化大屏 |

## 四端功能与需求的对应

### NEPS 公众监督员端（/sf/*）
- 注册（3-1）：手机号唯一校验、密码≥6位、真实姓名、年龄、性别 → `POST /api/auth/register`
- 登录（3-2）→ `POST /api/auth/login`
- 选择网格地址（3-3）：省/市二级联动 + 具体地址（≤100字）→ `POST /api/auth/profile`
- 反馈空气质量（3-4）：预估 AQI 等级（1优~6严重污染）+ 描述信息 → `POST /api/aqiFeedback/save`
- 浏览历史反馈（3-5）：仅本人、按时间倒序 → `GET /api/aqiFeedback/query?telId=`

### NEPG 网格员端（/gw/*）
- 登录（3-6）：登录编码，校验工作状态（HR系统）
- 浏览指派任务（3-7）→ `GET /api/task/list/{gridCode}`
- 输入并提交实测 AQI 数据（3-8）：SO2/CO/PM2.5 三项浓度等级必填，
  **AQI = MAX（SO2AQI，COAQI，PM2.5AQI）** → `POST /api/task/measure`

### NEPM 系统管理端（/ 与 /admin/*）
- 公众监督数据列表（3-9）：条件查询（地区/等级/时间段/状态/关键字）→ `GET /api/aqiFeedback/query`
- 指派网格员（3-10）：本地指派优先（当前网格区域有在岗网格员），否则就近异地指派 → `POST /api/task/assign`
- 确认 AQI 数据（3-11）：列表/查询/详情/确认纳入统计/退回重新检测 → `GET /api/aqiData/list|confirm/{id}|reject/{id}`
- 统计数据管理（五项）→ `GET /api/stats/province|distribution|trend|realtime|coverage`
- AQI 级别表维护（附录数据）→ `/api/aqi/*`

### NEPV 决策者端（/screen）
- 可视化大屏：省分组超标统计、AQI 指数分布、12 个月趋势、检测数量实时统计、全国网格覆盖率
  （覆盖口径：有网格员或有反馈记录的网格城市 / 34 省与 106 大城市）

## 状态机

```
反馈: 0待指派 ──指派──▶ 1已指派 ──提交实测──▶ 2待确认 ──确认──▶ 3已确认(完成)
                        ▲                    │
                        └──────退回(重新指派)──┘  （同时实测数据 state=2 已退回）
实测数据: 0待确认 → 1已确认(纳入统计) / 2已退回
```

## 密码加密（需求 3.3.3 数据保密性）

- 后端 `com.example.demo.util.SecretUtil`：`salt$sha256(salt+明文)` 加盐存储，登录时校验。
- 种子账号密码均为 123456（已按该算法预生成）。

## 沙箱预览服务说明

`preview/nep_preview_server.py` 与 SpringBoot 版接口契约完全一致（同一套 URL、同一套
ResultVO 返回结构、同一套业务规则），供无法访问 Maven 中央仓库的沙箱环境在线体验使用；
本地开发请使用 `backend/demo` 的正式 SpringBoot 后端。

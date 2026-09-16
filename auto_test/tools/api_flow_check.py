# -*- coding: utf-8 -*-
"""API 层流程核对：验证 auto_test 五个用例所依赖的业务流程与提示文案是否与运行中的后端一致。
仅使用标准库。后端：preview 服务 http://127.0.0.1:9000/api（与 SpringBoot 契约一致）。"""
import json
import random
import time
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:9000/api"
ok = 0
bad = 0


def call(method, path, body=None):
    url = BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def check(name, cond, detail=""):
    global ok, bad
    if cond:
        ok += 1
        print("  [通过] " + name)
    else:
        bad += 1
        print("  [失败] " + name + "  实际：" + str(detail))


def msg(res):
    return (res or {}).get("message", "")


print("==== 用例1 等价核对：登录矩阵 ====")
matrix = [
    ("supervisor", "13800001111", "123456", 200),
    ("grid", "grid001", "123456", 200),
    ("admin", "admin", "123456", 200),
    ("viewer", "viewer", "123456", 200),
    ("supervisor", "13800001111", "wrongpwd", 400),
    ("supervisor", "13900000000", "123456", 400),
    ("admin", "admin", "wrongpwd", 400),
    ("admin", "nobody", "123456", 400),
    ("viewer", "grid001", "123456", 400),
    ("grid", "grid005", "123456", 400),
]
for role, acc, pwd, want in matrix:
    r = call("POST", "/auth/login", {"account": acc, "password": pwd, "role": role})
    check("login %s/%s -> code=%s" % (role, acc, r.get("code")), r.get("code") == want, r)

print("---- 用例2 等价核对：注册 + 提交反馈 + 历史反馈 ----")
phone = "137" + str(random.randint(10000000, 99999999))
mark = "自动化测试标识AT" + str(int(time.time()) % 1000000)
r = call("POST", "/auth/register",
         {"telId": phone, "password": "123456", "realName": "自动化测试监督员", "age": 25, "gender": "男"})
check("注册新监督员 %s 返回“注册成功”" % phone, r.get("code") == 200 and "注册成功" in msg(r), r)
r = call("POST", "/auth/login", {"account": phone, "password": "123456", "role": "supervisor"})
check("新账号可登录（提示含“登录成功”）", r.get("code") == 200 and "登录成功" in msg(r), r)

prof = call("GET", "/auth/profile?account=13800002222")["data"]
r = call("POST", "/aqiFeedback/save", {
    "telId": "13800002222", "provinceId": prof["provinceId"], "cityId": prof["cityId"],
    "address": prof["address"], "afDate": time.strftime("%Y-%m-%d"), "afTime": time.strftime("%H:%M:%S"),
    "information": mark + "：建筑工地围挡缺失，扬尘较大，建议洒水降尘。", "estimatedGrade": 2})
check("提交反馈返回“提交成功”", r.get("code") == 200 and "提交成功" in msg(r), r)
feedbacks = call("GET", "/aqiFeedback/select")["data"]
mine = [f for f in feedbacks if f.get("telId") == "13800002222" and mark in (f.get("information") or "")]
check("历史反馈（仅本人）可见本次记录", len(mine) == 1, "匹配数=%d" % len(mine))
af_id = mine[0]["afId"] if mine else None
if mine:
    check("新提交反馈状态为“待指派”(state=0)", mine[0]["state"] == 0, mine[0]["state"])

print("---- 用例3 等价核对：管理员查询/指派/删除 ----")
pending = [f for f in call("GET", "/aqiFeedback/select")["data"] if f.get("state") == 0]
check("按状态“待指派”可过滤出数据", len(pending) > 0, "数量=%d" % len(pending))
workers = call("GET", "/gridWorker/list")["data"]
usable = [w for w in workers if w.get("working")]
check("指派弹窗存在可用网格员", len(usable) > 0, "可用数=%d" % len(usable))
if af_id and usable:
    r = call("POST", "/task/assign", {"afId": af_id, "gridCode": usable[0]["gridCode"]})
    check("指派返回“指派成功”", r.get("code") == 200 and "指派成功" in msg(r), r)
    after = [f for f in call("GET", "/aqiFeedback/select")["data"] if f["afId"] == af_id]
    check("指派后状态流转为“已指派”(state=1)", after and after[0]["state"] == 1, after)
# 删除：用本次新建的另一条待指派数据（避免影响后续链路）
r = call("POST", "/aqiFeedback/save", {
    "telId": "13800002222", "provinceId": prof["provinceId"], "cityId": prof["cityId"],
    "address": prof["address"], "afDate": time.strftime("%Y-%m-%d"), "afTime": time.strftime("%H:%M:%S"),
    "information": "待删除数据 " + mark, "estimatedGrade": 1})
del_id = None
for f in call("GET", "/aqiFeedback/select")["data"]:
    if ("待删除数据 " + mark) in (f.get("information") or ""):
        del_id = f["afId"]
if del_id:
    r = call("DELETE", "/aqiFeedback/delete/%s" % del_id)
    check("删除返回“删除成功”", r.get("code") == 200 and "删除成功" in msg(r), r)
    still = [f for f in call("GET", "/aqiFeedback/select")["data"] if f["afId"] == del_id]
    check("被删除的反馈已不在列表", len(still) == 0, "仍存在")

print("---- 用例4 等价核对：反馈→指派→实测→确认 端到端 ----")
e2e = "自动化链路标识E2E" + str(int(time.time()) % 1000000)
prof4 = call("GET", "/auth/profile?account=13800004444")["data"]
r = call("POST", "/aqiFeedback/save", {
    "telId": "13800004444", "provinceId": prof4["provinceId"], "cityId": prof4["cityId"],
    "address": prof4["address"], "afDate": time.strftime("%Y-%m-%d"), "afTime": time.strftime("%H:%M:%S"),
    "information": e2e + "：燃煤电厂厂区周边有刺鼻气味，疑似 SO₂ 超标，请核实。", "estimatedGrade": 3})
check("链路步骤1 提交反馈成功", r.get("code") == 200 and "提交成功" in msg(r), r)
target = [f for f in call("GET", "/aqiFeedback/select")["data"] if e2e in (f.get("information") or "")]
check("链路步骤1 检索到本次反馈（唯一标识命中）", len(target) == 1, "匹配数=%d" % len(target))
if target:
    check("链路步骤1 状态为“待指派”", target[0]["state"] == 0, target[0]["state"])
    r = call("POST", "/task/assign", {"afId": target[0]["afId"], "gridCode": "grid001"})
    check("链路步骤2 指派给 grid001 成功", r.get("code") == 200 and "指派成功" in msg(r), r)
    tasks = call("GET", "/task/list/grid001")["data"]
    hit = [t for t in tasks if e2e in (t.get("information") or "")]
    check("链路步骤3 grid001“我的任务”出现该任务", len(hit) == 1, "匹配数=%d" % len(hit))
    r = call("POST", "/task/measure", {"afId": target[0]["afId"], "gridCode": "grid001",
                                      "so2Grade": 2, "coGrade": 2, "pm25Grade": 3, "aqiGrade": 3})
    check("链路步骤3 提交实测数据成功", r.get("code") == 200 and "提交成功" in msg(r), r)
    rows = call("GET", "/aqiData/list")["data"]
    row = [d for d in rows if d.get("afId") == target[0]["afId"]]
    check("链路步骤4 “确认AQI数据”列表出现本次实测数据", len(row) == 1, "匹配数=%d" % len(row))
    if row:
        check("链路步骤4 数据状态为“待确认”(state=0)", row[0]["state"] == 0, row[0]["state"])
        check("链路步骤4 AQI=MAX(2,2,3)=3（界面呈“三级·轻度”）", row[0]["aqiGrade"] == 3, row[0]["aqiGrade"])
        check("链路步骤4 检测网格员为 grid001", row[0].get("gridCode") == "grid001", row[0].get("gridCode"))
        r = call("GET", "/aqiData/confirm/%s" % row[0]["dataId"])
        check("链路步骤4 确认返回“数据已确认，纳入统计范围”",
              "确认" in msg(r) and "统计" in msg(r), r)

print("---- 用例5 等价核对：网格员新增/编辑 + 请假审批 + 状态联动 ----")
code = "grid" + str(random.randint(1000, 9999))
edit_name = "自动化测试改名"
r = call("POST", "/employee/save", {"empCode": code, "realName": "自动化测试网格员", "password": "123456",
                                    "provinceId": 8, "cityId": 21, "working": 1})
check("新增网格员返回“网格员账号创建成功”", r.get("code") == 200 and "成功" in msg(r), r)
emp = [e for e in call("GET", "/employee/list")["data"] if e.get("empCode") == code]
check("网格员列表包含新账号 %s" % code, len(emp) == 1, "匹配数=%d" % len(emp))
if emp:
    r = call("POST", "/employee/update", {"empId": emp[0]["empId"], "realName": edit_name,
                                         "provinceId": emp[0]["provinceId"], "cityId": emp[0]["cityId"], "working": 1})
    check("编辑姓名返回“更新成功”", r.get("code") == 200 and "成功" in msg(r), r)
    emp2 = [e for e in call("GET", "/employee/list")["data"] if e.get("empCode") == code]
    check("列表姓名已更新", emp2 and emp2[0].get("realName") == edit_name, emp2)
r = call("POST", "/auth/login", {"account": code, "password": "123456", "role": "grid"})
check("新账号可登录（进入我的任务前置条件）", r.get("code") == 200, r)
r = call("POST", "/leave/apply", {"empCode": code, "reason": "自动化测试请假：家庭事务",
                                  "startDate": "2026-09-21", "endDate": "2026-09-22"})
check("请假申请返回“请假申请已提交”", r.get("code") == 200 and "请假申请已提交" in msg(r), r)
leaves = call("GET", "/leave/list?empCode=%s" % code)["data"]
pend = [l for l in leaves if l.get("state") == 0]
check("请假审批列表包含本次申请且状态“待审批”", len(pend) == 1, "待审批数=%d" % len(pend))
if pend:
    r = call("POST", "/leave/approve", {"leaveId": pend[0]["leaveId"], "agree": True})
    check("同意请假返回“已同意请假”", r.get("code") == 200 and "已同意请假" in msg(r), r)
    emp3 = [e for e in call("GET", "/employee/list")["data"] if e.get("empCode") == code]
    check("工作状态变为“请假中”(working=0)", emp3 and emp3[0].get("working") in (0, False), emp3)
    r = call("POST", "/auth/login", {"account": code, "password": "123456", "role": "grid"})
    check("请假中账号再次登录被拒（账号不可用）", r.get("code") == 400 and "账号不可用" in msg(r), r)

print("=" * 52)
print("接口层等价核对结束：通过 %d 项，失败 %d 项" % (ok, bad))
print("=" * 52)

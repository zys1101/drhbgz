#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
东软环保公众监督系统 —— 沙箱预览服务（仅供在线预览演示使用）

说明：
  正式后端为 backend/demo 下的 SpringBoot(MyBatis-Plus + MySQL) 工程。
  本沙箱环境无法访问 Maven 中央仓库，无法运行 SpringBoot，
  因此提供这个与 SpringBoot 版接口契约完全一致的轻量预览服务（Python 标准库 + SQLite），
  使四端（NEPS/NEPG/NEPM/NEPV）功能可以在线完整体验。

  · 监听 9000 端口，与 SpringBoot 版一致（前端 devServer 代理 /api）
  · 首次启动自动建库（preview/nep_preview.db）并载入 sql/seed_data.json 种子数据
  · 业务规则与 SpringBoot 版保持一致（状态机/AQI=MAX/本地异地指派/确认退回/五项统计）

  启动：python3 preview/nep_preview_server.py
"""
import json
import math
import mimetypes
import os
import re
import sqlite3
import threading
import time
from datetime import date, datetime, timedelta
from decimal import ROUND_HALF_UP, Decimal
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(BASE_DIR)
DB_PATH = os.path.join(BASE_DIR, 'nep_preview.db')
SEED_PATH = os.path.join(ROOT, 'sql', 'seed_data.json')
DIST_DIR = os.path.join(ROOT, 'front', 'dist')

TEL_RE = re.compile(r'^1\d{10}$')
DATE_FMT = '%Y-%m-%d'
TIME_FMT = '%H:%M:%S'

_lock = threading.RLock()


def now_strs():
    now = datetime.now()
    return now.strftime(DATE_FMT), now.strftime(TIME_FMT)


# ================================================================ 数据库
def get_conn():
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute('PRAGMA foreign_keys=ON')
    return conn


def init_db():
    fresh = not os.path.exists(DB_PATH)
    if fresh:
        print('[preview] 初始化数据库:', DB_PATH)
    with open(SEED_PATH, encoding='utf-8') as fp:
        seed = json.load(fp)
    conn = get_conn()
    c = conn.cursor()
    if not fresh:
        # 既有数据库（可能含演示过程中产生的数据）只补齐人员管理相关结构，不重建
        c.execute('''CREATE TABLE IF NOT EXISTS leave (
          leave_id INTEGER PRIMARY KEY AUTOINCREMENT, emp_id INTEGER NOT NULL,
          reason TEXT NOT NULL, start_date TEXT NOT NULL, end_date TEXT NOT NULL,
          state INTEGER NOT NULL DEFAULT 0, apply_date TEXT, apply_time TEXT,
          approve_date TEXT, approve_time TEXT)''')
        # 网格员增员请求表（本地无可用网格员时由管理员发起）
        c.execute('''CREATE TABLE IF NOT EXISTS grid_demand (
          demand_id INTEGER PRIMARY KEY AUTOINCREMENT, province_id INTEGER NOT NULL,
          city_id INTEGER NOT NULL, af_id INTEGER, reason TEXT,
          state INTEGER NOT NULL DEFAULT 0, apply_date TEXT, apply_time TEXT,
          handle_date TEXT, handle_time TEXT, handle_remark TEXT)''')
        if not c.execute('SELECT 1 FROM leave LIMIT 1').fetchone():
            _seed_leaves(c, seed)
        conn.commit()
        conn.close()
        print('[preview] 已补齐人员管理（请假）与增员请求结构，保留原数据')
        return
    c.executescript('''
    CREATE TABLE grid_province (province_id INTEGER PRIMARY KEY, province_name TEXT NOT NULL);
    CREATE TABLE grid_city (
      city_id INTEGER PRIMARY KEY, city_name TEXT NOT NULL, province_id INTEGER NOT NULL,
      city_tier TEXT, is_big_city INTEGER NOT NULL DEFAULT 1);
    CREATE TABLE aqi (
      aqi_id INTEGER PRIMARY KEY, chinese_explain TEXT, aqi_explain TEXT, aqi_range TEXT,
      color TEXT, so2_min INTEGER, so2_max INTEGER, co_min INTEGER, co_max INTEGER,
      spm_min INTEGER, spm_max INTEGER, health_impact TEXT, take_steps TEXT);
    CREATE TABLE employee (
      emp_id INTEGER PRIMARY KEY, emp_code TEXT UNIQUE NOT NULL, password TEXT NOT NULL,
      real_name TEXT NOT NULL, role TEXT NOT NULL, province_id INTEGER, city_id INTEGER,
      working INTEGER NOT NULL DEFAULT 1);
    CREATE TABLE supervisor (
      tel_id TEXT PRIMARY KEY, password TEXT NOT NULL, real_name TEXT NOT NULL, age INTEGER,
      gender TEXT, province_id INTEGER, city_id INTEGER, address TEXT,
      register_date TEXT, register_time TEXT);
    CREATE TABLE aqi_feedback (
      af_id INTEGER PRIMARY KEY AUTOINCREMENT, tel_id TEXT NOT NULL,
      province_id INTEGER NOT NULL, city_id INTEGER NOT NULL, address TEXT,
      information TEXT, estimated_grade INTEGER, af_date TEXT, af_time TEXT,
      gm_id INTEGER, assign_date TEXT, assign_time TEXT,
      state INTEGER NOT NULL DEFAULT 0, remarks TEXT);
    CREATE TABLE aqi_data (
      data_id INTEGER PRIMARY KEY AUTOINCREMENT, af_id INTEGER NOT NULL UNIQUE,
      so2_grade INTEGER NOT NULL, co_grade INTEGER NOT NULL, pm25_grade INTEGER NOT NULL,
      aqi_grade INTEGER NOT NULL, emp_id INTEGER, grid_code TEXT,
      submit_date TEXT, submit_time TEXT, state INTEGER NOT NULL DEFAULT 0);
    ''')
    for p in seed['provinces']:
        c.execute('INSERT INTO grid_province VALUES (?,?)', (p['provinceId'], p['provinceName']))
    for ct in seed['cities']:
        c.execute('INSERT INTO grid_city VALUES (?,?,?,?,?)',
                  (ct['cityId'], ct['cityName'], ct['provinceId'], ct['cityTier'], ct['isBigCity']))
    for lv in seed['aqiLevels']:
        c.execute('INSERT INTO aqi VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)', (
            lv['aqiId'], lv['chineseExplain'], lv['aqiExplain'], lv['aqiRange'], lv['color'],
            lv['so2Min'], lv['so2Max'], lv['coMin'], lv['coMax'], lv['spmMin'], lv['spmMax'],
            lv['healthImpact'], lv['takeSteps']))
    for e in seed['employees']:
        c.execute('INSERT INTO employee VALUES (?,?,?,?,?,?,?,?)', (
            e['empId'], e['empCode'], e['password'], e['realName'], e['role'],
            e['provinceId'], e['cityId'], e['working']))
    for s in seed['supervisors']:
        c.execute('INSERT INTO supervisor VALUES (?,?,?,?,?,?,?,?,?,?)', (
            s['telId'], s['password'], s['realName'], s['age'], s['gender'],
            s['provinceId'], s['cityId'], s['address'], '2025-09-01', '09:00:00'))
    for f in seed['feedbacks']:
        c.execute('INSERT INTO aqi_feedback VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)', (
            f['afId'], f['telId'], f['provinceId'], f['cityId'], f['address'], f['information'],
            f['estimatedGrade'], f['afDate'], f['afTime'], f['gmId'], f['assignDate'],
            f['assignTime'], f['state'], f['remarks']))
    # 让自增主键接着种子数据继续
    c.execute("UPDATE sqlite_sequence SET seq=? WHERE name='aqi_feedback'",
              (max(f['afId'] for f in seed['feedbacks']),))
    for d in seed['aqiData']:
        c.execute('INSERT INTO aqi_data VALUES (?,?,?,?,?,?,?,?,?,?,?)', (
            d['dataId'], d['afId'], d['so2Grade'], d['coGrade'], d['pm25Grade'], d['aqiGrade'],
            d['empId'], d['gridCode'], d['submitDate'], d['submitTime'], d['state']))
    c.execute("UPDATE sqlite_sequence SET seq=? WHERE name='aqi_data'",
              (max(d['dataId'] for d in seed['aqiData']),))
    # 人员管理：请假表
    c.execute('''CREATE TABLE IF NOT EXISTS leave (
      leave_id INTEGER PRIMARY KEY AUTOINCREMENT, emp_id INTEGER NOT NULL,
      reason TEXT NOT NULL, start_date TEXT NOT NULL, end_date TEXT NOT NULL,
      state INTEGER NOT NULL DEFAULT 0, apply_date TEXT, apply_time TEXT,
      approve_date TEXT, approve_time TEXT)''')
    _seed_leaves(c, seed)
    # 网格员增员请求表（本地无可用网格员时的增员请求）
    # 与 leave 一样用 IF NOT EXISTS 幂等补建，已存在的旧库也能自动升级
    c.execute('''CREATE TABLE IF NOT EXISTS grid_demand (
      demand_id INTEGER PRIMARY KEY AUTOINCREMENT, province_id INTEGER NOT NULL,
      city_id INTEGER NOT NULL, af_id INTEGER, reason TEXT,
      state INTEGER NOT NULL DEFAULT 0, apply_date TEXT, apply_time TEXT,
      handle_date TEXT, handle_time TEXT, handle_remark TEXT)''')
    conn.commit()
    conn.close()
    print('[preview] 种子数据载入完成：省%d 市%d 反馈%d 实测%d 请假%d' % (
        len(seed['provinces']), len(seed['cities']), len(seed['feedbacks']),
        len(seed['aqiData']), len(seed.get('leaves', []))))


def _seed_leaves(c, seed):
    """载入请假示例数据（待审批/已同意/已驳回），供人员管理演示"""
    for lv in seed.get('leaves', []):
        c.execute('INSERT INTO leave (leave_id, emp_id, reason, start_date, end_date, state,'
                  ' apply_date, apply_time, approve_date, approve_time) VALUES (?,?,?,?,?,?,?,?,?,?)', (
            lv['leaveId'], lv['empId'], lv['reason'], lv['startDate'], lv['endDate'], lv['state'],
            lv.get('applyDate'), lv.get('applyTime'), lv.get('approveDate'), lv.get('approveTime')))
    ids = [lv['leaveId'] for lv in seed.get('leaves', [])]
    if ids:
        c.execute("UPDATE sqlite_sequence SET seq=? WHERE name='leave'", (max(ids),))


# ================================================================ 工具
def row_to_camel(row):
    """sqlite Row -> camelCase dict"""
    if row is None:
        return None
    out = {}
    for key in row.keys():
        parts = key.split('_')
        camel = parts[0] + ''.join(p.title() for p in parts[1:])
        out[camel] = row[key]
    return out


def rows_to_camel(rows):
    return [row_to_camel(r) for r in rows]


class BizError(Exception):
    def __init__(self, code, message):
        super().__init__(message)
        self.code = code
        self.message = message


def sha256_hex(raw):
    import hashlib
    return hashlib.sha256(raw.encode('utf-8')).hexdigest()


def verify_password(raw, stored):
    if not raw or not stored or '$' not in stored:
        return False
    salt = stored.split('$', 1)[0]
    return salt + '$' + sha256_hex(salt + raw) == stored


# ================================================================ 业务
FEEDBACK_SELECT = '''
SELECT aqi.*, p.province_name, c.city_name, e.real_name AS grid_name, e.emp_code AS grid_code,
       d.aqi_grade, d.so2_grade, d.co_grade, d.pm25_grade,
       d.submit_date AS measure_date, d.submit_time AS measure_time
FROM aqi_feedback aqi
JOIN grid_province p ON aqi.province_id = p.province_id
JOIN grid_city c ON aqi.city_id = c.city_id
LEFT JOIN employee e ON aqi.gm_id = e.emp_id
LEFT JOIN aqi_data d ON d.af_id = aqi.af_id AND d.state = 1
'''


def feedback_query(conn, where='1=1', params=(), order=True):
    sql = FEEDBACK_SELECT + (' WHERE ' + where) + (' ORDER BY aqi.af_date DESC, aqi.af_time DESC' if order else '')
    return rows_to_camel(conn.execute(sql, params).fetchall())


def get_feedback(conn, af_id):
    rows = feedback_query(conn, 'aqi.af_id = ?', (af_id,), order=False)
    return rows[0] if rows else None


def get_employee_by_code(conn, code):
    return conn.execute('SELECT * FROM employee WHERE emp_code = ?', (code,)).fetchone()


# ---------------------------------------------------------------- 任务超时回池
# 与 SpringBoot 端一致：已指派超过 REPOOL_HOURS 小时仍未提交实测数据 → 自动回到“待指派”池。
# SpringBoot 用 @Scheduled 定时扫描，预览服务没有常驻调度线程，改为在每个请求入口
# 先做一次轻量扫描（只查 state=1 的行），效果等价且可观测。
REPOOL_HOURS = int(os.environ.get('NEP_REPOOL_HOURS', '24'))


def repool_timed_out_tasks(conn):
    """回收超时未接单的任务，返回回收条数"""
    rows = conn.execute(
        "SELECT af_id, assign_date, assign_time, remarks FROM aqi_feedback WHERE state = 1").fetchall()
    if not rows:
        return 0
    deadline = datetime.now() - timedelta(hours=REPOOL_HOURS)
    tip = '指派超过 %d 小时未提交实测数据，已自动回收重新进入待指派池' % REPOOL_HOURS
    count = 0
    for r in rows:
        if not r['assign_date']:
            continue
        t = (r['assign_time'] or '00:00:00')
        if len(t) == 5:
            t += ':00'
        try:
            assigned_at = datetime.strptime('%s %s' % (r['assign_date'], t), '%Y-%m-%d %H:%M:%S')
        except ValueError:
            continue
        if assigned_at > deadline:
            continue
        old = (r['remarks'] or '').strip()
        remarks = tip if not old else old + '；' + tip   # 追加，不覆盖原有备注
        conn.execute('UPDATE aqi_feedback SET state=0, gm_id=NULL, assign_date=NULL, '
                     'assign_time=NULL, remarks=? WHERE af_id=?', (remarks, r['af_id']))
        count += 1
    if count:
        conn.commit()
    return count


def api_repool(conn):
    n = repool_timed_out_tasks(conn)
    return n, ('已回收 %d 条超时任务' % n) if n else '没有超时未接单的任务'


# ---------------------------------------------------------------- 网格员增员请求
def has_local_working_worker(conn, province_id, city_id):
    """该网格区域是否存在可工作的本地网格员"""
    row = conn.execute(
        "SELECT COUNT(*) AS n FROM employee WHERE role='grid' AND working=1 "
        "AND province_id=? AND city_id=?", (province_id, city_id)).fetchone()
    return row['n'] > 0


def api_grid_demand_apply(conn, body):
    """发起增员/增援申请。
    两种调用方式（与 SpringBoot 端一致）：
      1. 管理员在“指派”弹窗发起：传 afId
      2. 决策者在大屏发起：传 provinceId + cityId（大屏看不到具体反馈），
         自动关联该区域最早的一条待指派反馈；可选 source=viewer 用于标注发起方
    """
    af_id = int(body.get('afId') or 0)
    from_viewer = str(body.get('source') or '').lower() == 'viewer'
    fb = None
    if af_id:
        fb = conn.execute('SELECT * FROM aqi_feedback WHERE af_id=?', (af_id,)).fetchone()
        if fb is None:
            raise BizError(400, '反馈数据不存在')
    else:
        # 未给反馈编号：按区域发起（决策者大屏）
        province_id = body.get('provinceId')
        city_id = body.get('cityId')
        if province_id is None or city_id is None:
            raise BizError(400, '缺少反馈编号或区域（provinceId/cityId）')
        fb = conn.execute(
            'SELECT * FROM aqi_feedback WHERE state=0 AND province_id=? AND city_id=? '
            'ORDER BY af_id LIMIT 1', (int(province_id), int(city_id))).fetchone()
        if fb is None:
            raise BizError(400, '该网格区域当前没有待指派任务，无需增援')

    if has_local_working_worker(conn, fb['province_id'], fb['city_id']):
        raise BizError(400, '该网格区域已有可工作的网格员，请直接本地指派')
    exist = conn.execute(
        "SELECT * FROM grid_demand WHERE state=0 AND province_id=? AND city_id=? LIMIT 1",
        (fb['province_id'], fb['city_id'])).fetchone()
    if exist is not None:
        return row_to_camel(exist), '该网格区域已有待处理的增员请求（编号 %d）' % exist['demand_id']

    city = conn.execute('SELECT city_name FROM grid_city WHERE city_id=?',
                        (fb['city_id'],)).fetchone()
    city_name = city['city_name'] if city else str(fb['city_id'])
    if body.get('reason'):
        reason = body['reason']
    elif from_viewer:
        reason = ('决策者在大屏发起增援申请：网格区域【%s】无可工作的本地网格员，待指派任务无法派单'
                  % city_name)
    else:
        reason = ('网格区域【%s】无可工作的本地网格员，反馈任务无法指派，申请增加网格员' % city_name)
    d, t = now_strs()
    cur = conn.execute(
        'INSERT INTO grid_demand (province_id, city_id, af_id, reason, state, apply_date, apply_time)'
        ' VALUES (?,?,?,?,0,?,?)', (fb['province_id'], fb['city_id'], fb['af_id'], reason, d, t))
    conn.commit()
    row = conn.execute('SELECT * FROM grid_demand WHERE demand_id=?', (cur.lastrowid,)).fetchone()
    return row_to_camel(row), ('已提交增援申请，等待管理员处理' if from_viewer
                               else '已提交增员请求，等待决策者/管理员处理')


GRID_DEMAND_SELECT = '''
SELECT d.*, p.province_name, c.city_name
FROM grid_demand d
LEFT JOIN grid_province p ON d.province_id = p.province_id
LEFT JOIN grid_city c ON d.city_id = c.city_id
'''


def api_grid_demand_list(conn, qs):
    conds, params = ['1=1'], []
    if qs.get('state'):
        conds.append('d.state = ?')
        params.append(int(qs['state']))
    if qs.get('cityId'):
        conds.append('d.city_id = ?')
        params.append(int(qs['cityId']))
    rows = conn.execute(GRID_DEMAND_SELECT + ' WHERE ' + ' AND '.join(conds) +
                        ' ORDER BY d.state ASC, d.demand_id DESC', params).fetchall()
    return rows_to_camel(rows), '查询成功'


def api_grid_demand_handle(conn, body):
    demand_id = int(body.get('demandId') or 0)
    row = conn.execute('SELECT * FROM grid_demand WHERE demand_id=?', (demand_id,)).fetchone()
    if row is None:
        raise BizError(400, '增员请求不存在')
    if row['state'] != 0:
        raise BizError(400, '该请求已处理，不能重复处理')
    state = int(body.get('state') if body.get('state') is not None else 1)
    if state not in (1, 2):
        raise BizError(400, '处理结果只能是“已处理”或“已忽略”')
    d, t = now_strs()
    conn.execute('UPDATE grid_demand SET state=?, handle_date=?, handle_time=?, handle_remark=? '
                 'WHERE demand_id=?', (state, d, t, body.get('remark'), demand_id))
    conn.commit()
    return True, ('已标记为已处理' if state == 1 else '已忽略该请求')


def require_grid_worker(conn, grid_code):
    emp = get_employee_by_code(conn, grid_code)
    if emp is None or emp['role'] != 'grid':
        raise BizError(400, '网格员不存在')
    return emp


def api_register(conn, body):
    tel = (body.get('telId') or '').strip()
    if not TEL_RE.match(tel):
        raise BizError(400, '请输入正确的11位手机号')
    if not body.get('password') or len(body['password']) < 6:
        raise BizError(400, '密码不能少于6位')
    if not body.get('realName'):
        raise BizError(400, '请输入真实姓名')
    age = body.get('age')
    if not isinstance(age, int) or age < 1 or age > 120:
        raise BizError(400, '请输入有效年龄')
    if body.get('gender') not in ('男', '女'):
        raise BizError(400, '请选择性别')
    if conn.execute('SELECT 1 FROM supervisor WHERE tel_id=?', (tel,)).fetchone():
        raise BizError(400, '该手机号已存在')
    d, t = now_strs()
    salt = 'nep'
    conn.execute(
        'INSERT INTO supervisor (tel_id,password,real_name,age,gender,register_date,register_time)'
        ' VALUES (?,?,?,?,?,?,?)',
        (tel, salt + '$' + sha256_hex(salt + body['password']), body['realName'],
         age, body['gender'], d, t))
    conn.commit()
    return True, '注册成功'


def api_login(conn, body):
    account = (body.get('account') or '').strip()
    password = body.get('password') or ''
    role = body.get('role') or ''
    if not account or not password:
        raise BizError(400, '请输入账号和密码')
    if role == 'supervisor':
        sup = conn.execute('SELECT * FROM supervisor WHERE tel_id=?', (account,)).fetchone()
        if sup is None:
            raise BizError(400, '该用户不存在，请先注册')
        if not verify_password(password, sup['password']):
            raise BizError(400, '手机号或密码错误')
        real_role, real_name = 'supervisor', sup['real_name']
    else:
        emp = get_employee_by_code(conn, account)
        if emp is None:
            raise BizError(400, '账号或密码错误')
        if not verify_password(password, emp['password']):
            raise BizError(400, '账号或密码错误')
        if emp['working'] != 1:
            raise BizError(400, '账号不可用，请联系管理员（账号状态由人员管理维护）')
        real_role, real_name = emp['role'], emp['real_name']
        if role and role != real_role:
            raise BizError(400, '该账号不属于当前选择的用户类型')
    return {'account': account, 'role': real_role, 'username': real_name}, '登录成功'


def api_profile(conn, account):
    row = conn.execute('''
        SELECT s.*, p.province_name, c.city_name FROM supervisor s
        LEFT JOIN grid_province p ON s.province_id=p.province_id
        LEFT JOIN grid_city c ON s.city_id=c.city_id WHERE s.tel_id=?''', (account,)).fetchone()
    if row is None:
        raise BizError(400, '用户不存在')
    data = row_to_camel(row)
    data.pop('password', None)
    return data, '查询成功'


def api_save_profile(conn, body):
    account = str(body.get('account') or '')
    province_id = body.get('provinceId')
    city_id = body.get('cityId')
    address = (str(body.get('address') or '')).strip()
    sup = conn.execute('SELECT * FROM supervisor WHERE tel_id=?', (account,)).fetchone()
    if sup is None:
        raise BizError(400, '用户不存在')
    if not province_id or not city_id:
        raise BizError(400, '请选择完整网格区域')
    if not address or len(address) > 100:
        raise BizError(400, '请填写有效地址')
    conn.execute('UPDATE supervisor SET province_id=?, city_id=?, address=? WHERE tel_id=?',
                 (int(province_id), int(city_id), address, account))
    conn.commit()
    return api_profile(conn, account)


def api_feedback_save(conn, body):
    tel = (str(body.get('telId') or '')).strip()
    sup = conn.execute('SELECT 1 FROM supervisor WHERE tel_id=?', (tel,)).fetchone()
    if not sup:
        raise BizError(400, '反馈者身份无效，请重新登录')
    province_id, city_id = body.get('provinceId'), body.get('cityId')
    address = (body.get('address') or '').strip()
    grade = body.get('estimatedGrade')
    info = (body.get('information') or '').strip()
    if not province_id or not city_id:
        raise BizError(400, '请选择完整网格区域（省、市）')
    if not address or len(address) > 100:
        raise BizError(400, '请填写有效地址')
    if not grade or int(grade) < 1 or int(grade) > 6:
        raise BizError(400, '请选择预估AQI等级')
    if not info:
        raise BizError(400, '请填写空气质量描述')
    d, t = now_strs()
    conn.execute('''
        INSERT INTO aqi_feedback (tel_id, province_id, city_id, address, information,
          estimated_grade, af_date, af_time, state)
        VALUES (?,?,?,?,?,?,?,?,0)''',
        (tel, int(province_id), int(city_id), address, info, int(grade), d, t))
    conn.commit()
    return True, '提交成功'


def api_assign(conn, body):
    af_id = int(body.get('afId') or 0)
    grid_code = str(body.get('gridCode') or '')
    fb = conn.execute('SELECT * FROM aqi_feedback WHERE af_id=?', (af_id,)).fetchone()
    if fb is None:
        raise BizError(400, '反馈数据不存在')
    if fb['state'] == 3:
        raise BizError(400, '该反馈已完成确认，不能再指派')
    emp = require_grid_worker(conn, grid_code)
    if emp['working'] != 1:
        raise BizError(400, '该网格员当前处于非工作状态（请假/人员管理维护）')
    # 业务规则：只允许本地指派。本地无可用网格员时应发起“增员请求”。
    if emp['province_id'] != fb['province_id'] or emp['city_id'] != fb['city_id']:
        raise BizError(400, '该网格区域无可工作的本地网格员，不允许异地指派；请发起“增员请求”')
    d, t = now_strs()
    conn.execute('UPDATE aqi_feedback SET gm_id=?, assign_date=?, assign_time=?, state=1 WHERE af_id=?',
                 (emp['emp_id'], d, t, af_id))
    conn.commit()
    return True, '指派成功'


def api_my_tasks(conn, grid_code):
    emp = require_grid_worker(conn, grid_code)
    rows = feedback_query(conn, 'aqi.gm_id = ? AND aqi.state = 1', (emp['emp_id'],))
    return rows, '查询成功'


def api_measure(conn, body):
    af_id = int(body.get('afId') or 0)
    grid_code = str(body.get('gridCode') or '')
    so2, co, pm25 = body.get('so2Grade'), body.get('coGrade'), body.get('pm25Grade')
    fb = conn.execute('SELECT * FROM aqi_feedback WHERE af_id=?', (af_id,)).fetchone()
    if fb is None:
        raise BizError(400, '反馈任务不存在')
    if fb['state'] != 1:
        raise BizError(400, '任务当前状态不可提交实测数据')
    emp = get_employee_by_code(conn, grid_code)
    if emp is None:
        raise BizError(400, '网格员不存在')
    for g, name in ((so2, 'SO2二氧化硫'), (co, 'CO一氧化碳'), (pm25, 'PM2.5悬浮颗粒物')):
        if not g or int(g) < 1 or int(g) > 6:
            raise BizError(400, '请完整录入%s AQI浓度等级（1-6级）' % name)
    so2, co, pm25 = int(so2), int(co), int(pm25)
    aqi = max(so2, co, pm25)  # AQI = MAX（SO2AQI，COAQI，PM2.5AQI）
    d, t = now_strs()
    conn.execute('''
        INSERT INTO aqi_data (af_id, so2_grade, co_grade, pm25_grade, aqi_grade,
          emp_id, grid_code, submit_date, submit_time, state)
        VALUES (?,?,?,?,?,?,?,?,?,0)''',
        (af_id, so2, co, pm25, aqi, emp['emp_id'], emp['emp_code'], d, t))
    conn.execute('UPDATE aqi_feedback SET state=2 WHERE af_id=?', (af_id,))
    conn.commit()
    return {'afId': af_id, 'so2Grade': so2, 'coGrade': co, 'pm25Grade': pm25, 'aqiGrade': aqi}, '提交成功'


AQI_DATA_SELECT = '''
SELECT d.*, f.province_id, f.city_id, f.address, f.tel_id, f.estimated_grade, f.information,
       p.province_name, c.city_name
FROM aqi_data d
JOIN aqi_feedback f ON d.af_id = f.af_id
JOIN grid_province p ON f.province_id = p.province_id
JOIN grid_city c ON f.city_id = c.city_id
'''


def api_confirm(conn, data_id):
    data = conn.execute('SELECT * FROM aqi_data WHERE data_id=?', (data_id,)).fetchone()
    if data is None:
        raise BizError(400, '实测数据不存在')
    if data['state'] != 0:
        raise BizError(400, '该数据当前状态不可确认')
    conn.execute('UPDATE aqi_data SET state=1 WHERE data_id=?', (data_id,))
    conn.execute('UPDATE aqi_feedback SET state=3 WHERE af_id=?', (data['af_id'],))
    conn.commit()
    return True, '数据已确认，纳入统计范围'


def api_reject(conn, data_id):
    data = conn.execute('SELECT * FROM aqi_data WHERE data_id=?', (data_id,)).fetchone()
    if data is None:
        raise BizError(400, '实测数据不存在')
    if data['state'] != 0:
        raise BizError(400, '该数据当前状态不可退回')
    conn.execute('UPDATE aqi_data SET state=2 WHERE data_id=?', (data_id,))
    conn.execute('''UPDATE aqi_feedback SET state=0, gm_id=NULL,
        remarks='实测数据存在异常，已退回，需重新指派检测' WHERE af_id=?''', (data['af_id'],))
    conn.commit()
    return True, '已退回，任务重新进入待指派状态'


def api_grid_workers(conn):
    rows = conn.execute('''
        SELECT e.*, p.province_name, c.city_name FROM employee e
        LEFT JOIN grid_province p ON e.province_id=p.province_id
        LEFT JOIN grid_city c ON e.city_id=c.city_id
        WHERE e.role='grid' ORDER BY e.emp_id''').fetchall()
    out = []
    for r in rows:
        out.append({
            'empId': r['emp_id'], 'gridCode': r['emp_code'], 'realName': r['real_name'],
            'region': (r['province_name'] or '') + '-' + (r['city_name'] or ''),
            'working': bool(r['working'])})
    return out, '查询成功'


DATE_RE = re.compile(r'^\d{4}-\d{2}-\d{2}$')


def api_employee_list(conn):
    rows = conn.execute('''
        SELECT e.*, p.province_name, c.city_name FROM employee e
        LEFT JOIN grid_province p ON e.province_id=p.province_id
        LEFT JOIN grid_city c ON e.city_id=c.city_id
        WHERE e.role='grid' ORDER BY e.emp_id''').fetchall()
    return rows_to_camel(rows), '查询成功'


def api_employee_save(conn, body):
    code = (body.get('empCode') or '').strip()
    name = (body.get('realName') or '').strip()
    pwd = body.get('password') or ''
    if not code:
        raise BizError(400, '请输入登录编码')
    if not name:
        raise BizError(400, '请输入真实姓名')
    if len(pwd) < 6:
        raise BizError(400, '初始密码不能少于6位')
    prov = body.get('provinceId')
    if prov is None:
        raise BizError(400, '请选择负责省份')
    if conn.execute('SELECT 1 FROM employee WHERE emp_code=?', (code,)).fetchone():
        raise BizError(400, '该登录编码已存在')
    # 与种子/注册相同的加密：salt$sha256(salt+password)
    salt = 'nep'
    pw = salt + '$' + sha256_hex(salt + pwd)
    working = 1 if body.get('working') in (None, '', 1, True) else 0
    conn.execute('INSERT INTO employee (emp_code,password,real_name,role,province_id,city_id,working)'
                 ' VALUES (?,?,?,?,?,?,?)',
                 (code, pw, name, 'grid', int(prov),
                  int(body['cityId']) if body.get('cityId') else None, working))
    conn.commit()
    return True, '网格员账号创建成功'


def api_employee_update(conn, body):
    emp_id = int(body.get('empId') or 0)
    emp = conn.execute('SELECT * FROM employee WHERE emp_id=?', (emp_id,)).fetchone()
    if emp is None:
        raise BizError(400, '网格员不存在')
    fields, params = {}, []
    for col, key in (('real_name', 'realName'), ('province_id', 'provinceId'),
                     ('city_id', 'cityId'), ('working', 'working')):
        if body.get(key) is not None:
            fields[col] = body[key]
            params.append(body[key])
    if not fields:
        raise BizError(400, '没有需要修改的字段')
    cols = ', '.join('%s=?' % k for k in fields)
    conn.execute('UPDATE employee SET %s WHERE emp_id=?' % cols, params + [emp_id])
    conn.commit()
    return True, '更新成功'


def api_leave_apply(conn, body):
    code = (body.get('empCode') or body.get('gridCode') or '').strip()
    emp = conn.execute('SELECT * FROM employee WHERE emp_code=?', (code,)).fetchone()
    if emp is None or emp['role'] != 'grid':
        raise BizError(400, '网格员不存在')
    reason = (body.get('reason') or '').strip()
    start, end = body.get('startDate') or '', body.get('endDate') or ''
    if not reason:
        raise BizError(400, '请填写请假事由')
    if not DATE_RE.match(start) or not DATE_RE.match(end) or start > end:
        raise BizError(400, '请选择正确的起止日期')
    d, t = now_strs()
    conn.execute('INSERT INTO leave (emp_id,reason,start_date,end_date,state,apply_date,apply_time)'
                 ' VALUES (?,?,?,?,0,?,?)',
                 (emp['emp_id'], reason, start, end, d, t))
    conn.commit()
    return True, '请假申请已提交，等待管理员审批'


LEAVE_SELECT = '''
SELECT l.*, e.emp_code, e.real_name AS grid_name
FROM leave l JOIN employee e ON l.emp_id = e.emp_id
'''


def api_leave_list(conn, qs):
    conds, params = ['1=1'], []
    if qs.get('empCode'):
        conds.append('e.emp_code = ?')
        params.append(qs['empCode'])
    if qs.get('state'):
        conds.append('l.state = ?')
        params.append(int(qs['state']))
    rows = conn.execute(LEAVE_SELECT + ' WHERE ' + ' AND '.join(conds) +
                        ' ORDER BY l.leave_id DESC', params).fetchall()
    return rows_to_camel(rows), '查询成功'


def api_leave_approve(conn, body):
    leave_id = int(body.get('leaveId') or 0)
    agree = bool(body.get('agree'))
    row = conn.execute('SELECT * FROM leave WHERE leave_id=?', (leave_id,)).fetchone()
    if row is None:
        raise BizError(400, '请假申请不存在')
    if row['state'] != 0:
        raise BizError(400, '该申请已审批，不能重复处理')
    d, t = now_strs()
    if agree:
        # 同意请假：网格员进入请假（非工作）状态，登录与指派会被拦截
        conn.execute('UPDATE leave SET state=1, approve_date=?, approve_time=? WHERE leave_id=?',
                     (d, t, leave_id))
        conn.execute('UPDATE employee SET working=0 WHERE emp_id=?', (row['emp_id'],))
        msg = '已同意请假，该网格员进入请假状态'
    else:
        conn.execute('UPDATE leave SET state=2, approve_date=?, approve_time=? WHERE leave_id=?',
                     (d, t, leave_id))
        msg = '已驳回请假申请'
    conn.commit()
    return True, msg


def api_leave_back(conn, body):
    leave_id = int(body.get('leaveId') or 0)
    row = conn.execute('SELECT * FROM leave WHERE leave_id=?', (leave_id,)).fetchone()
    if row is None or row['state'] != 1:
        raise BizError(400, '仅已同意的请假可以销假')
    d, t = now_strs()
    conn.execute('UPDATE leave SET state=3, approve_date=?, approve_time=? WHERE leave_id=?',
                 (d, t, leave_id))
    conn.execute('UPDATE employee SET working=1 WHERE emp_id=?', (row['emp_id'],))
    conn.commit()
    return True, '已销假，网格员恢复工作状态'


def api_supervisor_list(conn):
    rows = conn.execute('''
        SELECT s.*, p.province_name, c.city_name FROM supervisor s
        LEFT JOIN grid_province p ON s.province_id=p.province_id
        LEFT JOIN grid_city c ON s.city_id=c.city_id
        ORDER BY s.register_date DESC, s.tel_id''').fetchall()
    return rows_to_camel(rows), '查询成功'


def api_supervisor_update(conn, body):
    tel = (body.get('telId') or '').strip()
    sup = conn.execute('SELECT * FROM supervisor WHERE tel_id=?', (tel,)).fetchone()
    if sup is None:
        raise BizError(400, '公众监督员不存在')
    fields, params = {}, []
    for col, key in (('real_name', 'realName'), ('age', 'age'), ('gender', 'gender'),
                     ('province_id', 'provinceId'), ('city_id', 'cityId'), ('address', 'address')):
        if body.get(key) is not None:
            fields[col] = body[key]
            params.append(body[key])
    if not fields:
        raise BizError(400, '没有需要修改的字段')
    cols = ', '.join('%s=?' % k for k in fields)
    conn.execute('UPDATE supervisor SET %s WHERE tel_id=?' % cols, params + [tel])
    conn.commit()
    return True, '更新成功'


def api_stats_province(conn):
    rows = conn.execute('''
        SELECT p.province_name AS province,
          SUM(CASE WHEN d.so2_grade >= 3 THEN 1 ELSE 0 END) AS so2,
          SUM(CASE WHEN d.co_grade >= 3 THEN 1 ELSE 0 END) AS co,
          SUM(CASE WHEN d.pm25_grade >= 3 THEN 1 ELSE 0 END) AS pm25,
          SUM(CASE WHEN d.aqi_grade >= 3 THEN 1 ELSE 0 END) AS aqi,
          COUNT(*) AS total
        FROM aqi_data d
        JOIN aqi_feedback f ON d.af_id = f.af_id
        JOIN grid_province p ON f.province_id = p.province_id
        WHERE d.state = 1
        GROUP BY p.province_id, p.province_name
        ORDER BY aqi DESC''').fetchall()
    return rows_to_camel(rows), '查询成功'


def api_stats_distribution(conn):
    rows = conn.execute(
        'SELECT aqi_grade AS grade, COUNT(*) AS cnt FROM aqi_data WHERE state=1 GROUP BY aqi_grade'
    ).fetchall()
    count_by = {r['grade']: r['cnt'] for r in rows}
    names = {1: '一级（优）', 2: '二级（良）', 3: '三级（轻度污染）',
             4: '四级（中度污染）', 5: '五级（重度污染）', 6: '六级（严重污染）'}
    return [{'name': names[g], 'value': count_by.get(g, 0)} for g in range(1, 7)], '查询成功'


def api_stats_trend(conn):
    today = datetime.now().date()
    months = []
    cur = today.replace(day=1)
    for i in range(11, -1, -1):
        m = cur.month - i
        y = cur.year
        while m <= 0:
            m += 12
            y -= 1
        months.append('%04d-%02d' % (y, m))
    start = months[0] + '-01'
    rows = conn.execute('''
        SELECT substr(submit_date, 1, 7) AS month, COUNT(*) AS exceed
        FROM aqi_data
        WHERE state=1 AND aqi_grade>=3 AND submit_date >= ?
        GROUP BY substr(submit_date, 1, 7)''', (start,)).fetchall()
    by_month = {r['month']: r['exceed'] for r in rows}
    return [{'month': m, 'exceed': by_month.get(m, 0)} for m in months], '查询成功'


def api_stats_realtime(conn):
    row = conn.execute('''
        SELECT COUNT(*) AS total,
          SUM(CASE WHEN aqi_grade <= 2 THEN 1 ELSE 0 END) AS good,
          SUM(CASE WHEN aqi_grade >= 3 THEN 1 ELSE 0 END) AS exceed
        FROM aqi_data WHERE state=1''').fetchone()
    return {'total': row['total'] or 0, 'good': row['good'] or 0, 'exceed': row['exceed'] or 0}, '查询成功'


def api_stats_coverage(conn):
    province_total = conn.execute('SELECT COUNT(*) AS n FROM grid_province').fetchone()['n']
    city_total = conn.execute('SELECT COUNT(*) AS n FROM grid_city WHERE is_big_city=1').fetchone()['n']
    covered = conn.execute('''
        SELECT DISTINCT c.city_id, c.city_name, p.province_name
        FROM grid_city c JOIN grid_province p ON c.province_id=p.province_id
        WHERE c.city_id IN (
          SELECT city_id FROM employee WHERE role='grid' AND city_id IS NOT NULL
          UNION SELECT city_id FROM aqi_feedback)''').fetchall()
    covered_list = sorted([{'province': r['province_name'], 'city': r['city_name']} for r in covered],
                          key=lambda x: x['province'])
    provinces_covered = len({c['province'] for c in covered_list})
    return {
        'provinceCovered': provinces_covered, 'provinceTotal': province_total,
        'cityCovered': len(covered_list), 'cityTotal': city_total,
        'coveredList': covered_list}, '查询成功'


# ---------------------------------------------------------------- 决策者（NEPV）看板统计
# 口径与 backend/demo 的 DecisionStatsController 逐行对应：
#   · 忙碌：在岗（working=1）且名下存在 state=1（已指派未完成）任务的网格员
#   · 空闲：在岗但没有在办任务的网格员
#   · 需增员：存在未处理的增员请求；或待指派任务量超过“空闲网格员 × 人均承载上限”
#   · 覆盖不足：该城市没有在岗网格员（有反馈却无人可派）
#   · 环境良好：反馈环比未上升，且该城市已确认实测的平均 AQI 等级 ≤ 2（优/良）
# 人均在办任务承载上限对应 SpringBoot 的 nep.task.worker-capacity（默认 3），可用环境变量覆盖
WORKER_CAPACITY = int(os.environ.get('NEP_WORKER_CAPACITY', '3'))

WORKFORCE_WORKER_SELECT = '''
SELECT e.emp_id, e.province_id, e.city_id, e.working,
       p.province_name, c.city_name
FROM employee e
LEFT JOIN grid_province p ON e.province_id = p.province_id
LEFT JOIN grid_city c ON e.city_id = c.city_id
WHERE e.role = 'grid'
ORDER BY e.emp_id
'''

BUSY_BY_GRID_SELECT = '''
SELECT gm_id, COUNT(*) AS cnt FROM aqi_feedback
WHERE state = 1 AND gm_id IS NOT NULL GROUP BY gm_id
'''

# 各城市已确认实测的平均 AQI 等级与样本数（判断“环境是否确实良好”）
CITY_AVG_GRADE_SELECT = '''
SELECT f.city_id, AVG(d.aqi_grade) AS avg_grade, COUNT(*) AS measured
FROM aqi_data d JOIN aqi_feedback f ON d.af_id = f.af_id
WHERE d.state = 1 GROUP BY f.city_id
'''

# 各城市在岗网格员数
CITY_WORKING_SELECT = '''
SELECT city_id, COUNT(*) AS cnt FROM employee
WHERE role = 'grid' AND working = 1 AND city_id IS NOT NULL GROUP BY city_id
'''

# 各城市待指派任务数（state=0）
CITY_PENDING_SELECT = '''
SELECT city_id, COUNT(*) AS cnt FROM aqi_feedback WHERE state = 0 GROUP BY city_id
'''


def api_stats_workforce(conn):
    """网格员人力看板：总数/在岗/非在岗/忙碌/空闲 + 是否需要增员"""
    workers = conn.execute(WORKFORCE_WORKER_SELECT).fetchall()
    busy_by_grid = {r['gm_id']: r['cnt']
                    for r in conn.execute(BUSY_BY_GRID_SELECT).fetchall()}

    total = len(workers)
    on_leave = 0
    busy = 0
    # city_id -> 聚合结果；dict 保持首次出现顺序，与 SpringBoot 的 LinkedHashMap 一致
    regions = {}
    for w in workers:
        working = (w['working'] == 1)
        if not working:
            on_leave += 1
        is_busy = working and busy_by_grid.get(w['emp_id'], 0) > 0
        if is_busy:
            busy += 1
        key = w['city_id'] if w['city_id'] is not None else -1
        region = regions.get(key)
        if region is None:
            region = {'provinceName': w['province_name'], 'cityName': w['city_name'],
                      'total': 0, 'working': 0, 'busy': 0, 'idle': 0}
            regions[key] = region
        region['total'] += 1
        if working:
            region['working'] += 1
            if is_busy:
                region['busy'] += 1
            else:
                region['idle'] += 1

    working_count = total - on_leave
    idle = working_count - busy

    pending_tasks = conn.execute(
        'SELECT COUNT(*) AS n FROM aqi_feedback WHERE state = 0').fetchone()['n']
    # 未处理的增员请求：与 GridDemandMapper.list 相同的排序（state ASC, demand_id DESC）
    demands = conn.execute(GRID_DEMAND_SELECT + ' WHERE d.state = 0'
                           ' ORDER BY d.state ASC, d.demand_id DESC').fetchall()

    # 是否需增员：① 有未处理的增员请求（某区域一个在岗网格员都没有）；
    #            ② 待指派任务量已超过“空闲网格员 × 人均承载上限”
    capacity = WORKER_CAPACITY
    demand_need = len(demands)
    backlog_need = 0
    if capacity > 0 and pending_tasks > idle * capacity:
        extra = pending_tasks - idle * capacity
        backlog_need = int(math.ceil(extra / float(capacity)))   # 向上取整，同 Math.ceil
    suggest_add = demand_need + backlog_need

    lack_regions = [{'provinceName': d['province_name'], 'cityName': d['city_name'],
                     'reason': d['reason'], 'afId': d['af_id']} for d in demands]
    # 给出“为什么需要增员”的可读依据，便于决策者直接判断
    need_reasons = ['%s · %s 无在岗网格员，已提交增员请求（反馈 %s）' % (
        d['province_name'], d['city_name'], '-' if d['af_id'] is None else d['af_id'])
        for d in demands]
    if backlog_need > 0:
        need_reasons.append(
            '待指派任务 %d 条，超出 %d 名空闲网格员按人均 %d 条的承载能力，建议增员 %d 人'
            % (pending_tasks, idle, capacity, backlog_need))

    # 可发起增援申请的区域：有任务却一个在岗网格员都没有（决策者大屏据此给出提交入口）
    demand_by_region = {(d['province_id'], d['city_id']): d for d in demands}
    need_worker_regions = []
    for r in conn.execute(
            "SELECT f.province_id AS province_id, f.city_id AS city_id, "
            "       p.province_name AS province_name, c.city_name AS city_name, "
            "       COUNT(*) AS pending_tasks "
            "FROM aqi_feedback f "
            "JOIN grid_province p ON f.province_id = p.province_id "
            "JOIN grid_city c ON f.city_id = c.city_id "
            "WHERE f.state = 0 AND NOT EXISTS ("
            "  SELECT 1 FROM employee e WHERE e.role='grid' AND e.working=1 "
            "    AND e.province_id = f.province_id AND e.city_id = f.city_id) "
            "GROUP BY f.province_id, f.city_id, p.province_name, c.city_name "
            "ORDER BY COUNT(*) DESC").fetchall():
        exist = demand_by_region.get((r['province_id'], r['city_id']))
        need_worker_regions.append({
            'provinceId': r['province_id'], 'cityId': r['city_id'],
            'provinceName': r['province_name'], 'cityName': r['city_name'],
            'pendingTasks': r['pending_tasks'],
            'hasDemand': exist is not None,
            'demandId': exist['demand_id'] if exist is not None else None
        })

    return {
        'total': total, 'working': working_count, 'onLeave': on_leave,
        'busy': busy, 'idle': idle, 'capacity': capacity,
        'pendingTasks': pending_tasks, 'pendingDemands': demand_need,
        'suggestAdd': suggest_add, 'needMore': suggest_add > 0,
        'needReasons': need_reasons, 'regions': list(regions.values()),
        'lackRegions': lack_regions,
        'needWorkerRegions': need_worker_regions}, '查询成功'


def java_round(x):
    """Java Math.round(double)：四舍五入（.5 向上），Python round() 是银行家舍入，不能直接用"""
    return int(math.floor(x + 0.5))


def java_fmt_grade(value):
    """Java String.format("%.1f", v)：对 double 的精确值做 HALF_UP 保留 1 位小数"""
    return str(Decimal(float(value)).quantize(Decimal('0.1'), rounding=ROUND_HALF_UP))


def coverage_reason(working, cur, prev, avg_grade, measured):
    """反馈“多/少”的原因判定，分支顺序与 SpringBoot DecisionStatsController#reasonOf 完全一致"""
    if working == 0:
        return '覆盖不足：该区域无在岗网格员，反馈少更可能是缺少检测覆盖而非环境良好'
    good_air = avg_grade is not None and measured > 0 and avg_grade <= 2.0
    if good_air and cur <= prev:
        return '环境良好：已确认实测平均 AQI 等级 %s（优/良，样本 %d），反馈自然较少' % (
            java_fmt_grade(avg_grade), measured)
    if cur < prev:
        return '反馈减少：该区域有在岗网格员，但反馈环比下降，需关注公众参与度（可能宣传/引导不足）'
    if cur > prev:
        return '反馈增加：环比上升，建议关注该区域空气质量与治理进展'
    return '基本持平：需继续观察（该区域已确认的实测样本不足，暂无法判定为环境良好）'


def api_stats_feedback_coverage(conn):
    """反馈覆盖度环比分析：哪些城市反馈多/少，以及“少”是环境良好还是覆盖不足"""
    today = datetime.now().date()
    current_month = today.strftime('%Y-%m')
    # 上月 = 本月第一天往前一天（等价于 Java 的 today.minusMonths(1)）
    previous_month = (today.replace(day=1) - timedelta(days=1)).strftime('%Y-%m')

    # 按城市 + 月份聚合本月/上月反馈数（af_date 为 'yyyy-MM-dd'，取前 7 位即月份）
    rows = conn.execute('''
        SELECT f.city_id, p.province_name, c.city_name,
               substr(f.af_date, 1, 7) AS month, COUNT(*) AS cnt
        FROM aqi_feedback f
        JOIN grid_province p ON f.province_id = p.province_id
        JOIN grid_city c ON f.city_id = c.city_id
        WHERE f.af_date >= ?
        GROUP BY f.city_id, p.province_name, c.city_name, substr(f.af_date, 1, 7)
        ORDER BY f.city_id''', (previous_month + '-01',)).fetchall()

    cities = []
    city_by_id = {}
    for r in rows:
        city = city_by_id.get(r['city_id'])
        if city is None:
            city = {'provinceName': r['province_name'], 'cityName': r['city_name'],
                    'cityId': r['city_id'], 'current': 0, 'previous': 0}
            city_by_id[r['city_id']] = city
            cities.append(city)
        if r['month'] == current_month:
            city['current'] += r['cnt']
        elif r['month'] == previous_month:
            city['previous'] += r['cnt']

    working_by_city = {r['city_id']: r['cnt']
                       for r in conn.execute(CITY_WORKING_SELECT).fetchall()}
    avg_grade_by_city = {}
    measured_by_city = {}
    for r in conn.execute(CITY_AVG_GRADE_SELECT).fetchall():
        avg_grade_by_city[r['city_id']] = r['avg_grade']
        measured_by_city[r['city_id']] = r['measured']
    pending_by_city = {r['city_id']: r['cnt']
                       for r in conn.execute(CITY_PENDING_SELECT).fetchall()}

    current_total = 0
    previous_total = 0
    for city in cities:
        cur, prev = city['current'], city['previous']
        current_total += cur
        previous_total += prev
        city_id = city['cityId']
        working = working_by_city.get(city_id, 0)
        avg_grade = avg_grade_by_city.get(city_id)
        measured = measured_by_city.get(city_id, 0)
        city['working'] = working
        city['avgGrade'] = avg_grade
        city['measured'] = measured
        city['pendingTasks'] = pending_by_city.get(city_id, 0)
        city['delta'] = cur - prev
        city['deltaPercent'] = None if prev == 0 else java_round(
            (cur - prev) * 1000.0 / prev) / 10.0
        city['reason'] = coverage_reason(working, cur, prev, avg_grade, measured)

    # 反馈“多/少”榜单：按本月反馈数降序取前 5；少榜即该排序的倒序前 5
    ordered = sorted(cities, key=lambda c: c['current'], reverse=True)
    more = ordered[:5]
    less = list(reversed(ordered))[:5]

    return {'currentMonth': current_month, 'previousMonth': previous_month,
            'currentTotal': current_total, 'previousTotal': previous_total,
            'cities': cities, 'more': more, 'less': less}, '查询成功'


def chunk_reply(text, size=6):
    """把回复按标点优先切成小块，用于 SSE 流式逐块输出"""
    import re
    parts = re.split(r'(?<=[；，。！？：:\n])', text)
    chunks = []
    for p in parts:
        if not p:
            continue
        if len(p) <= size * 2:
            chunks.append(p)
        else:
            for i in range(0, len(p), size):
                chunks.append(p[i:i + size])
    return chunks or [text]

# ================================================================ HTTP 服务
class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    server_version = 'NEP-Preview/1.0'

    def log_message(self, fmt, *args):  # 安静模式：仅打印关键请求
        pass

    # ---------- 响应 ----------
    def send_json(self, obj, status=200):
        data = json.dumps(obj, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json;charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()
        self.wfile.write(data)

    def send_file_bytes(self, body, mime, cache=False):
        self.send_response(200)
        self.send_header('Content-Type', mime)
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Cache-Control', 'public, max-age=3600' if cache else 'no-cache')
        self.end_headers()
        self.wfile.write(body)

    def serve_static(self, path):
        """托管 front/dist 构建产物；非 /api 的路径 SPA 回退到 index.html"""
        if not os.path.isdir(DIST_DIR):
            self.send_json({'code': 404, 'message':
                            '前端未构建：请先在 front/ 目录执行 npm run build'}, status=404)
            return
        rel = path.lstrip('/') or 'index.html'
        # 去掉查询串后已处理；防目录穿越
        rel = os.path.normpath(rel).lstrip('/')
        if rel.startswith('..'):
            self.send_json({'code': 404, 'message': 'Not Found'}, status=404)
            return
        file_path = os.path.join(DIST_DIR, rel)
        if not os.path.isfile(file_path):
            # SPA 路由回退
            file_path = os.path.join(DIST_DIR, 'index.html')
            rel = 'index.html'
        mime = mimetypes.guess_type(file_path)[0] or 'application/octet-stream'
        if mime.startswith('text/') or mime in ('application/javascript', 'application/json'):
            mime += '; charset=utf-8'
        with open(file_path, 'rb') as fp:
            body = fp.read()
        self.send_file_bytes(body, mime, cache=(rel != 'index.html'))

    def ok(self, data=None, message='操作成功'):
        self.send_json({'code': 200, 'message': message, 'data': data})

    def read_body(self):
        length = int(self.headers.get('Content-Length') or 0)
        if length <= 0:
            return {}
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode('utf-8'))
        except Exception:
            return {}

    # ---------- 路由 ----------
    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

    def do_GET(self):
        self.route('GET')

    def do_POST(self):
        self.route('POST')

    def do_DELETE(self):
        self.route('DELETE')

    def route(self, method):
        parsed = urlparse(self.path)
        path = parsed.path
        qs = {k: v[0] for k, v in parse_qs(parsed.query).items()}
        body = self.read_body() if method in ('POST', 'PUT') else {}
        # 去掉 /api 前缀
        is_api = path == '/api' or path.startswith('/api/')
        if path.startswith('/api/'):
            path = path[len('/api'):]
        try:
            with _lock:
                conn = get_conn()
                try:
                    self.dispatch(conn, method, path, qs, body, is_api)
                finally:
                    conn.close()
        except BizError as e:
            self.send_json({'code': e.code, 'message': e.message})
        except Exception as e:  # noqa
            import traceback
            traceback.print_exc()
            self.send_json({'code': 500, 'message': '操作失败', 'data': str(e)})

    def dispatch(self, conn, method, path, qs, body, is_api=False):
        # 每个请求入口先做一次超时任务回收扫描（等价于 SpringBoot 的定时任务）。
        # 手动触发接口 /task/repool 本身要返回“本次回收了几条”，因此跳过前置扫描，
        # 否则数量会被这里提前回收掉，接口永远返回 0。
        if path != '/task/repool':
            try:
                repool_timed_out_tasks(conn)
            except Exception:  # noqa
                pass
        # ---- 认证 ----
        if method == 'POST' and path == '/auth/register':
            _, msg = api_register(conn, body)
            return self.ok(True, msg)
        if method == 'POST' and path == '/auth/login':
            data, msg = api_login(conn, body)
            return self.ok(data, msg)
        if method == 'GET' and path == '/auth/profile':
            data, msg = api_profile(conn, qs.get('account', ''))
            return self.ok(data, msg)
        if method == 'POST' and path == '/auth/profile':
            data, msg = api_save_profile(conn, body)
            return self.ok(data, msg)
        # ---- 行政区划 ----
        if method == 'GET' and path == '/province/list':
            rows = conn.execute('SELECT * FROM grid_province ORDER BY province_id').fetchall()
            return self.ok(rows_to_camel(rows))
        if method == 'GET' and path == '/province/getCitys':
            rows = conn.execute('SELECT * FROM grid_city WHERE province_id=? ORDER BY city_id',
                                (qs.get('provinceId'),)).fetchall()
            return self.ok(rows_to_camel(rows))
        # ---- 反馈 ----
        if method == 'GET' and path == '/aqiFeedback/select':
            return self.ok(feedback_query(conn))
        if method == 'GET' and path == '/aqiFeedback/query':
            conds, params = ['1=1'], []
            if qs.get('telId'):
                conds.append('aqi.tel_id = ?')
                params.append(qs['telId'])
            if qs.get('provinceId'):
                conds.append('aqi.province_id = ?')
                params.append(int(qs['provinceId']))
            if qs.get('cityId'):
                conds.append('aqi.city_id = ?')
                params.append(int(qs['cityId']))
            if qs.get('grade'):
                conds.append('aqi.estimated_grade = ?')
                params.append(int(qs['grade']))
            if qs.get('state'):
                conds.append('aqi.state = ?')
                params.append(int(qs['state']))
            if qs.get('dateFrom'):
                conds.append('aqi.af_date >= ?')
                params.append(qs['dateFrom'])
            if qs.get('dateTo'):
                conds.append('aqi.af_date <= ?')
                params.append(qs['dateTo'])
            if qs.get('keyword'):
                conds.append("(aqi.address LIKE ? OR aqi.information LIKE ? OR aqi.tel_id LIKE ?"
                             " OR p.province_name LIKE ? OR c.city_name LIKE ?)")
                kw = '%' + qs['keyword'] + '%'
                params += [kw] * 5
            return self.ok(feedback_query(conn, ' AND '.join(conds), params))
        if method == 'GET' and path.startswith('/aqiFeedback/find/'):
            af_id = int(path.rsplit('/', 1)[-1])
            fb = conn.execute('SELECT * FROM aqi_feedback WHERE af_id=?', (af_id,)).fetchone()
            return self.ok(row_to_camel(fb))
        if method == 'POST' and path == '/aqiFeedback/save':
            _, msg = api_feedback_save(conn, body)
            return self.ok(True, msg)
        if method == 'POST' and path == '/aqiFeedback/update':
            fb = row_to_camel(conn.execute('SELECT * FROM aqi_feedback WHERE af_id=?',
                                           (body.get('afId'),)).fetchone())
            if fb is None:
                raise BizError(400, '反馈不存在')
            fields = {k: v for k, v in row_to_camel({k: body.get(k) for k in
                      ('af_id', 'tel_id', 'province_id', 'city_id', 'address', 'information',
                       'estimated_grade', 'state', 'remarks')}).items() if v is not None}
            sets = ', '.join('%s=?' % k for k in fields)
            conn.execute('UPDATE aqi_feedback SET %s WHERE af_id=?' % sets,
                         list(fields.values()) + [fb['afId']])
            conn.commit()
            return self.ok(True, '更新成功')
        if method == 'DELETE' and path.startswith('/aqiFeedback/delete/'):
            af_id = int(path.rsplit('/', 1)[-1])
            conn.execute('DELETE FROM aqi_feedback WHERE af_id=?', (af_id,))
            conn.commit()
            return self.ok(True, '删除成功')
        # ---- 任务流转 ----
        if method == 'POST' and path == '/task/assign':
            _, msg = api_assign(conn, body)
            return self.ok(True, msg)
        if method == 'GET' and path.startswith('/task/list/'):
            data, msg = api_my_tasks(conn, path.rsplit('/', 1)[-1])
            return self.ok(data, msg)
        if method == 'POST' and path == '/task/measure':
            data, msg = api_measure(conn, body)
            return self.ok(data, msg)
        if method == 'POST' and path == '/task/repool':
            n, msg = api_repool(conn)
            return self.ok(n, msg)
        # ---- 网格员增员请求 ----
        if method == 'POST' and path == '/gridDemand/apply':
            data, msg = api_grid_demand_apply(conn, body)
            return self.ok(data, msg)
        if method == 'GET' and path == '/gridDemand/list':
            data, msg = api_grid_demand_list(conn, qs)
            return self.ok(data, msg)
        if method == 'POST' and path == '/gridDemand/handle':
            _, msg = api_grid_demand_handle(conn, body)
            return self.ok(True, msg)
        # ---- 确认AQI数据 ----
        if method == 'GET' and path == '/aqiData/list':
            rows = conn.execute(AQI_DATA_SELECT + ' ORDER BY d.data_id DESC').fetchall()
            return self.ok(rows_to_camel(rows))
        if method == 'GET' and path.startswith('/aqiData/find/'):
            data_id = int(path.rsplit('/', 1)[-1])
            row = conn.execute(AQI_DATA_SELECT + ' WHERE d.data_id=?', (data_id,)).fetchone()
            return self.ok(row_to_camel(row))
        if method == 'GET' and path.startswith('/aqiData/confirm/'):
            _, msg = api_confirm(conn, int(path.rsplit('/', 1)[-1]))
            return self.ok(True, msg)
        if method == 'GET' and path.startswith('/aqiData/reject/'):
            _, msg = api_reject(conn, int(path.rsplit('/', 1)[-1]))
            return self.ok(True, msg)
        # ---- 网格员 ----
        if method == 'GET' and path == '/gridWorker/list':
            data, msg = api_grid_workers(conn)
            return self.ok(data, msg)
        # ---- 统计 ----
        if method == 'GET' and path == '/stats/province':
            data, msg = api_stats_province(conn)
            return self.ok(data, msg)
        if method == 'GET' and path == '/stats/distribution':
            data, msg = api_stats_distribution(conn)
            return self.ok(data, msg)
        if method == 'GET' and path == '/stats/trend':
            data, msg = api_stats_trend(conn)
            return self.ok(data, msg)
        if method == 'GET' and path == '/stats/realtime':
            data, msg = api_stats_realtime(conn)
            return self.ok(data, msg)
        if method == 'GET' and path == '/stats/coverage':
            data, msg = api_stats_coverage(conn)
            return self.ok(data, msg)
        # 决策者（NEPV）看板
        if method == 'GET' and path == '/stats/workforce':
            data, msg = api_stats_workforce(conn)
            return self.ok(data, msg)
        if method == 'GET' and path == '/stats/feedbackCoverage':
            data, msg = api_stats_feedback_coverage(conn)
            return self.ok(data, msg)
        # ---- AQI级别表 ----
        if method == 'GET' and path == '/aqi/list':
            rows = conn.execute('SELECT * FROM aqi ORDER BY aqi_id').fetchall()
            return self.ok(rows_to_camel(rows))
        if method == 'GET' and path.startswith('/aqi/find/'):
            row = conn.execute('SELECT * FROM aqi WHERE aqi_id=?',
                               (int(path.rsplit('/', 1)[-1]),)).fetchone()
            return self.ok(row_to_camel(row))
        if method == 'POST' and path == '/aqi/save':
            lv = body
            conn.execute('''INSERT INTO aqi (chinese_explain,aqi_explain,aqi_range,color,
              so2_min,so2_max,co_min,co_max,spm_min,spm_max,health_impact,take_steps)
              VALUES (?,?,?,?,?,?,?,?,?,?,?,?)''',
                (lv.get('chineseExplain'), lv.get('aqiExplain'), lv.get('aqiRange'), lv.get('color'),
                 lv.get('so2Min'), lv.get('so2Max'), lv.get('coMin'), lv.get('coMax'),
                 lv.get('spmMin'), lv.get('spmMax'), lv.get('healthImpact'), lv.get('takeSteps')))
            conn.commit()
            return self.ok(True, '保存成功')
        if method == 'POST' and path == '/aqi/update':
            lv = body
            conn.execute('''UPDATE aqi SET chinese_explain=?,aqi_explain=?,aqi_range=?,color=?,
              so2_min=?,so2_max=?,co_min=?,co_max=?,spm_min=?,spm_max=?,health_impact=?,take_steps=?
              WHERE aqi_id=?''',
                (lv.get('chineseExplain'), lv.get('aqiExplain'), lv.get('aqiRange'), lv.get('color'),
                 lv.get('so2Min'), lv.get('so2Max'), lv.get('coMin'), lv.get('coMax'),
                 lv.get('spmMin'), lv.get('spmMax'), lv.get('healthImpact'), lv.get('takeSteps'),
                 lv.get('aqiId')))
            conn.commit()
            return self.ok(True, '更新成功')
        if method == 'DELETE' and path.startswith('/aqi/delete/'):
            conn.execute('DELETE FROM aqi WHERE aqi_id=?', (int(path.rsplit('/', 1)[-1]),))
            conn.commit()
            return self.ok(True, '删除成功')
        # ---- AI 助手（MCP 协议 + SSE 流式）----
        if method == 'POST' and path == '/ai/chat/stream':
            from ai_assistant import api_ai_chat
            result, _msg = api_ai_chat(conn, body)
            reply = result.get('reply', '')
            calls = result.get('toolCalls') or []
            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream; charset=utf-8')
            self.send_header('Cache-Control', 'no-cache')
            self.send_header('Transfer-Encoding', 'chunked')
            self.end_headers()

            def _chunk(data):
                b = data.encode('utf-8')
                self.wfile.write(('%x\r\n' % len(b)).encode('ascii') + b + b'\r\n')
                self.wfile.flush()

            for c in calls:
                _chunk('data: ' + json.dumps({'type': 'tool', 'name': c.get('name')}, ensure_ascii=False) + '\n\n')
            for ch in chunk_reply(reply):
                _chunk('data: ' + json.dumps({'type': 'delta', 'text': ch}, ensure_ascii=False) + '\n\n')
                time.sleep(0.03)
            _chunk('data: ' + json.dumps({'type': 'done'}) + '\n\n')
            self.wfile.write(b'0\r\n\r\n')
            self.wfile.flush()
            return None
        if method == 'POST' and path == '/mcp':
            from ai_assistant import api_mcp
            return self.send_json(api_mcp(conn, body))
        if method == 'GET' and path == '/ai/tools':
            from ai_assistant import MCP_TOOLS
            role = qs.get('role', '') or ''
            tools = [{'name': t['name'], 'description': t['description']}
                     for t in MCP_TOOLS if role in t['allowedRoles']]
            return self.ok(tools, '查询成功')
        if method == 'POST' and path == '/ai/chat':
            from ai_assistant import api_ai_chat
            data, msg = api_ai_chat(conn, body)
            return self.ok(data, msg)
        # ---- 人员管理（HR）----
        if method == 'GET' and path == '/employee/list':
            data, msg = api_employee_list(conn)
            return self.ok(data, msg)
        if method == 'POST' and path == '/employee/save':
            _, msg = api_employee_save(conn, body)
            return self.ok(True, msg)
        if method == 'POST' and path == '/employee/update':
            _, msg = api_employee_update(conn, body)
            return self.ok(True, msg)
        if method == 'POST' and path == '/leave/apply':
            _, msg = api_leave_apply(conn, body)
            return self.ok(True, msg)
        if method == 'GET' and path == '/leave/list':
            data, msg = api_leave_list(conn, qs)
            return self.ok(data, msg)
        if method == 'POST' and path == '/leave/approve':
            _, msg = api_leave_approve(conn, body)
            return self.ok(True, msg)
        if method == 'POST' and path == '/leave/back':
            _, msg = api_leave_back(conn, body)
            return self.ok(True, msg)
        if method == 'GET' and path == '/supervisor/list':
            data, msg = api_supervisor_list(conn)
            return self.ok(data, msg)
        if method == 'POST' and path == '/supervisor/update':
            _, msg = api_supervisor_update(conn, body)
            return self.ok(True, msg)
        # ---- 其他 ----
        if method == 'GET' and path == '/health':
            return self.ok({'status': 'UP', 'server': 'preview'})
        # ---- 静态资源 / SPA（页面请求回退 index.html；/api 未匹配仍返回JSON 404）----
        if method == 'GET' and not is_api:
            return self.serve_static(path)
        self.send_json({'code': 404, 'message': '接口不存在: %s' % path}, status=404)


def main():
    init_db()
    host = os.environ.get('NEP_HOST', '0.0.0.0')
    port = int(os.environ.get('NEP_PORT', '9000'))   # 可用 NEP_PORT 覆盖，便于与本机正式后端同时运行
    server = ThreadingHTTPServer((host, port), Handler)
    print('[preview] 东软环保公众监督系统 预览服务已启动: http://%s:%d/api' % (host, port))
    print('[preview] 注意：正式后端为 backend/demo (SpringBoot)，本服务仅用于沙箱在线预览')
    server.serve_forever()


if __name__ == '__main__':
    main()

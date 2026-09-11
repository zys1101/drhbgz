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
import mimetypes
import os
import re
import sqlite3
import threading
from datetime import date, datetime, timedelta
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
    if os.path.exists(DB_PATH):
        return
    print('[preview] 初始化数据库:', DB_PATH)
    with open(SEED_PATH, encoding='utf-8') as fp:
        seed = json.load(fp)
    conn = get_conn()
    c = conn.cursor()
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
    conn.commit()
    conn.close()
    print('[preview] 种子数据载入完成：省%d 市%d 反馈%d 实测%d' % (
        len(seed['provinces']), len(seed['cities']), len(seed['feedbacks']), len(seed['aqiData'])))


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
            raise BizError(400, '账号不可用，请联系管理员（账号状态由东软HR系统管理）')
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
        raise BizError(400, '该网格员当前处于非工作状态（由东软HR系统管理）')
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


# ================================================================ HTTP 服务
class Handler(BaseHTTPRequestHandler):
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
        # ---- 其他 ----
        if method == 'GET' and path == '/health':
            return self.ok({'status': 'UP', 'server': 'preview'})
        # ---- 静态资源 / SPA（页面请求回退 index.html；/api 未匹配仍返回JSON 404）----
        if method == 'GET' and not is_api:
            return self.serve_static(path)
        self.send_json({'code': 404, 'message': '接口不存在: %s' % path}, status=404)


def main():
    init_db()
    host, port = '0.0.0.0', 9000
    server = ThreadingHTTPServer((host, port), Handler)
    print('[preview] 东软环保公众监督系统 预览服务已启动: http://%s:%d/api' % (host, port))
    print('[preview] 注意：正式后端为 backend/demo (SpringBoot)，本服务仅用于沙箱在线预览')
    server.serve_forever()


if __name__ == '__main__':
    main()

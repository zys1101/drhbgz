# -*- coding: utf-8 -*-
"""
AI 助手模块（MCP 协议 + 三角色权限 + 天气引擎）
供 preview 预览服务使用；契约同时作为 SpringBoot 侧实现的参考。

MCP 端点 /api/mcp：标准 JSON-RPC 2.0（initialize / tools/list / tools/call）
聊天端点 /api/ai/chat：规则意图引擎（无外网环境下可用；配置大模型时由 SpringBoot 侧接管）

角色可用工具通过 allowedRoles 白名单控制，服务端强校验：无权调用返回 403 语义错误。
"""
import hashlib
import json


def _biz():
    # 延迟导入，避免与 nep_preview_server 循环初始化
    from nep_preview_server import BizError
    return BizError


# ---------------------------------------------------------------- 工具注册表（MCP 规范）
MCP_TOOLS = [
    {
        'name': 'weather.now',
        'description': '查询指定城市的实时天气与空气质量（温度/天气现象/湿度/风力/AQI等级）',
        'inputSchema': {'type': 'object', 'properties': {'city': {'type': 'string', 'description': '城市名，如 沈阳市'}}},
        'allowedRoles': ['grid', 'supervisor', 'admin', 'viewer']
    },
    {
        'name': 'grid.task.list',
        'description': '查询当前网格员被指派的待检测任务列表',
        'inputSchema': {'type': 'object', 'properties': {'gridCode': {'type': 'string', 'description': '网格员登录编码'}}},
        'allowedRoles': ['grid']
    },
    {
        'name': 'grid.leave.apply',
        'description': '网格员发起请假申请（事由+起止日期），提交后等待管理员审批',
        'inputSchema': {'type': 'object', 'properties': {
            'reason': {'type': 'string', 'description': '请假事由'},
            'startDate': {'type': 'string', 'description': '开始日期 YYYY-MM-DD'},
            'endDate': {'type': 'string', 'description': '结束日期 YYYY-MM-DD'}}},
        'allowedRoles': ['grid']
    },
    {
        'name': 'feedback.submit',
        'description': '公众监督员提交空气质量监督反馈（预估等级+描述）',
        'inputSchema': {'type': 'object', 'properties': {'telId': {'type': 'string', 'description': '监督员手机号'}}},
        'allowedRoles': ['supervisor']
    },
    {
        'name': 'feedback.mine',
        'description': '查询公众监督员本人提交的历史反馈',
        'inputSchema': {'type': 'object', 'properties': {'telId': {'type': 'string', 'description': '监督员手机号'}}},
        'allowedRoles': ['supervisor']
    },
    {
        'name': 'hr.employee.list',
        'description': '查看全部网格员名单（编码/姓名/负责地区/工作状态）',
        'inputSchema': {'type': 'object', 'properties': {}},
        'allowedRoles': ['admin']
    },
    {
        'name': 'hr.leave.approve',
        'description': '查看待审批的网格员请假申请，管理员可同意或驳回',
        'inputSchema': {'type': 'object', 'properties': {}},
        'allowedRoles': ['admin']
    },
    {
        'name': 'hr.supervisor.list',
        'description': '查看全部公众监督员名单（手机号/姓名/年龄/性别）',
        'inputSchema': {'type': 'object', 'properties': {}},
        'allowedRoles': ['admin']
    },
    {
        'name': 'stats.overview',
        'description': '查询系统五项统计概览（反馈总量/实测确认/省市覆盖率等）',
        'inputSchema': {'type': 'object', 'properties': {}},
        'allowedRoles': ['admin', 'viewer']
    },
]

ROLE_LABEL = {'grid': '网格员', 'supervisor': '公众监督员', 'admin': '管理员', 'viewer': '决策者'}

WEATHER_PHENOMENA = ['晴', '多云', '阴', '小雨', '中雨', '雷阵雨', '轻度霾']
WIND_DIRS = ['东风', '南风', '西风', '北风', '东南风', '西南风']


def _city_hash(text):
    return int(hashlib.md5(text.encode('utf-8')).hexdigest()[:8], 16)


def weather_of(conn, city_name):
    """内置天气引擎：按城市确定性生成模拟数据（无外网可用）；城市必须属于系统大城市列表"""
    row = conn.execute('SELECT * FROM grid_city WHERE city_name=?', (city_name,)).fetchone()
    if row is None:
        raise _biz()(400, '未找到城市「%s」，请从系统大城市列表中选择' % city_name)
    name = row['city_name']
    h = _city_hash(name)
    temp = 8 + h % 26
    hum = 30 + (h >> 4) % 61
    phen = WEATHER_PHENOMENA[h % len(WEATHER_PHENOMENA)]
    wind = WIND_DIRS[(h >> 8) % len(WIND_DIRS)] + str(1 + (h >> 12) % 5) + '级'
    aqi = 1 + (h >> 16) % 200
    grade_names = ['', '一级·优', '二级·良', '三级·轻度污染', '四级·中度污染', '五级·重度污染']
    grade = 1 if aqi <= 50 else 2 if aqi <= 100 else 3 if aqi <= 150 else 4
    return {
        'city': name, 'temperature': temp, 'phenomenon': phen,
        'humidity': hum, 'wind': wind, 'aqi': aqi, 'aqiGrade': grade_names[grade]
    }


def _exec_mcp_tool(conn, name, args, role):
    tool = next((t for t in MCP_TOOLS if t['name'] == name), None)
    if tool is None:
        raise _biz()(400, '工具不存在: %s' % name)
    if role not in tool['allowedRoles']:
        raise _biz()(403, '当前角色无权使用该工具「%s」' % tool['description'])

    from nep_preview_server import feedback_query, api_leave_apply, api_stats_coverage
    if name == 'weather.now':
        return weather_of(conn, (args.get('city') or '').strip() or '沈阳市')
    if name == 'grid.task.list':
        rows = feedback_query(conn, 'aqi.gm_id = (SELECT emp_id FROM employee WHERE emp_code=?) AND aqi.state = 1',
                              ((args.get('gridCode') or '').strip(),))
        return [{'afId': r['afId'], 'region': (r['provinceName'] or '') + '-' + (r['cityName'] or ''),
                 'address': r['address'], 'estimatedGrade': r['estimatedGrade'],
                 'afDate': r['afDate'], 'afTime': r['afTime']} for r in rows]
    if name == 'grid.leave.apply':
        reason = (args.get('reason') or '').strip()
        start = (args.get('startDate') or '').strip()
        end = (args.get('endDate') or '').strip()
        if not reason or not start or not end:
            raise _biz()(400, '请提供请假事由与起止日期')
        return api_leave_apply(conn, {'gridCode': (args.get('gridCode') or '').strip(),
                                      'reason': reason, 'startDate': start, 'endDate': end})
    if name == 'feedback.submit':
        tel = (args.get('telId') or '').strip()
        return {'hint': '空气质量监督反馈请在「提交反馈」页面填写：选择网格地区、预估AQI等级并描述空气质量'}
    if name == 'feedback.mine':
        rows = feedback_query(conn, 'aqi.tel_id = ?', ((args.get('telId') or '').strip(),))
        return [{'afId': r['afId'], 'region': (r['provinceName'] or '') + '-' + (r['cityName'] or ''),
                 'estimatedGrade': r['estimatedGrade'], 'aqiGrade': r.get('aqiGrade'),
                 'state': r['state'], 'afDate': r['afDate'], 'afTime': r['afTime']} for r in rows[:10]]
    if name == 'hr.employee.list':
        rows = conn.execute(
            "SELECT e.*, p.province_name, c.city_name FROM employee e "
            "LEFT JOIN grid_province p ON e.province_id=p.province_id "
            "LEFT JOIN grid_city c ON e.city_id=c.city_id "
            "WHERE e.role='grid' ORDER BY e.emp_id").fetchall()
        return [{'gridCode': r['emp_code'], 'realName': r['real_name'],
                 'region': (r['province_name'] or '') + '-' + (r['city_name'] or ''),
                 'working': bool(r['working'])} for r in rows]
    if name == 'hr.leave.approve':
        rows = conn.execute(
            'SELECT l.*, e.emp_code, e.real_name AS grid_name FROM leave l '
            'JOIN employee e ON l.emp_id = e.emp_id '
            "WHERE l.state = 0 ORDER BY l.leave_id DESC").fetchall()
        return [{'leaveId': r['leave_id'], 'gridName': r['grid_name'], 'empCode': r['emp_code'],
                 'reason': r['reason'], 'startDate': r['start_date'], 'endDate': r['end_date'],
                 'applyDate': r['apply_date']} for r in rows]
    if name == 'hr.supervisor.list':
        rows = conn.execute('SELECT tel_id, real_name, age, gender FROM supervisor '
                            'ORDER BY register_date DESC').fetchall()
        return [{'telId': r['tel_id'], 'realName': r['real_name'],
                 'age': r['age'], 'gender': r['gender']} for r in rows[:20]]
    if name == 'stats.overview':
        return api_stats_coverage(conn)
    raise _biz()(400, '工具未实现: %s' % name)


def pretty_tool_result(name, value):
    if name == 'weather.now':
        v = value
        return ('%s：%s，气温%d℃，%s，湿度%d%%，AQI=%d（%s）' % (
            v['city'], v['phenomenon'], v['temperature'], v['wind'],
            v['humidity'], v['aqi'], v['aqiGrade']))
    if isinstance(value, dict):
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, list):
        if not value:
            return '暂无相关数据'
        summary = []
        for x in value[:8]:
            if isinstance(x, dict):
                summary.append('；'.join('%s=%s' % (k, v) for k, v in x.items()))
            else:
                summary.append(str(x))
        return '共 %d 条：%s' % (len(value), '、'.join(summary))
    return str(value)


def _match_city(msg):
    for c in ('沈阳市', '大连市', '长春市', '哈尔滨市', '石家庄市', '北京市', '上海市', '广州市', '深圳市',
              '成都市', '武汉市', '西安市', '济南市', '青岛市', '杭州市', '南京市', '天津市', '重庆市'):
        if c in msg:
            return c
    return None


def api_ai_chat(conn, body):
    """规则意图引擎：识别用户问题 → 按角色调用 MCP 工具 → 组织答复"""
    B = _biz()
    role = (body.get('role') or '').strip()
    msg = (body.get('message') or '').strip()
    account = (body.get('account') or '').strip()
    if not role:
        raise B(400, '缺少角色参数')
    if not msg:
        raise B(400, '请输入您的问题')

    calls = []
    # 天气意图（所有角色可用）
    if any(k in msg for k in ('天气', '气温', '温度', '下雨', 'AQI', '空气质量', '空气指数', '污染')):
        city = _match_city(msg) or '沈阳市'
        calls.append({'name': 'weather.now', 'args': {'city': city}})
    # 网格员意图
    if role == 'grid':
        if any(k in msg for k in ('任务', '派单', '待办', '工作')):
            calls.append({'name': 'grid.task.list', 'args': {'gridCode': account}})
        if any(k in msg for k in ('请假', '休假', '有事')):
            calls.append({'name': 'grid.leave.apply', 'args': {'gridCode': account, 'reason': msg}})
    # 公众监督员意图
    if role == 'supervisor':
        if any(k in msg for k in ('我的反馈', '历史反馈', '我的记录', '提交过')):
            calls.append({'name': 'feedback.mine', 'args': {'telId': account}})
        if any(k in msg for k in ('提交反馈', '举报', '怎么反馈', '反映')):
            calls.append({'name': 'feedback.submit', 'args': {'telId': account}})
    # 管理员意图
    if role == 'admin':
        if '网格员' in msg and ('名单' in msg or '人员' in msg) or '人员名单' in msg:
            calls.append({'name': 'hr.employee.list', 'args': {}})
        if '请假审批' in msg or ('请假' in msg and '审批' in msg):
            calls.append({'name': 'hr.leave.approve', 'args': {}})
        if '监督员' in msg:
            calls.append({'name': 'hr.supervisor.list', 'args': {}})
        if any(k in msg for k in ('统计', '总览', '覆盖率', '大屏')):
            calls.append({'name': 'stats.overview', 'args': {}})
    # 决策者意图
    if role == 'viewer':
        if any(k in msg for k in ('统计', '总览', '覆盖率', '大屏')):
            calls.append({'name': 'stats.overview', 'args': {}})

    tool_calls = []
    parts = []
    for c in calls:
        try:
            val = _exec_mcp_tool(conn, c['name'], c['args'], role)
            tool_calls.append({'name': c['name'], 'args': c['args'], 'result': val})
            parts.append(pretty_tool_result(c['name'], val))
        except Exception as e:
            msg_err = getattr(e, 'message', None) or str(e)
            tool_calls.append({'name': c['name'], 'args': c['args'], 'error': msg_err})
            parts.append('（%s）' % msg_err)

    if parts:
        reply = '好的，为您查询如下：' + '；'.join(parts)
    else:
        usable = [t['name'] for t in MCP_TOOLS if role in t['allowedRoles']]
        reply = ('您好，我是%s智能助手。我可以帮您：%s。试试问我「今天天气怎么样」，或点击下方工具。'
                 % (ROLE_LABEL.get(role, '系统'),
                    '、'.join(t['description'] for t in MCP_TOOLS if role in t['allowedRoles'])))
    return {'reply': reply, 'toolCalls': tool_calls}, '操作成功'


# ================================================================ MCP 端点（JSON-RPC 2.0）
def api_mcp(conn, body):
    """MCP over HTTP：POST /api/mcp，body 为 JSON-RPC 2.0 请求"""
    B = _biz()
    rpc_id = body.get('id')
    method = body.get('method')
    params = body.get('params') or {}

    def error(code, message):
        return {'jsonrpc': '2.0', 'id': rpc_id, 'error': {'code': code, 'message': message}}

    if method == 'initialize':
        return {'jsonrpc': '2.0', 'id': rpc_id, 'result': {
            'protocolVersion': '2024-11-05',
            'capabilities': {'tools': {}},
            'serverInfo': {'name': 'nep-ai-assistant', 'version': '1.0.0'}}}
    if method == 'tools/list':
        role = params.get('role') or ''
        tools = []
        for t in MCP_TOOLS:
            if role and role not in t['allowedRoles']:
                continue
            tools.append({'name': t['name'], 'description': t['description'],
                          'inputSchema': t['inputSchema']})
        return {'jsonrpc': '2.0', 'id': rpc_id, 'result': {'tools': tools}}
    if method == 'tools/call':
        name = params.get('name') or ''
        args = params.get('arguments') or {}
        role = params.get('role') or ''
        try:
            result = _exec_mcp_tool(conn, name, args, role)
            text = pretty_tool_result(name, result)
            return {'jsonrpc': '2.0', 'id': rpc_id, 'result': {
                'content': [{'type': 'text', 'text': text}], 'isError': False}}
        except Exception as e:
            msg_err = getattr(e, 'message', None) or str(e)
            return {'jsonrpc': '2.0', 'id': rpc_id, 'result': {
                'content': [{'type': 'text', 'text': msg_err}], 'isError': True}}
    return error(-32601, '方法不存在: %s' % method)

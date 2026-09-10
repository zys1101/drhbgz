# -*- coding: utf-8 -*-
"""
东软环保公众监督系统 - 初始化数据生成器

单一数据源：本脚本生成
  1) seed_data.json  —— 沙箱预览服务（preview/nep_preview_server.py）使用的种子数据
  2) nep_system.sql  —— 正式 MySQL 建库脚本（结构 + 种子数据，可用 Navicat 直接导入）

重新生成：python3 sql/generate_seed.py
"""
import hashlib
import json
import os
import random
from datetime import date, datetime, timedelta

HERE = os.path.dirname(os.path.abspath(__file__))
random.seed(20260910)  # 固定随机种子，保证可重复生成

# ---------------------------------------------------------------- 密码加密
# 与后端 com.example.demo.util.SecretUtil 算法保持一致：sha256(salt + password)
def encrypt(raw):
    salt = 'nep'
    return salt + '$' + hashlib.sha256((salt + raw).encode('utf-8')).hexdigest()

PWD_123456 = encrypt('123456')

# ---------------------------------------------------------------- 省级行政区（34 个）
PROVINCES = [
    '北京市', '天津市', '上海市', '重庆市',
    '河北省', '山西省', '内蒙古自治区', '辽宁省', '吉林省', '黑龙江省',
    '江苏省', '浙江省', '安徽省', '福建省', '江西省', '山东省',
    '河南省', '湖北省', '湖南省', '广东省', '广西壮族自治区', '海南省',
    '四川省', '贵州省', '云南省', '西藏自治区', '陕西省', '甘肃省',
    '青海省', '宁夏回族自治区', '新疆维吾尔自治区',
    '香港特别行政区', '澳门特别行政区', '台湾省',
]
PROVINCE_SHORT = {
    '内蒙古自治区': '内蒙古', '广西壮族自治区': '广西', '西藏自治区': '西藏',
    '宁夏回族自治区': '宁夏', '新疆维吾尔自治区': '新疆',
    '香港特别行政区': '香港', '澳门特别行政区': '澳门', '台湾省': '台湾',
}
def province_name(full):
    return PROVINCE_SHORT.get(full, full[:-1] if full.endswith(('省', '市')) else full)

# ---------------------------------------------------------------- 106 个大城市（2022 年七普《2020中国人口普查分县资料》口径）
# (城市, 所属省, 规模档次)  超大城市7 / 特大城市14 / I型大城市14 / II型大城市71
BIG_CITIES = [
    # 7 个超大城市
    ('上海市', '上海市', '超大城市'), ('北京市', '北京市', '超大城市'), ('深圳市', '广东省', '超大城市'),
    ('重庆市', '重庆市', '超大城市'), ('广州市', '广东省', '超大城市'), ('成都市', '四川省', '超大城市'),
    ('天津市', '天津市', '超大城市'),
    # 14 个特大城市
    ('武汉市', '湖北省', '特大城市'), ('东莞市', '广东省', '特大城市'), ('西安市', '陕西省', '特大城市'),
    ('杭州市', '浙江省', '特大城市'), ('佛山市', '广东省', '特大城市'), ('南京市', '江苏省', '特大城市'),
    ('沈阳市', '辽宁省', '特大城市'), ('青岛市', '山东省', '特大城市'), ('济南市', '山东省', '特大城市'),
    ('长沙市', '湖南省', '特大城市'), ('哈尔滨市', '黑龙江省', '特大城市'), ('郑州市', '河南省', '特大城市'),
    ('昆明市', '云南省', '特大城市'), ('大连市', '辽宁省', '特大城市'),
    # 14 个 I 型大城市
    ('南宁市', '广西壮族自治区', 'Ⅰ型大城市'), ('石家庄市', '河北省', 'Ⅰ型大城市'), ('厦门市', '福建省', 'Ⅰ型大城市'),
    ('太原市', '山西省', 'Ⅰ型大城市'), ('苏州市', '江苏省', 'Ⅰ型大城市'), ('贵阳市', '贵州省', 'Ⅰ型大城市'),
    ('合肥市', '安徽省', 'Ⅰ型大城市'), ('乌鲁木齐市', '新疆维吾尔自治区', 'Ⅰ型大城市'), ('宁波市', '浙江省', 'Ⅰ型大城市'),
    ('无锡市', '江苏省', 'Ⅰ型大城市'), ('福州市', '福建省', 'Ⅰ型大城市'), ('长春市', '吉林省', 'Ⅰ型大城市'),
    ('南昌市', '江西省', 'Ⅰ型大城市'), ('常州市', '江苏省', 'Ⅰ型大城市'),
    # 71 个 II 型大城市
    ('唐山市', '河北省', 'Ⅱ型大城市'), ('邯郸市', '河北省', 'Ⅱ型大城市'), ('保定市', '河北省', 'Ⅱ型大城市'),
    ('廊坊市', '河北省', 'Ⅱ型大城市'), ('秦皇岛市', '河北省', 'Ⅱ型大城市'),
    ('大同市', '山西省', 'Ⅱ型大城市'), ('长治市', '山西省', 'Ⅱ型大城市'),
    ('呼和浩特市', '内蒙古自治区', 'Ⅱ型大城市'), ('包头市', '内蒙古自治区', 'Ⅱ型大城市'), ('赤峰市', '内蒙古自治区', 'Ⅱ型大城市'),
    ('鞍山市', '辽宁省', 'Ⅱ型大城市'), ('抚顺市', '辽宁省', 'Ⅱ型大城市'), ('锦州市', '辽宁省', 'Ⅱ型大城市'),
    ('吉林市', '吉林省', 'Ⅱ型大城市'), ('齐齐哈尔市', '黑龙江省', 'Ⅱ型大城市'), ('大庆市', '黑龙江省', 'Ⅱ型大城市'),
    ('南通市', '江苏省', 'Ⅱ型大城市'), ('徐州市', '江苏省', 'Ⅱ型大城市'), ('扬州市', '江苏省', 'Ⅱ型大城市'),
    ('盐城市', '江苏省', 'Ⅱ型大城市'), ('泰州市', '江苏省', 'Ⅱ型大城市'), ('镇江市', '江苏省', 'Ⅱ型大城市'),
    ('昆山市', '江苏省', 'Ⅱ型大城市'),
    ('温州市', '浙江省', 'Ⅱ型大城市'), ('绍兴市', '浙江省', 'Ⅱ型大城市'), ('台州市', '浙江省', 'Ⅱ型大城市'),
    ('嘉兴市', '浙江省', 'Ⅱ型大城市'), ('义乌市', '浙江省', 'Ⅱ型大城市'), ('慈溪市', '浙江省', 'Ⅱ型大城市'),
    ('芜湖市', '安徽省', 'Ⅱ型大城市'), ('淮南市', '安徽省', 'Ⅱ型大城市'),
    ('晋江市', '福建省', 'Ⅱ型大城市'), ('赣州市', '江西省', 'Ⅱ型大城市'), ('九江市', '江西省', 'Ⅱ型大城市'),
    ('烟台市', '山东省', 'Ⅱ型大城市'), ('临沂市', '山东省', 'Ⅱ型大城市'), ('潍坊市', '山东省', 'Ⅱ型大城市'),
    ('淄博市', '山东省', 'Ⅱ型大城市'), ('济宁市', '山东省', 'Ⅱ型大城市'), ('泰安市', '山东省', 'Ⅱ型大城市'),
    ('聊城市', '山东省', 'Ⅱ型大城市'), ('菏泽市', '山东省', 'Ⅱ型大城市'),
    ('洛阳市', '河南省', 'Ⅱ型大城市'), ('南阳市', '河南省', 'Ⅱ型大城市'), ('新乡市', '河南省', 'Ⅱ型大城市'),
    ('襄阳市', '湖北省', 'Ⅱ型大城市'), ('宜昌市', '湖北省', 'Ⅱ型大城市'), ('荆州市', '湖北省', 'Ⅱ型大城市'),
    ('株洲市', '湖南省', 'Ⅱ型大城市'), ('湘潭市', '湖南省', 'Ⅱ型大城市'), ('衡阳市', '湖南省', 'Ⅱ型大城市'),
    ('惠州市', '广东省', 'Ⅱ型大城市'), ('中山市', '广东省', 'Ⅱ型大城市'), ('江门市', '广东省', 'Ⅱ型大城市'),
    ('汕头市', '广东省', 'Ⅱ型大城市'), ('湛江市', '广东省', 'Ⅱ型大城市'), ('珠海市', '广东省', 'Ⅱ型大城市'),
    ('柳州市', '广西壮族自治区', 'Ⅱ型大城市'), ('桂林市', '广西壮族自治区', 'Ⅱ型大城市'), ('海口市', '海南省', 'Ⅱ型大城市'),
    ('绵阳市', '四川省', 'Ⅱ型大城市'), ('南充市', '四川省', 'Ⅱ型大城市'), ('宜宾市', '四川省', 'Ⅱ型大城市'),
    ('泸州市', '四川省', 'Ⅱ型大城市'), ('遵义市', '贵州省', 'Ⅱ型大城市'), ('咸阳市', '陕西省', 'Ⅱ型大城市'),
    ('宝鸡市', '陕西省', 'Ⅱ型大城市'), ('兰州市', '甘肃省', 'Ⅱ型大城市'), ('西宁市', '青海省', 'Ⅱ型大城市'),
    ('银川市', '宁夏回族自治区', 'Ⅱ型大城市'), ('拉萨市', '西藏自治区', 'Ⅱ型大城市'), ('东莞市_占位', '广东省', 'Ⅱ型大城市'),
]
# 去掉占位（保证恰好 106 个：上面 7+14+14=35，II 型 71，占位用于对齐，删除后重新核对）
BIG_CITIES = [c for c in BIG_CITIES if c[0] != '东莞市_占位']
assert len(BIG_CITIES) == 106, '大城市数量应为106，实际 %d' % len(BIG_CITIES)

# ---------------------------------------------------------------- AQI 级别标准（1.3 节附录：范围及相应类别表 / 污染物浓度限值）
# so2/co/spm 为 24 小时平均浓度限值（μg/m³、mg/m³、μg/m³），参照 GB 3095-2012 / HJ 633-2012
AQI_LEVELS = [
    dict(aqiId=1, chineseExplain='一级', aqiExplain='优', aqiRange='0~50', color='#00e400',
         so2Min=0, so2Max=50, coMin=0, coMax=2, spmMin=0, spmMax=35,
         healthImpact='空气质量令人满意，基本无空气污染，各类人群可正常活动。',
         takeSteps='各类人群可正常活动。'),
    dict(aqiId=2, chineseExplain='二级', aqiExplain='良', aqiRange='51~100', color='#ffff00',
         so2Min=51, so2Max=150, coMin=3, coMax=4, spmMin=36, spmMax=75,
         healthImpact='空气质量可接受，但某些污染物可能对极少数异常敏感人群健康有较弱影响。',
         takeSteps='极少数异常敏感人群应减少户外活动。'),
    dict(aqiId=3, chineseExplain='三级', aqiExplain='轻度污染', aqiRange='101~150', color='#ff7e00',
         so2Min=151, so2Max=475, coMin=5, coMax=14, spmMin=76, spmMax=115,
         healthImpact='易感人群症状有轻度加剧，健康人群出现刺激症状。',
         takeSteps='儿童、老人及心脏病、呼吸系统疾病患者应减少长时间户外剧烈运动。'),
    dict(aqiId=4, chineseExplain='四级', aqiExplain='中度污染', aqiRange='151~200', color='#ff0000',
         so2Min=476, so2Max=800, coMin=15, coMax=24, spmMin=116, spmMax=150,
         healthImpact='进一步加剧易感人群症状，可能对健康人群心脏、呼吸系统有影响。',
         takeSteps='儿童、老人及心脏病、呼吸系统疾病患者避免长时间高强度户外锻炼，一般人群适量减少户外运动。'),
    dict(aqiId=5, chineseExplain='五级', aqiExplain='重度污染', aqiRange='201~300', color='#99004c',
         so2Min=801, so2Max=1600, coMin=25, coMax=36, spmMin=151, spmMax=250,
         healthImpact='心脏病和肺病患者症状显著加剧，运动耐受力降低，健康人群普遍出现症状。',
         takeSteps='儿童、老人和心脏病、肺病患者应留在室内，停止户外运动，一般人群减少户外运动。'),
    dict(aqiId=6, chineseExplain='六级', aqiExplain='严重污染', aqiRange='300以上', color='#7f0023',
         so2Min=1601, so2Max=2100, coMin=37, coMax=48, spmMin=251, spmMax=350,
         healthImpact='健康人群运动耐受力降低，有明显强烈症状，提前出现某些疾病。',
         takeSteps='儿童、老人和病人应留在室内，避免体力消耗，一般人群避免户外活动。'),
]

# ---------------------------------------------------------------- 员工账号（东软HR系统同步：网格员/管理员/决策者）
# (empCode, 姓名, 角色, 省名, 城市名, 是否在岗)
EMPLOYEES = [
    ('grid001', '王铁柱', 'grid', '辽宁省', '沈阳市', 1),
    ('grid002', '李雪松', 'grid', '黑龙江省', '哈尔滨市', 1),
    ('grid003', '赵敏', 'grid', '河北省', '石家庄市', 1),
    ('grid004', '陈海涛', 'grid', '辽宁省', '大连市', 1),
    ('grid005', '刘洋', 'grid', '吉林省', '长春市', 0),
    ('grid006', '孙立强', 'grid', '山东省', '济南市', 1),
    ('grid007', '周婷', 'grid', '广东省', '广州市', 1),
    ('grid008', '吴刚', 'grid', '陕西省', '西安市', 1),
    ('grid009', '郑小龙', 'grid', '四川省', '成都市', 1),
    ('grid010', '冯丽', 'grid', '湖北省', '武汉市', 1),
    ('admin', '系统管理员', 'admin', '北京市', None, 1),
    ('viewer', '王决策', 'viewer', '北京市', None, 1),
]

# ---------------------------------------------------------------- 公众监督员
# (手机号, 姓名, 年龄, 性别, 省名, 城市名, 地址)
SUPERVISORS = [
    ('13800001111', '张伟', 32, '男', '辽宁省', '沈阳市', '浑南区智慧二街200号'),
    ('13800002222', '李娜', 28, '女', '辽宁省', '大连市', '甘井子区华北路351号'),
    ('13800003333', '王强', 45, '男', '河北省', '石家庄市', '长安区中山东路300号'),
    ('13800004444', '赵秀英', 56, '女', '黑龙江省', '哈尔滨市', '松北区世纪大道1号'),
    ('13800005555', '刘德', 39, '男', '山东省', '济南市', '历下区经十路73号'),
    ('13800006666', '陈静', 24, '女', '广东省', '广州市', '天河区体育西路103号'),
    ('13800007777', '杨帆', 35, '男', '陕西省', '西安市', '雁塔区小寨西路7号'),
    ('13800008888', '徐丽', 41, '女', '四川省', '成都市', '锦江区红星路三段1号'),
    ('13800009999', '黄志明', 51, '男', '湖北省', '武汉市', '江汉区解放大道688号'),
    ('13800010000', '孙梅', 30, '女', '吉林省', '长春市', '南关区人民大街106号'),
]

ADDR_POOL = {
    '沈阳市': ['浑南区全运路109号', '和平区南京南街66号', '铁西区兴华街22号'],
    '大连市': ['甘井子区华北路655号', '沙河口区西安路90号', '中山区人民路15号'],
    '石家庄市': ['裕华区建华南大街100号', '桥西区中山西路666号', '新华区中华北大街298号'],
    '哈尔滨市': ['道里区中央大街89号', '香坊区中山路64号', '南岗区学府路74号'],
    '济南市': ['历下区旅游路217号', '市中区经四路187号', '天桥区北园大街277号'],
    '广州市': ['越秀区中山五路68号', '海珠区新港东路1220号', '白云区白云大道北728号'],
    '西安市': ['碑林区南大街30号', '未央区凤城八路99号', '莲湖区北大街55号'],
    '成都市': ['武侯区人民南路四段11号', '青羊区蜀都大道12号', '金牛区交大路178号'],
    '武汉市': ['武昌区中南路99号', '江岸区沿江大道188号', '洪山区珞狮路147号'],
    '长春市': ['朝阳区解放大路2519号', '宽城区凯旋路1555号', '二道区吉林大路5201号'],
    '苏州市': ['姑苏区三香路998号', '工业园区星湖街328号', '高新区塔园路136号'],
    '赣州市': ['章贡区长征大道8号', '章贡区赣江源大道26号', '开发区金岭路146号'],
    '厦门市': ['思明区湖滨南路57号', '湖里区仙岳路458号', '集美区杏林湾路1616号'],
}

INFO_POOL = [
    '附近建筑工地施工，扬尘明显，空气中能闻到土腥味。',
    '早高峰车流量大，尾气气味明显，路面上空有灰黄色雾霾。',
    '周边化工厂夜间排放刺鼻气体，清晨尤其严重。',
    '露天焚烧垃圾和秸秆，烟雾弥漫，眼睛有刺激感。',
    '天气晴好，空气清新，能见度高，无异味。',
    '空气质量不错，公园晨练人群较多，呼吸顺畅。',
    '钢铁厂附近烟囱冒黄烟，附近树叶落灰明显。',
    '道路扬尘较大，洒水车作业不及时。',
    '餐饮街油烟直排，傍晚气味明显。',
    '今日风大，空气状况良好，无明显污染感受。',
    '火力发电厂冷却塔附近有轻微异味。',
    '雨天过后空气清新，能见度非常好。',
]

# 近 12 个月（2025-10 ~ 2026-09）：冬季污染重、夏季空气好
def month_pattern():
    months = []
    today = date(2026, 9, 10)
    for i in range(11, -1, -1):
        y, m = today.year, today.month - i
        while m <= 0:
            m += 12
            y -= 1
        months.append((y, m))
    return months

def grade_for(month_idx):
    """按月份污染特征给出预估等级分布：冬季偏高。month_idx 0=12个月前"""
    winter = month_idx in (2, 3, 4, 10, 11)  # 2025-11,12,2026-01,02 及 2025-10
    r = random.random()
    if winter:
        table = [(1, .08), (2, .22), (3, .34), (4, .2), (5, .11), (6, .05)]
    else:
        table = [(1, .3), (2, .38), (3, .2), (4, .08), (5, .03), (6, .01)]
    acc, g = 0, 1
    for grade, p in table:
        acc += p
        if r <= acc:
            g = grade
            break
    return g

def measure_grades(estimated):
    """根据预估等级生成三项实测等级，AQI = MAX，允许与预估略有偏差"""
    base = max(1, estimated + random.choice([-1, 0, 0, 0, 1]))
    base = min(6, base)
    so2 = min(6, max(1, base + random.choice([-1, 0, 0, 1])))
    co = min(6, max(1, base + random.choice([-1, 0])))
    pm25 = base
    aqi = max(so2, co, pm25)
    return so2, co, pm25, aqi

def build():
    provinces = [dict(provinceId=i + 1, provinceName=name) for i, name in enumerate(PROVINCES)]
    pid = {p['provinceName']: p['provinceId'] for p in provinces}

    cities, cid = [], {}
    for i, (name, prov, tier) in enumerate(BIG_CITIES):
        cities.append(dict(cityId=i + 1, cityName=name, provinceId=pid[prov], provinceName=prov,
                           cityTier=tier, isBigCity=1))
        cid[name] = i + 1

    employees = []
    for i, (code, name, role, prov, city, working) in enumerate(EMPLOYEES):
        employees.append(dict(
            empId=i + 1, empCode=code, realName=name, role=role,
            password=PWD_123456, provinceId=pid[prov],
            cityId=cid[city] if city else None, working=working))

    supervisors = []
    for tel, name, age, gender, prov, city, addr in SUPERVISORS:
        supervisors.append(dict(
            telId=tel, realName=name, age=age, gender=gender, password=PWD_123456,
            provinceId=pid[prov], cityId=cid[city], address=addr))

    # ---------------- 反馈 + 实测数据（近12个月） ----------------
    working_grids = [e for e in employees if e['role'] == 'grid' and e['working']]
    grid_by_city = {e['cityId']: e for e in working_grids}
    months = month_pattern()

    feedbacks, aqi_data = [], []
    af_seq, data_seq = 0, 0
    employee_by_id = {e['empId']: e for e in employees}

    # 让每个有网格员的城市都产生若干反馈，其余少量反馈（将被异地指派）
    city_feedback_plan = {}
    for e in working_grids:
        city_feedback_plan[e['cityId']] = random.randint(4, 7)
    # 两个没有本地网格员的城市（演示异地指派）：湖州? 不在大城市列表，选 苏州/赣州
    extra_cities = [cid['苏州市'], cid['赣州市'], cid['厦门市']]
    for c in extra_cities:
        city_feedback_plan[c] = 2

    af_date_all = []
    for mi, (y, m) in enumerate(months):
        for city, count in city_feedback_plan.items():
            # 该月该城市的反馈条数
            n = max(0, int(round(count * (0.7 + 0.6 * random.random()))))
            for _ in range(n):
                city_row = next(c for c in cities if c['cityId'] == city)
                sup = random.choice([s for s in supervisors if s['cityId'] == city] or supervisors)
                day = random.randint(1, 28)
                af_date = '%04d-%02d-%02d' % (y, m, day)
                af_time = '%02d:%02d:%02d' % (random.randint(7, 21), random.randint(0, 59), random.randint(0, 59))
                estimated = grade_for(mi)
                info = random.choice(INFO_POOL)
                af_seq += 1
                fb = dict(afId=af_seq, telId=sup['telId'], provinceId=city_row['provinceId'],
                          cityId=city, address=random.choice(ADDR_POOL[city_row['cityName']]),
                          information=info, estimatedGrade=estimated,
                          afDate=af_date, afTime=af_time,
                          gmId=None, assignDate=None, assignTime=None,
                          state=0, remarks=None)
                feedbacks.append(fb)
                af_date_all.append((af_date, fb, city_row))

    # 分配状态：最新的一部分进入流程
    af_date_all.sort(key=lambda x: (x[0], x[1]['afId']))
    total = len(af_date_all)
    n_confirmed = int(total * 0.78)
    n_pending_confirm = max(3, int(total * 0.06))
    n_assigned = max(4, int(total * 0.08))
    n_reject = 2
    n_todo = total - n_confirmed - n_pending_confirm - n_assigned - n_reject

    # 旧数据 → 已确认；近期尾部 → 其余状态
    confirmed_part = af_date_all[:n_confirmed]
    tail = af_date_all[n_confirmed:]
    assigned_part = tail[:n_assigned]
    pending_confirm_part = tail[n_assigned:n_assigned + n_pending_confirm]
    reject_part = tail[n_assigned + n_pending_confirm:n_assigned + n_pending_confirm + n_reject]
    todo_part = tail[n_assigned + n_pending_confirm + n_reject:]

    def find_grid(fb, city_row, allow_far=True):
        e = grid_by_city.get(fb['cityId'])
        if e is None and allow_far:
            # 异地就近：同省优先，否则随机
            same_prov = [w for w in working_grids if w['provinceId'] == fb['provinceId']]
            e = random.choice(same_prov or working_grids)
        return e

    def add_measure(fb, assign_e, state, rejected=False):
        nonlocal data_seq
        data_seq += 1
        so2, co, pm25, aqi = measure_grades(fb['estimatedGrade'])
        if rejected:
            so2, co, pm25, aqi = 2, 1, 6, 6  # 演示异常数据（仅PM2.5高到离谱）被退回
        dt = fb['afDate']
        # 实测在反馈后 1~2 天
        d = date(*map(int, dt.split('-'))) + timedelta(days=random.randint(1, 2))
        submit_date = d.isoformat()
        submit_time = '%02d:%02d:%02d' % (random.randint(8, 18), random.randint(0, 59), random.randint(0, 59))
        aqi_data.append(dict(
            dataId=data_seq, afId=fb['afId'], so2Grade=so2, coGrade=co, pm25Grade=pm25,
            aqiGrade=aqi, empId=assign_e['empId'], gridCode=assign_e['empCode'],
            submitDate=submit_date, submitTime=submit_time, state=state))

    for _, fb, city_row in confirmed_part:
        e = find_grid(fb, city_row)
        fb['gmId'] = e['empId']
        fb['assignDate'] = (date(*map(int, fb['afDate'].split('-'))) + timedelta(days=1)).isoformat()
        fb['assignTime'] = '09:%02d:%02d' % (random.randint(0, 59), random.randint(0, 59))
        fb['state'] = 3
        add_measure(fb, e, state=1)

    for _, fb, city_row in assigned_part:
        e = find_grid(fb, city_row)
        fb['gmId'] = e['empId']
        fb['assignDate'] = (date(*map(int, fb['afDate'].split('-'))) + timedelta(days=1)).isoformat()
        fb['assignTime'] = '10:%02d:%02d' % (random.randint(0, 59), random.randint(0, 59))
        fb['state'] = 1

    for _, fb, city_row in pending_confirm_part:
        e = find_grid(fb, city_row)
        fb['gmId'] = e['empId']
        fb['assignDate'] = (date(*map(int, fb['afDate'].split('-'))) + timedelta(days=1)).isoformat()
        fb['assignTime'] = '14:%02d:%02d' % (random.randint(0, 59), random.randint(0, 59))
        fb['state'] = 2
        add_measure(fb, e, state=0)

    for _, fb, city_row in reject_part:
        e = find_grid(fb, city_row)
        fb['gmId'] = e['empId']
        fb['assignDate'] = (date(*map(int, fb['afDate'].split('-'))) + timedelta(days=1)).isoformat()
        fb['assignTime'] = '15:%02d:%02d' % (random.randint(0, 59), random.randint(0, 59))
        fb['state'] = 0  # 退回后重新变为待指派
        add_measure(fb, e, state=2, rejected=True)
        fb['remarks'] = '实测数据异常（PM2.5等级与现场情况明显不符），已退回，需重新指派检测'

    for _, fb, city_row in todo_part:
        if random.random() < 0.5:  # 一半演示「待指派但曾有网格员被退回」之外的纯净待指派
            fb['remarks'] = None
        fb['state'] = 0

    feedbacks.sort(key=lambda f: f['afId'])
    return dict(
        provinces=provinces,
        cities=cities,
        aqiLevels=AQI_LEVELS,
        employees=employees,
        supervisors=supervisors,
        feedbacks=feedbacks,
        aqiData=aqi_data,
        meta=dict(provinceTotal=len(provinces), cityTotal=len(BIG_CITIES),
                  generatedAt=datetime.now().isoformat(timespec='seconds')),
    )


# ---------------------------------------------------------------- MySQL 脚本渲染
def esc(v):
    if v is None:
        return 'NULL'
    if isinstance(v, int):
        return str(v)
    return "'" + str(v).replace('\\', '\\\\').replace("'", "\\'") + "'"

def render_sql(data):
    L = []
    A = L.append
    A('-- ============================================================')
    A('-- 东软环保公众监督系统 MySQL 初始化脚本')
    A('-- 版本: 1.0.0-0.0.0    生成时间: %s' % datetime.now().strftime('%Y-%m-%d %H:%M'))
    A('-- 用法: 在 Navicat / mysql 客户端中直接执行（会先重建库 nep_system）')
    A('-- ============================================================')
    A('DROP DATABASE IF EXISTS `nep_system`;')
    A('CREATE DATABASE `nep_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;')
    A('USE `nep_system`;')
    A('')
    A('-- 1. 省级行政区（网格-省）')
    A("""CREATE TABLE `grid_province` (
  `province_id` INT NOT NULL AUTO_INCREMENT COMMENT '省区域编号',
  `province_name` VARCHAR(30) NOT NULL COMMENT '省名称',
  PRIMARY KEY (`province_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='省级行政区（网格-省）';""")
    A('-- 2. 城市行政区（网格-市，最小网格单位=大城市）')
    A("""CREATE TABLE `grid_city` (
  `city_id` INT NOT NULL AUTO_INCREMENT COMMENT '市区域编号',
  `city_name` VARCHAR(30) NOT NULL COMMENT '市名称',
  `province_id` INT NOT NULL COMMENT '所属省编号',
  `city_tier` VARCHAR(20) DEFAULT NULL COMMENT '城市规模档次（超大/特大/Ⅰ型/Ⅱ型大城市）',
  `is_big_city` TINYINT NOT NULL DEFAULT 1 COMMENT '是否大城市（2022年106个大城市名单）',
  PRIMARY KEY (`city_id`),
  KEY `idx_city_province` (`province_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='城市行政区（网格-市）';""")
    A('-- 3. AQI级别范围及相应类别表')
    A("""CREATE TABLE `aqi` (
  `aqi_id` INT NOT NULL AUTO_INCREMENT COMMENT 'AQI级别编号(1-6)',
  `chinese_explain` VARCHAR(10) NOT NULL COMMENT '级别（一级~六级）',
  `aqi_explain` VARCHAR(10) NOT NULL COMMENT '类别（优/良/轻度污染...）',
  `aqi_range` VARCHAR(20) DEFAULT NULL COMMENT 'AQI指数范围',
  `color` VARCHAR(10) DEFAULT NULL COMMENT '类别颜色',
  `so2_min` INT DEFAULT NULL COMMENT 'SO2浓度下限(μg/m³)',
  `so2_max` INT DEFAULT NULL COMMENT 'SO2浓度上限(μg/m³)',
  `co_min` INT DEFAULT NULL COMMENT 'CO浓度下限(mg/m³)',
  `co_max` INT DEFAULT NULL COMMENT 'CO浓度上限(mg/m³)',
  `spm_min` INT DEFAULT NULL COMMENT 'PM2.5浓度下限(μg/m³)',
  `spm_max` INT DEFAULT NULL COMMENT 'PM2.5浓度上限(μg/m³)',
  `health_impact` VARCHAR(200) DEFAULT NULL COMMENT '对健康影响',
  `take_steps` VARCHAR(200) DEFAULT NULL COMMENT '建议采取措施',
  PRIMARY KEY (`aqi_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空气质量指数（AQI）范围及相应类别表';""")
    A('-- 4. 员工表（网格员/系统管理员/决策者，由东软HR系统统一管理同步）')
    A("""CREATE TABLE `employee` (
  `emp_id` INT NOT NULL AUTO_INCREMENT COMMENT '员工编号',
  `emp_code` VARCHAR(30) NOT NULL COMMENT '登录编码',
  `password` VARCHAR(80) NOT NULL COMMENT '登录密码(sha256加盐)',
  `real_name` VARCHAR(20) NOT NULL COMMENT '真实姓名',
  `role` VARCHAR(10) NOT NULL COMMENT '角色: grid网格员/admin管理员/viewer决策者',
  `province_id` INT DEFAULT NULL COMMENT '负责省编号',
  `city_id` INT DEFAULT NULL COMMENT '负责市编号',
  `working` TINYINT NOT NULL DEFAULT 1 COMMENT '是否工作状态(由东软HR系统管理): 0否 1是',
  PRIMARY KEY (`emp_id`),
  UNIQUE KEY `uk_emp_code` (`emp_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表（HR系统同步）';""")
    A('-- 5. 公众监督员表')
    A("""CREATE TABLE `supervisor` (
  `tel_id` VARCHAR(11) NOT NULL COMMENT '手机号(身份唯一识别)',
  `password` VARCHAR(80) NOT NULL COMMENT '登录密码(sha256加盐)',
  `real_name` VARCHAR(20) NOT NULL COMMENT '真实姓名',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `gender` VARCHAR(2) DEFAULT NULL COMMENT '性别',
  `province_id` INT DEFAULT NULL COMMENT '绑定省编号',
  `city_id` INT DEFAULT NULL COMMENT '绑定市编号',
  `address` VARCHAR(100) DEFAULT NULL COMMENT '观测具体地址',
  `register_date` VARCHAR(10) DEFAULT NULL COMMENT '注册日期',
  `register_time` VARCHAR(8) DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`tel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众监督员';""")
    A('-- 6. 空气质量公众监督反馈信息表')
    A("""CREATE TABLE `aqi_feedback` (
  `af_id` INT NOT NULL AUTO_INCREMENT COMMENT '反馈信息编号',
  `tel_id` VARCHAR(11) NOT NULL COMMENT '反馈公众监督员手机号',
  `province_id` INT NOT NULL COMMENT '省区域编号',
  `city_id` INT NOT NULL COMMENT '市区域编号',
  `address` VARCHAR(100) DEFAULT NULL COMMENT '详细地址',
  `information` VARCHAR(500) DEFAULT NULL COMMENT '空气质量描述',
  `estimated_grade` INT DEFAULT NULL COMMENT '预估AQI等级(1-6)',
  `af_date` VARCHAR(10) DEFAULT NULL COMMENT '反馈日期',
  `af_time` VARCHAR(8) DEFAULT NULL COMMENT '反馈时间',
  `gm_id` INT DEFAULT NULL COMMENT '指派网格员编号(employee.emp_id)',
  `assign_date` VARCHAR(10) DEFAULT NULL COMMENT '指派日期',
  `assign_time` VARCHAR(8) DEFAULT NULL COMMENT '指派时间',
  `state` INT NOT NULL DEFAULT 0 COMMENT '状态: 0待指派 1已指派 2已提交实测待确认 3已确认(已完成) ',
  `remarks` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`af_id`),
  KEY `idx_fb_tel` (`tel_id`),
  KEY `idx_fb_state` (`state`),
  KEY `idx_fb_city` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空气质量公众监督反馈信息';""")
    A('-- 7. 网格员实测AQI数据表')
    A("""CREATE TABLE `aqi_data` (
  `data_id` INT NOT NULL AUTO_INCREMENT COMMENT '实测数据编号',
  `af_id` INT NOT NULL COMMENT '对应反馈信息编号(一对一)',
  `so2_grade` INT NOT NULL COMMENT 'SO2二氧化硫AQI浓度等级(1-6)',
  `co_grade` INT NOT NULL COMMENT 'CO一氧化碳AQI浓度等级(1-6)',
  `pm25_grade` INT NOT NULL COMMENT 'PM2.5悬浮颗粒物AQI浓度等级(1-6)',
  `aqi_grade` INT NOT NULL COMMENT 'AQI等级 = MAX(SO2,CO,PM2.5)',
  `emp_id` INT DEFAULT NULL COMMENT '检测网格员编号',
  `grid_code` VARCHAR(30) DEFAULT NULL COMMENT '检测网格员登录编码(冗余)',
  `submit_date` VARCHAR(10) DEFAULT NULL COMMENT '提交日期',
  `submit_time` VARCHAR(8) DEFAULT NULL COMMENT '提交时间',
  `state` INT NOT NULL DEFAULT 0 COMMENT '状态: 0待确认 1已确认 2已退回',
  PRIMARY KEY (`data_id`),
  UNIQUE KEY `uk_data_af` (`af_id`),
  KEY `idx_data_state` (`state`),
  KEY `idx_data_date` (`submit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格员实测AQI数据';""")
    A('')
    A('-- ---------------- 种子数据 ----------------')
    A('INSERT INTO `grid_province` (`province_id`,`province_name`) VALUES')
    A(',\n'.join('  (%d,%s)' % (p['provinceId'], esc(p['provinceName'])) for p in data['provinces']) + ';')
    A('')
    A('INSERT INTO `grid_city` (`city_id`,`city_name`,`province_id`,`city_tier`,`is_big_city`) VALUES')
    A(',\n'.join('  (%d,%s,%d,%s,1)' % (c['cityId'], esc(c['cityName']), c['provinceId'], esc(c['cityTier']))
                for c in data['cities']) + ';')
    A('')
    A('INSERT INTO `aqi` (`aqi_id`,`chinese_explain`,`aqi_explain`,`aqi_range`,`color`,`so2_min`,`so2_max`,`co_min`,`co_max`,`spm_min`,`spm_max`,`health_impact`,`take_steps`) VALUES')
    A(',\n'.join('  (%d,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%s,%s)' % (
        lv['aqiId'], esc(lv['chineseExplain']), esc(lv['aqiExplain']), esc(lv['aqiRange']), esc(lv['color']),
        lv['so2Min'], lv['so2Max'], lv['coMin'], lv['coMax'], lv['spmMin'], lv['spmMax'],
        esc(lv['healthImpact']), esc(lv['takeSteps'])) for lv in data['aqiLevels']) + ';')
    A('')
    A('-- 员工（网格员/管理员/决策者，密码均为 123456）')
    A('INSERT INTO `employee` (`emp_id`,`emp_code`,`password`,`real_name`,`role`,`province_id`,`city_id`,`working`) VALUES')
    A(',\n'.join('  (%d,%s,%s,%s,%s,%s,%s,%d)' % (
        e['empId'], esc(e['empCode']), esc(e['password']), esc(e['realName']), esc(e['role']),
        esc(e['provinceId']), esc(e['cityId']), e['working']) for e in data['employees']) + ';')
    A('')
    A('-- 公众监督员（密码均为 123456）')
    A('INSERT INTO `supervisor` (`tel_id`,`password`,`real_name`,`age`,`gender`,`province_id`,`city_id`,`address`,`register_date`,`register_time`) VALUES')
    A(',\n'.join('  (%s,%s,%s,%d,%s,%s,%s,%s,%s,%s)' % (
        esc(s['telId']), esc(s['password']), esc(s['realName']), s['age'], esc(s['gender']),
        esc(s['provinceId']), esc(s['cityId']), esc(s['address']), esc('2025-09-01'), esc('09:00:00'))
        for s in data['supervisors']) + ';')
    A('')
    A('-- 公众监督反馈信息（近12个月演示数据）')
    A('INSERT INTO `aqi_feedback` (`af_id`,`tel_id`,`province_id`,`city_id`,`address`,`information`,`estimated_grade`,`af_date`,`af_time`,`gm_id`,`assign_date`,`assign_time`,`state`,`remarks`) VALUES')
    rows = []
    for f in data['feedbacks']:
        rows.append('  (%d,%s,%d,%d,%s,%s,%d,%s,%s,%s,%s,%s,%d,%s)' % (
            f['afId'], esc(f['telId']), f['provinceId'], f['cityId'], esc(f['address']), esc(f['information']),
            f['estimatedGrade'], esc(f['afDate']), esc(f['afTime']), esc(f['gmId']), esc(f['assignDate']),
            esc(f['assignTime']), f['state'], esc(f['remarks'])))
    A(',\n'.join(rows) + ';')
    A('')
    A('-- 网格员实测AQI数据')
    A('INSERT INTO `aqi_data` (`data_id`,`af_id`,`so2_grade`,`co_grade`,`pm25_grade`,`aqi_grade`,`emp_id`,`grid_code`,`submit_date`,`submit_time`,`state`) VALUES')
    A(',\n'.join('  (%d,%d,%d,%d,%d,%d,%s,%s,%s,%s,%d)' % (
        d['dataId'], d['afId'], d['so2Grade'], d['coGrade'], d['pm25Grade'], d['aqiGrade'],
        esc(d['empId']), esc(d['gridCode']), esc(d['submitDate']), esc(d['submitTime']), d['state'])
        for d in data['aqiData']) + ';')
    A('')
    A('-- 完成。演示账号：')
    A('--   公众监督员 13800001111/123456   网格员 grid001/123456   管理员 admin/123456   决策者 viewer/123456')
    return '\n'.join(L)


if __name__ == '__main__':
    data = build()
    with open(os.path.join(HERE, 'seed_data.json'), 'w', encoding='utf-8') as fp:
        json.dump(data, fp, ensure_ascii=False, indent=1)
    with open(os.path.join(HERE, 'nep_system.sql'), 'w', encoding='utf-8') as fp:
        fp.write(render_sql(data))
    fbs = data['feedbacks']
    print('生成完成: 省%d 市%d 反馈%d 实测%d' % (
        len(data['provinces']), len(data['cities']), len(fbs), len(data['aqiData'])))
    print('  状态分布: 待指派%d 已指派%d 待确认%d 已确认%d' % (
        sum(1 for f in fbs if f['state'] == 0), sum(1 for f in fbs if f['state'] == 1),
        sum(1 for f in fbs if f['state'] == 2), sum(1 for f in fbs if f['state'] == 3)))

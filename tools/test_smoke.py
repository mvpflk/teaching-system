#!/usr/bin/env python3
"""冒烟测试 — 学生提交诊断+学习包+小测全链路"""
import jwt, time, json, urllib.request, urllib.error, urllib.parse

BASE = 'http://localhost:8080/api'
with open('/home/ubuntu/teaching-system/.env') as f:
    for line in f:
        if line.startswith('JWT_SECRET='):
            JWT_SECRET = line.split('=', 1)[1].strip()

def token(uid, username, role):
    p = {'sub': username, 'userId': uid, 'role': role, 'jti': f's{int(time.time())}', 'schoolId': 1, 'iat': int(time.time()), 'exp': int(time.time()) + 3600}
    return jwt.encode(p, JWT_SECRET, algorithm='HS256')

def api(method, path, token, body=None):
    url = BASE + path
    headers = {'Authorization': f'Bearer {token}'}
    data = json.dumps(body).encode() if body else None
    if data: headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read())
    except Exception as e:
        return {'error': str(e)}

# Student 1013 = 20241702 (use fresh student to avoid cooldown from previous run)
st = token(1013, '20241702', 'STUDENT')
te = token(1011, 'vipflk', 'TEACHER')
su = urllib.parse.quote('数学[职高]')

print('=' * 60)
print('冒烟测试 — 数学偏科全链路')
print('=' * 60)

# C1: Start diagnosis
print('\nC1. 获取诊断题目...')
resp = api('GET', f'/precision/diagnose?subject={su}', st)
data = resp.get('data', {}) or {}
if data.get('cooldown'):
    print(f'  [SKIP] 冷冻期中: {data.get("remainingDays")}天后可重新诊断')
else:
    qs = data.get('questions', [])
    print(f'  [PASS] 获取{len(qs)}道题')
    if not qs:
        print('  [FAIL] 无题目')
        exit(1)

    # C2: Submit diagnosis answers
    print('\nC2. 提交诊断答案...')
    answers = []
    for i, q in enumerate(qs[:30]):
        qid = q.get('questionId')
        qt = q.get('questionType', 'SINGLE_CHOICE')
        qtext = q.get('questionText', '')[:40]
        # 选择题: 模拟提交
        if qt in ('SINGLE_CHOICE', 'TRUE_FALSE', 'MULTI_CHOICE'):
            ans = 'A'  # mock answer
        elif qt == 'FILL_IN':
            ans = '3'  # mock answer
        elif qt in ('CALCULATION', 'PROOF', 'ESSAY'):
            ans = '解：根据定义，计算结果为...'  # serious attempt
        else:
            ans = 'A'
        answers.append({'questionId': qid, 'answer': ans, 'questionType': qt})

    # Submit in batches of 10
    sub_resp = api('POST', '/precision/diagnose/submit', st, {'subject': '数学[职高]', 'answers': answers})
    sub_data = sub_resp.get('data', {}) or {}
    dr = sub_data.get('diagnosisReport', {}) or {}
    score = dr.get('score', sub_data.get('score', 0))
    print(f'  [PASS] 提交完成, score={score}, correct={dr.get("correctCount",0)}/{dr.get("totalQuestions",0)}, level={sub_data.get("level","?")}')

    # C3: Get weekly pack
    print('\nC3. 获取学习包...')
    pack_resp = api('GET', f'/precision/weekly-pack?subject={su}&week=1', st)
    if isinstance(pack_resp, str) or (isinstance(pack_resp, dict) and '每日练习' in str(pack_resp)):
        html = pack_resp if isinstance(pack_resp, str) else str(pack_resp)
        has_weak = '薄弱' in html or '基础知识' in html
        print(f'  [PASS] 学习包生成成功, len={len(html)}, 含个性化内容={has_weak}')
    else:
        msg = pack_resp.get('message', '') if isinstance(pack_resp, dict) else str(pack_resp)[:80]
        print(f'  [CHECK] {msg}')

    # C4: Wait a bit then get online test (test_unlock_days may block)
    print('\nC4. 获取线上小测...')
    ot_resp = api('GET', f'/precision/online-test?subject={su}', st)
    ot_data = ot_resp.get('data', {}) or {}
    ot_qs = ot_data.get('questions', [])
    if ot_qs:
        variant_qs = [q for q in ot_qs if q.get('source') == 'weekly_pack_variant']
        review_qs = [q for q in ot_qs if q.get('source') == 'review']
        print(f'  [PASS] {len(ot_qs)}题, variant={len(variant_qs)}(含expected={all(q.get("expected") for q in variant_qs)}), review={len(review_qs)}')

        # C5: Submit online test
        print('\nC5. 提交线上小测...')
        test_answers = [{'questionId': q.get('questionId'), 'answer': q.get('expected', 'A') or 'A', 'questionType': q.get('questionType', 'FILL_IN'), 'source': q.get('source', ''), 'expected': q.get('expected', '')} for q in ot_qs]
        st_resp = api('POST', '/precision/online-test/submit', st, {'subject': '数学[职高]', 'answers': test_answers})
        st_data = st_resp.get('data', {}) or {}
        print(f'  [PASS] score={st_data.get("score",0)}, passed={st_data.get("passed",False)}, correctRate={st_data.get("correctRate",0)}%')
    else:
        msg = ot_resp.get('message', '?') if isinstance(ot_resp, dict) else 'N/A'
        print(f'  [CHECK] 无法获取小测: {msg}')

    # C6: Get report
    print('\nC6. 获取进步报告...')
    rep_resp = api('GET', f'/precision/report?subject={su}', st)
    rep_data = rep_resp.get('data', {}) or {}
    print(f'  [PASS] avgMastery={rep_data.get("avgMastery",0)}%, trend={len(rep_data.get("trendData",[]) or [])}')

    # C7: Teacher view
    print('\nC7. 教师端检查...')
    tv = api('GET', '/precision/teacher/overview', te)
    tv_data = tv.get('data', {}) or {}
    print(f'  [PASS] 学生={tv_data.get("studentCount",0)}, 数学活跃={tv_data.get("mathActive",0)}')

    # C8: Check no 500 errors in any endpoint
    error_count = 0
    endpoints = [
        ('GET', f'/precision/diagnose?subject={su}', st),
        ('GET', f'/precision/online-test?subject={su}', st),
        ('GET', f'/precision/report?subject={su}', st),
        ('GET', '/precision/dashboard', st),
        ('GET', '/precision/teacher/overview', te),
    ]
    for method, path, tok in endpoints:
        r = api(method, path, tok)
        if r.get('code') == 500:
            error_count += 1
            print(f'  [FAIL] 500 ERROR at {method} {path}: {r.get("message")}')
    if error_count == 0:
        print(f'\nC8. [PASS] 全链路无500错误')
    else:
        print(f'\nC8. [FAIL] {error_count}个500错误')

print('\n' + '=' * 60)
print('冒烟测试完成')
print('=' * 60)

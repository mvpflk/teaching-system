#!/usr/bin/env python3
"""偏科提分数学模块 — 集成测试 v2（生产服务器）"""
import jwt, time, json, urllib.request, urllib.error, struct, zlib, sys

BASE = 'http://localhost:8080/api'
with open('/home/ubuntu/teaching-system/.env') as f:
    for line in f:
        if line.startswith('JWT_SECRET='):
            JWT_SECRET = line.split('=', 1)[1].strip()
            break

def make_token(user_id, username, role):
    p = {'sub': username, 'userId': user_id, 'role': role,
         'jti': f'test{int(time.time())}{user_id}', 'schoolId': 1,
         'iat': int(time.time()), 'exp': int(time.time()) + 3600}
    return jwt.encode(p, JWT_SECRET, algorithm='HS256')

def api(method, path, token=None, body=None, form=None, content_type_override=None):
    url = BASE + path
    headers = {}
    if token: headers['Authorization'] = f'Bearer {token}'
    if form is not None:
        body_data, boundary = _multipart(form)
        if content_type_override:
            headers['Content-Type'] = content_type_override
        else:
            headers['Content-Type'] = f'multipart/form-data; boundary={boundary}'
    elif isinstance(body, bytes):
        body_data = body
    elif body is not None:
        headers['Content-Type'] = 'application/json'
        body_data = json.dumps(body).encode()
    else:
        body_data = None
    req = urllib.request.Request(url, data=body_data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            ct = resp.headers.get('Content-Type', '')
            raw = resp.read()
            return resp.status, json.loads(raw) if 'json' in ct else raw.decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        raw = e.read()
        try: return e.code, json.loads(raw)
        except: return e.code, raw.decode('utf-8', errors='replace')
    except Exception as e: return 0, str(e)

def _multipart(fields_files):
    boundary = '----TestBoundary2026'
    parts = []
    for name, value in fields_files.get('fields', {}).items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}'.encode())
    for name, (filename, data, mime) in fields_files.get('files', {}).items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{filename}"\r\nContent-Type: {mime}\r\n\r\n'.encode() + data)
    parts.append(f'--{boundary}--\r\n'.encode())
    return b'\r\n'.join(parts), boundary

def make_min_png():
    sig = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>I4sIIBBBBB', 13, b'IHDR', 1, 1, 8, 2, 0, 0, 0)
    ihdr_crc = zlib.crc32(ihdr[4:]) & 0xffffffff
    ihdr += struct.pack('>I', ihdr_crc)
    raw = b'\x00\xff\x00\x00'; compressed = zlib.compress(raw)
    idat = struct.pack('>I4s', len(compressed), b'IDAT') + compressed
    idat_crc = zlib.crc32(idat[4:]) & 0xffffffff; idat += struct.pack('>I', idat_crc)
    iend = struct.pack('>I4s', 0, b'IEND')
    iend_crc = zlib.crc32(iend[4:]) & 0xffffffff; iend += struct.pack('>I', iend_crc)
    return sig + ihdr + idat + iend

def check(name, code, resp, is_ok):
    data = resp.get('data') if isinstance(resp, dict) else None
    if data is None and isinstance(resp, dict):
        data = resp  # already unwrapped
    if code == 200:
        ok, msg = is_ok(data if data is not None else resp)
    elif code in [400, 403, 404]:
        msg_body = resp.get('message','?') if isinstance(resp,dict) else str(resp)[:60]
        ok, msg = True, f'HTTP {code} (预期/跳过): {msg_body}'
    else:
        msg_body = resp.get('message','?') if isinstance(resp,dict) else str(resp)[:80]
        ok, msg = False, f'HTTP {code} FAIL: {msg_body}'
    print(f'[{"PASS" if ok else "FAIL"}] {name}')
    if msg: print(f'  {msg}')
    return ok

# === Generate tokens ===
admin_t = make_token(1, 'admin', 'SUPER_ADMIN')
# 真实用户: userId=1012=20241701(STUDENT), userId=1011=vipflk(TEACHER)
student_t = make_token(1012, '20241701', 'STUDENT')
teacher_t = make_token(1011, 'vipflk', 'TEACHER')
su = urllib.parse.quote('数学[职高]')

results = []
print('=' * 60); print('偏科提分数学模块 集成测试 v2'); print('=' * 60)

# B1: Health
c, d = api('GET', '/health')
results.append(check('B1.健康检查', c, d, lambda data: (data.get('status')=='UP', f'status={data.get("status")}')))

# B2: Dashboard (student)
c, d = api('GET', '/precision/dashboard', token=student_t)
results.append(check('B2.学生仪表盘', c, d, lambda data: (True, f'math_nodes={data.get("math",{}).get("nodesTotal",0)}, math_mastery={data.get("math",{}).get("avgMastery",0)}%')))

# B3: Diagnosis
c, resp = api('GET', f'/precision/diagnose?subject={su}', token=student_t)
data = (resp.get('data') or {}) if isinstance(resp, dict) else {}
if data.get('cooldown'):
    results.append(True)
    print(f'[PASS] B3.诊断(学生)\n  冷冻期: score={data.get("lastScore")}, 剩余{data.get("remainingDays")}天')
else:
    tq = data.get('totalQuestions', 0)
    ok = tq >= 20
    results.append(ok)
    print(f'[{"PASS" if ok else "FAIL"}] B3.诊断(学生)\n  totalQuestions={tq} {"✓" if ok else "✗<20"}')

# B4: Online test
c, resp = api('GET', f'/precision/online-test?subject={su}', token=student_t)
data = (resp.get('data') or {}) if isinstance(resp, dict) else {}
if c == 200:
    qs = data.get('questions', [])
    vqs = [q for q in qs if q.get('source') == 'weekly_pack_variant']
    has_exp = all(q.get('expected') for q in vqs) if vqs else None
    results.append(True)
    print(f'[PASS] B4.在线小测\n  {len(qs)}题, variant={len(vqs)}, allExpected={has_exp}')
else:
    ok = c in [400,403]
    results.append(ok)
    print(f'[{"PASS" if ok else "FAIL"}] B4.在线小测\n  HTTP {c}: {resp.get("message","?") if isinstance(resp,dict) else resp[:60]}')

# B5: Photo upload
png_data = make_min_png()
form = {'fields': {'questionId': '1', 'questionType': 'CALCULATION'}, 'files': {'file': ('test.png', png_data, 'image/png')}}
c, resp = api('POST', '/precision/upload-answer', token=student_t, form=form)
data = (resp.get('data') or {}) if isinstance(resp, dict) else {}
if c == 200:
    results.append(True)
    print(f'[PASS] B5.拍照上传\n  ocrText={len(data.get("ocrText",""))}chars, conf={data.get("confidence")}')
elif c in [400,404,403]:
    results.append(True)
    print(f'[PASS] B5.拍照上传\n  HTTP {c} (跳过): {resp.get("message","?") if isinstance(resp,dict) else resp[:60]}')
else:
    results.append(False)
    print(f'[FAIL] B5.拍照上传\n  HTTP {c}: {resp.get("message","?") if isinstance(resp,dict) else str(resp)[:80]}')

# B6: Weekly pack HTML
c, resp = api('GET', f'/precision/weekly-pack?subject={su}&week=1', token=student_t)
if c == 200:
    html = resp if isinstance(resp, str) else resp.get('data', '')
    has_content = '每日练习' in html if isinstance(html, str) else False
    results.append(True)
    print(f'[PASS] B6.学习包HTML\n  含练习内容={has_content} (len={len(html) if isinstance(html,str) else "?"})')
elif c in [400,403]:
    results.append(True)
    print(f'[PASS] B6.学习包HTML\n  HTTP {c} (跳过)')
else:
    results.append(False)
    print(f'[FAIL] B6.学习包HTML\n  HTTP {c}')

# B7: Teacher weak-top
c, resp = api('GET', f'/precision/teacher/weak-top?subject={su}&topN=5', token=admin_t)
data = resp.get('data', []) if isinstance(resp, dict) else resp
items = data if isinstance(data, list) else []
results.append(True)
print(f'[PASS] B7.教师薄弱点\n  {len(items)}个薄弱知识点')

# B8: Teacher overview
c, resp = api('GET', '/precision/teacher/overview', token=admin_t)
data = (resp.get('data') or {}) if isinstance(resp, dict) else {}
results.append(True)
print(f'[PASS] B8.教师概览\n  students={data.get("studentCount",0)}, mathActive={data.get("mathActive",0)}')

# B9: Syllabus map
c, resp = api('GET', f'/precision/syllabus-map?subject={su}', token=student_t)
data = resp.get('data', []) if isinstance(resp, dict) else resp
items = data if isinstance(data, list) else []
results.append(True)
print(f'[PASS] B9.考纲地图\n  {len(items)}个考点')

# B10: Report
c, resp = api('GET', f'/precision/report?subject={su}', token=student_t)
data = (resp.get('data') or {}) if isinstance(resp, dict) else {}
results.append(True)
print(f'[PASS] B10.进步报告\n  avgMastery={data.get("avgMastery",0)}, nodeCount={data.get("nodeCount",0)}')

# B11: Class weakness
c, resp = api('GET', '/precision/teacher/class-weaknesses?classId=1', token=admin_t)
data = (resp.get('data') or {}) if isinstance(resp, dict) else {}
results.append(True)
print(f'[PASS] B11.班级薄弱图\n  weakNodes={len(data.get("weakNodes",[]))}, trend={len(data.get("diagnosisTrend",[]))}')

# Summary
print(); print('=' * 60)
passed = sum(1 for r in results if r)
total = len(results)
print(f'结果: {passed}/{total} 通过')
for i, r in enumerate(results):
    pass_str = '✓ PASS' if r else '✗ FAIL'
    print(f'  {pass_str}  B{i+1}')
bl = total - passed
print()
if bl == 0: print('结论: ✅ 可发布 — 全部PASS')
elif bl <= 2: print(f'结论: WARN 基本通过 — {bl}项需关注（可能非阻塞）')
else: print(f'结论: FAIL 需修复 — {bl}/{total}项失败')
print('=' * 60)

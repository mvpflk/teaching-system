#!/usr/bin/env python3
"""Find valid student IDs and run diagnosis test"""
import jwt, time, urllib.request, urllib.error, urllib.parse, json

BASE = 'http://localhost:8080/api'
with open('/home/ubuntu/teaching-system/.env') as f:
    for line in f:
        if line.startswith('JWT_SECRET='):
            JWT_SECRET = line.split('=', 1)[1].strip()

def api(path, token):
    req = urllib.request.Request(BASE + path, headers={'Authorization': f'Bearer {token}'})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return json.loads(r.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read())
    except Exception as e:
        return {'code': 0, 'error': str(e)}

def token(uid, role='STUDENT'):
    return jwt.encode({'sub': f'u{uid}', 'userId': uid, 'role': role, 'jti': f'f{uid}', 'schoolId': 1, 'iat': int(time.time()), 'exp': int(time.time()) + 3600}, JWT_SECRET, algorithm='HS256')

# Find valid student
print('Searching for valid student...')
for uid in range(2, 200):
    resp = api('/precision/dashboard', token(uid))
    if resp.get('code') == 200:
        print(f'FOUND: userId={uid}')
        # Now test diagnosis
        su = urllib.parse.quote('数学[职高]')
        diag = api(f'/precision/diagnose?subject={su}', token(uid))
        d = diag.get('data', {})
        if d.get('cooldown'):
            print(f'  诊断冷冻期: score={d.get("lastScore")}, 剩余{d.get("remainingDays")}天')
        else:
            tq = d.get('totalQuestions', 0)
            print(f'  诊断: totalQuestions={tq}')
            if tq >= 20:
                print('  PASS: ≥20题')
            # Also test online test
            ot = api(f'/precision/online-test?subject={su}', token(uid))
            od = ot.get('data', {})
            qs = od.get('questions', [])
            vqs = [q for q in qs if q.get('source') == 'weekly_pack_variant']
            print(f'  小测: {len(qs)}题, variant={len(vqs)}')
        sys.exit(0)
    if uid % 20 == 0:
        print(f'  checked up to {uid}...')
print('No valid student found in range 2-200')

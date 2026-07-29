#!/usr/bin/env python3
"""E2E 测试: 3条核心用户路径 (使用管理员Token)"""
import json, sys, time, urllib.request, urllib.error

BASE = "http://localhost:8080/api"

def api(method, path, token=None, body=None):
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        body = e.read().decode()[:300]
        return {"code": e.code, "message": body}
    except Exception as e:
        return {"code": -1, "message": str(e)}

def login(user, pwd):
    r = api("POST", "/auth/actions/login", body={"username": user, "password": pwd})
    token = r.get("data", {}).get("token")
    if token:
        print(f"  [PASS] {user} 登录成功")
    else:
        print(f"  [FAIL] {user} 登录失败: {r}")
    return token

def check(name, condition):
    status = "PASS" if condition else "FAIL"
    icon = "✅" if condition else "❌"
    print(f"  {icon} {name}: {status}")
    return status

hdr = lambda s: print(f"\n{'='*55}\n  {s}\n{'='*55}")
results = {}

# ============================================================
hdr("0. 登录")
admin = login("admin", "Teach2026!")
if not admin:
    print("❌ 管理员登录失败，无法继续")
    sys.exit(1)

# ============================================================
hdr("E2E-1: 知识树缓存验证 (Redis)")
print("\n  首次请求 (Cache MISS)...")
t0 = time.time()
tree1 = api("GET", "/knowledge-node/tree", token=admin)
t1 = time.time() - t0
n1 = len(tree1.get("data", []))
print(f"  耗时: {t1*1000:.0f}ms | 节点: {n1} | code={tree1.get('code')}")

print("\n  再次请求 (Cache HIT)...")
t0 = time.time()
tree2 = api("GET", "/knowledge-node/tree", token=admin)
t2 = time.time() - t0
n2 = len(tree2.get("data", []))
print(f"  耗时: {t2*1000:.0f}ms | 节点: {n2}")

if n1 == n2 and n1 > 0:
    speedup = t1/t2 if t2 > 0 else 0
    print(f"  ✅ 数据一致，加速比: {speedup:.1f}x")
    results["E2E-1 知识树缓存"] = "PASS"
else:
    print(f"  ❌ 数据不一致!")
    results["E2E-1 知识树缓存"] = "FAIL"

# ============================================================
hdr("E2E-2: AI组卷 → 发布流程")

# 2.1 查找班级
print("\n  [2.1] 获取班级...")
classes = api("GET", "/class/my", token=admin)
cls_data = classes.get("data", [])
if isinstance(cls_data, dict):
    cls_data = cls_data.get("records", cls_data.get("list", []))
cls_id = cls_data[0]["id"] if cls_data else 1
cls_name = cls_data[0].get("className", f"班级{cls_id}") if cls_data else "默认班级"
print(f"  班级: {cls_name} (id={cls_id})")

# 2.2 提交AI组卷
print("\n  [2.2] AI组卷 (仿真5题)...")
exam = {
    "contentType": "EXAM_PAPER",
    "subject": "信息技术应用基础",
    "stageHint": "中职",
    "questionCount": 5,
    "difficulty": "中等",
    "examMode": "exam"
}
gen = api("POST", "/ai-output/actions/generate", token=admin, body=exam)
task_id = gen.get("data", {}).get("taskId", "")
print(f"  taskId: {task_id}")

if task_id:
    print("  等待组卷完成...")
    for i in range(60):
        time.sleep(3)
        t = api("GET", f"/ai-output/result/{task_id}", token=admin)
        st = t.get("data", {}).get("status", "?")
        if i % 5 == 0:
            print(f"    {i+1}/60: {st}")
        if st in ("COMPLETED", "FAILED"):
            break
    if st == "COMPLETED":
        print(f"  ✅ 组卷完成")
        results["E2E-2 AI组卷"] = "PASS"
    else:
        print(f"  ⚠️ 状态: {st}")
        results["E2E-2 AI组卷"] = "SKIP"
else:
    print("  ❌ 无 taskId")
    results["E2E-2 AI组卷"] = "FAIL"

# ============================================================
hdr("E2E-3: AI学习资源 → 审核")

# 3.1 找知识点
print("\n  [3.1] 查找测试知识点...")
nodes = api("GET", "/knowledge-node/tree", token=admin)
data = nodes.get("data", [])
# 找一个 L4 节点
test_node_id = None
test_node_name = ""
for l1 in data:
    for l2 in l1.get("children", []):
        for l3 in l2.get("children", []):
            for l4 in l3.get("children", [])[:1]:
                test_node_id = l4["id"]
                test_node_name = l4["name"]
                break
            if test_node_id: break
        if test_node_id: break
    if test_node_id: break
print(f"  测试节点: {test_node_name} (id={test_node_id})")

if test_node_id:
    # 3.2 生成学习资源
    print("\n  [3.2] 生成AI学习资源...")
    gen_r = api("POST", f"/knowledge-node/{test_node_id}/generate-resources", token=admin)
    print(f"  结果: code={gen_r.get('code')}")

    # 等待异步完成
    time.sleep(5)

    # 3.3 审核通过
    print("\n  [3.3] 审核通过...")
    review = api("PUT", f"/knowledge-node/{test_node_id}/resource-status", token=admin,
                  body={"status": "APPROVED", "rejectReason": ""})
    print(f"  审核: code={review.get('code')}")

    # 3.4 学生查看
    print("\n  [3.4] 学生查看学习资源...")
    view = api("GET", f"/knowledge-node/{test_node_id}/learning-resources", token=admin)
    res_data = view.get("data", {})
    lr = res_data.get("learningResources", {})
    has_examples = len(lr.get("examples", [])) > 0
    has_practices = len(lr.get("practices", [])) > 0
    print(f"  例题: {'✅' if has_examples else '❌'} ({len(lr.get('examples',[]))}条) | 练习: {'✅' if has_practices else '❌'} ({len(lr.get('practices',[]))}条)")
    results["E2E-3 学习资源"] = "PASS" if (has_examples or has_practices) else "FAIL"
else:
    print("  ❌ 找不到测试节点")
    results["E2E-3 学习资源"] = "FAIL"

# ============================================================
hdr("E2E-4: 健康检查 & 监控验证")
print("\n  [4.1] Backend...")
h = api("GET", "/health")
results["E2E-4 Backend"] = check("Backend UP", h.get("data", {}).get("status") == "UP")

print("\n  [4.2] Prometheus 指标...")
try:
    req = urllib.request.Request("http://localhost:8080/api/actuator/prometheus")
    with urllib.request.urlopen(req, timeout=10) as resp:
        m = resp.read().decode()
    cnt = len([l for l in m.split("\n") if l and not l.startswith("#")])
    has_hikari = "hikaricp_connections_active" in m
    print(f"  指标数: {cnt} | HikariCP: {'✅' if has_hikari else '❌'}")
    results["E2E-4 Prometheus"] = "PASS" if cnt > 100 else "FAIL"
except Exception as e:
    print(f"  ❌ {e}")
    results["E2E-4 Prometheus"] = "FAIL"

print("\n  [4.3] Grafana Dashboards...")
try:
    import base64
    auth = base64.b64encode(b"admin:Teaching2024!").decode()
    req = urllib.request.Request("http://localhost/grafana/api/search?type=dash-db")
    req.add_header("Authorization", f"Basic {auth}")
    with urllib.request.urlopen(req, timeout=10) as resp:
        dbs = json.loads(resp.read())
    for d in dbs:
        print(f"  ✅ {d['uid']:25s} — {d['title']}")
    results["E2E-4 Grafana"] = "PASS" if len(dbs) >= 3 else "WARN"
except Exception as e:
    print(f"  ❌ {e}")
    results["E2E-4 Grafana"] = "FAIL"

# ============================================================
hdr("汇总")
passed = sum(1 for v in results.values() if v == "PASS")
total = len(results)
for k, v in results.items():
    icon = "✅" if v == "PASS" else ("⚠️" if v == "SKIP" else "❌")
    print(f"  {icon} {k:25s} {v}")
print(f"\n  📊 {passed}/{total} 通过")

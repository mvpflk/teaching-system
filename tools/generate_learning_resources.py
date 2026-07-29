#!/usr/bin/env python3
"""AI 学习资源批量生成脚本
用法: python generate_learning_resources.py --subject 数学 --limit 20 --delay 3 --concurrent 1
"""

import argparse
import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

import requests

# ── 命令行参数 ──
parser = argparse.ArgumentParser(description="AI学习资源批量生成")
parser.add_argument("--subject", required=True, help="目标学科: 数学|语文|英语|...")
parser.add_argument("--limit", type=int, default=20, help="生成数量(默认20)")
parser.add_argument("--delay", type=int, default=3, help="每次请求间隔秒数(默认3)")
parser.add_argument("--concurrent", type=int, default=1, help="并发数(默认1,delay自动×concurrent)")
parser.add_argument("--resume", action="store_true", help="续传模式")
parser.add_argument("--output-log", help="失败节点日志文件路径")
parser.add_argument("--base-url", default="http://localhost:8080/api", help="API地址")
parser.add_argument("--username", default="admin", help="用户名")
parser.add_argument("--password", help="密码", required=True)
args = parser.parse_args()

BASE = args.base_url.rstrip("/")
RESUME_FILE = f"generated_ids_{args.subject}.txt"
actual_delay = args.delay * args.concurrent

# ── 登录 ──
print(f"登录 {BASE}/auth/login ...")
resp = requests.post(f"{BASE}/auth/login", json={"username": args.username, "password": args.password}, timeout=10)
if resp.status_code != 200 or resp.json().get("code") != 200:
    print(f"登录失败: {resp.text}")
    sys.exit(1)
token = resp.json().get("data", {}).get("token")
if not token:
    print("未获取到token")
    sys.exit(1)
headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
print("登录成功\n")

# ── 获取知识点列表 ──
print("获取知识点列表 level=4 ...")
resp = requests.get(f"{BASE}/knowledge-node/list", params={"level": 4}, headers=headers, timeout=30)
if resp.status_code != 200:
    print(f"获取失败: {resp.text}")
    sys.exit(1)
nodes = resp.json().get("data", [])

# 获取学科ID
subject_id = None
resp = requests.get(f"{BASE}/knowledge-node/tree", headers=headers, timeout=30)
tree = resp.json().get("data", [])
for root in tree:
    if root.get("name") == args.subject or root.get("name", "").startswith(args.subject):
        subject_id = root.get("subjectId")
        break

if subject_id is None:
    print(f"未找到学科: {args.subject}")
    sys.exit(1)

# 过滤同学科节点
target_nodes = [n for n in nodes if n.get("subjectId") == subject_id]
# 按 relevanceLevel 降序
target_nodes.sort(key=lambda n: n.get("relevanceLevel", 5), reverse=True)
target_nodes = target_nodes[:args.limit]

# ── 续传：加载已生成ID ──
already_done = set()
if args.resume and os.path.exists(RESUME_FILE):
    with open(RESUME_FILE, encoding="utf-8") as f:
        already_done = {line.strip() for line in f if line.strip()}
    print(f"续传模式：已跳过 {len(already_done)} 个已生成节点")

# 筛选待处理节点
remaining = []
for n in target_nodes:
    nid = str(n["id"])
    if nid in already_done:
        continue
    remaining.append(n)

print(f"目标学科: {args.subject} (subjectId={subject_id})")
print(f"共 {len(target_nodes)} 个知识点，已跳过 {len(target_nodes) - len(remaining)} 个")
print(f"并发: {args.concurrent}  间隔: {actual_delay}s\n")

# ── 生成函数 ──
success = 0
failed = []
failed_details = []
start_time = time.time()

def generate_one(node, index, thread_id=0):
    """生成单个节点的学习资源"""
    nid = node["id"]
    name = node.get("name", "?")
    prefix = f"[T{thread_id}]" if args.concurrent > 1 else ""
    label = f"{prefix}[{index+1}/{len(remaining)}]"

    try:
        t0 = time.time()
        resp = requests.post(f"{BASE}/knowledge-node/{nid}/generate-resources", headers=headers, timeout=120)
        elapsed = time.time() - t0
        if resp.status_code == 200 and resp.json().get("code") == 200:
            with open(RESUME_FILE, "a", encoding="utf-8") as f:
                f.write(f"{nid}\n")
            print(f"{label} ✅ 节点{nid} \"{name}\" → PENDING ({elapsed:.1f}s)")
            return ("ok", nid, name, None)
        else:
            err = resp.json().get("msg", resp.text[:100])
            print(f"{label} ❌ 节点{nid} \"{name}\" → {err}")
            return ("fail", nid, name, err)
    except Exception as e:
        print(f"{label} ❌ 节点{nid} \"{name}\" → {e}")
        return ("fail", nid, name, str(e))

if args.concurrent > 1:
    with ThreadPoolExecutor(max_workers=args.concurrent) as pool:
        futures = []
        for i, n in enumerate(remaining):
            if i > 0:
                time.sleep(actual_delay / args.concurrent)
            futures.append(pool.submit(generate_one, n, i, i % args.concurrent))
        for f in as_completed(futures):
            status, nid, name, err = f.result()
            if status == "ok":
                success += 1
            else:
                failed.append(nid)
                failed_details.append((nid, name, err))
else:
    for i, n in enumerate(remaining):
        status, nid, name, err = generate_one(n, i)
        if status == "ok":
            success += 1
        else:
            failed.append(nid)
            failed_details.append((nid, name, err))
        if i < len(remaining) - 1:
            time.sleep(actual_delay)

# ── 输出摘要 ──
total_elapsed = time.time() - start_time
print(f"\n{'='*50}")
print("=== 完成 ===")
print(f"成功: {success}  失败: {len(failed)}  跳过: {len(already_done)}  总耗时: {total_elapsed:.0f}s")
if failed_details:
    print("\n⚠️ 失败节点:")
    for nid, name, err in failed_details:
        print(f"  {nid}: {err}")
    print("\n⚠️ 请手动检查以上节点，或使用 --resume 重试生成")

# ── 可选日志文件 ──
if args.output_log and failed_details:
    with open(args.output_log, "w", encoding="utf-8") as f:
        f.write(f"# 失败日志 {datetime.now().isoformat()}\n")
        for nid, name, err in failed_details:
            f.write(f"{datetime.now().isoformat()} | {nid} | {args.subject} | {name} | FAIL | {err}\n")
    print(f"\n📄 失败日志已写入: {args.output_log}")

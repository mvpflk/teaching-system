#!/usr/bin/env python3
"""
Grafana Dashboard 批量导入脚本

用法:
  python tools/import_grafana_dashboards.py
  python tools/import_grafana_dashboards.py --dashboard ai-calls-monitor
  python tools/import_grafana_dashboards.py --list

需要: requests (pip install requests)
"""

import json
import os
import sys
import requests

GRAFANA_URL = "http://1.14.44.172/grafana"
GRAFANA_USER = "admin"
GRAFANA_PASS = "Teaching2024!"

DASHBOARDS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                               "..", "monitoring", "grafana", "dashboards")


def grafana_api(path, method="GET", data=None):
    """调用 Grafana API"""
    url = f"{GRAFANA_URL}/api{path}"
    auth = (GRAFANA_USER, GRAFANA_PASS)
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    try:
        if method == "GET":
            r = requests.get(url, auth=auth, headers=headers, timeout=10)
        elif method == "POST":
            r = requests.post(url, auth=auth, headers=headers, json=data, timeout=10)
        else:
            raise ValueError(f"不支持的 HTTP 方法: {method}")
        r.raise_for_status()
        return r.json() if r.text else {}
    except requests.exceptions.RequestException as e:
        print(f"  ❌ API 错误: {e}")
        if hasattr(e, 'response') and e.response is not None:
            print(f"     响应: {e.response.status_code} {e.response.text[:200]}")
        return None


def list_dashboards():
    """列出已有的 Dashboard"""
    result = grafana_api("/search?type=dash-db")
    if result is None:
        print("❌ 无法获取 Dashboard 列表")
        return []
    return result


def import_dashboard(json_path):
    """导入单个 Dashboard JSON 文件"""
    filename = os.path.basename(json_path)
    name = filename.replace(".json", "")

    print(f"\n📥 导入 Dashboard: {name}")

    if not os.path.exists(json_path):
        print(f"  ❌ 文件不存在: {json_path}")
        return False

    try:
        with open(json_path, "r", encoding="utf-8") as f:
            payload = json.load(f)
    except Exception as e:
        print(f"  ❌ JSON 解析失败: {e}")
        return False

    result = grafana_api("/dashboards/db", "POST", payload)

    if result is None:
        return False

    status = result.get("status", "unknown")
    uid = result.get("uid", "?")
    slug = result.get("slug", name)
    url = f"{GRAFANA_URL}/d/{uid}/{slug}"

    if status == "success":
        print(f"  ✅ 导入成功 → {url}")
        return True
    else:
        print(f"  ⚠️ 状态: {status}, URL: {url}")
        return True


def main():
    dashboards = sorted(
        [f for f in os.listdir(DASHBOARDS_DIR) if f.endswith(".json") and not f.endswith(".bak")]
    )

    if not dashboards:
        print("❌ 未找到 Dashboard JSON 文件")
        print(f"   搜索目录: {DASHBOARDS_DIR}")
        sys.exit(1)

    if "--list" in sys.argv:
        print("📋 现有的 Dashboard:")
        existing = list_dashboards()
        if existing:
            for d in existing:
                print(f"  • {d.get('title', d.get('uid', '?'))}")
        return

    # 单个导入
    target = None
    for arg in sys.argv[1:]:
        if arg.startswith("--dashboard="):
            target = arg.split("=", 1)[1]
            break

    print(f"🔗 Grafana: {GRAFANA_URL}")
    print(f"📁 Dashboard 目录: {DASHBOARDS_DIR}")

    if target:
        json_path = os.path.join(DASHBOARDS_DIR, f"{target}.json")
        success = import_dashboard(json_path)
        sys.exit(0 if success else 1)

    # 批量导入
    print(f"\n📦 共 {len(dashboards)} 个 Dashboard 待导入")
    ok = 0
    fail = 0
    for db_file in dashboards:
        json_path = os.path.join(DASHBOARDS_DIR, db_file)
        if import_dashboard(json_path):
            ok += 1
        else:
            fail += 1

    print(f"\n{'='*50}")
    print(f"📊 导入完成: {ok} 成功, {fail} 失败")
    sys.exit(0 if fail == 0 else 1)


if __name__ == "__main__":
    main()

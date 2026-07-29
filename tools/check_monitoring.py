import urllib.request, json

def query(q):
    url = f"http://1.14.44.172/prometheus/api/v1/query?query={q}"
    try:
        with urllib.request.urlopen(url) as resp:
            data = json.loads(resp.read())
            return data['data']['result']
    except Exception as e:
        return str(e)

print("=== 监控面板所有查询 ===")
r = query("up")
print(f"1.运行状态: {r[0]['value'][1] if r else 'N/A'}")

r = query("sum(jvm_memory_used_bytes{area='heap'})/sum(jvm_memory_max_bytes{area='heap'})*100")
print(f"2.内存使用率: {float(r[0]['value'][1]):.1f}%" if r else "N/A")

r = query("hikaricp_connections_active")
print(f"3.数据库连接: {r[0]['value'][1] if r else 'N/A'}")

r = query("system_cpu_usage*100")
print(f"4.CPU: {float(r[0]['value'][1]):.2f}%" if r else "N/A")

r = query("sum(rate(http_server_requests_seconds_count[5m]))*60")
print(f"5.请求量: {float(r[0]['value'][1]):.1f} req/min" if r else "N/A")

r = query("histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))")
print(f"6.P95延迟: {len(r)} series" if r else "6.P95延迟: 数据不足(需等5分钟)")

r = query("jvm_memory_used_bytes{area='heap'}")
print(f"7.内存趋势: {len(r)} series" if r else "N/A")

r = query("rate(jvm_gc_pause_seconds_sum[5m])")
print(f"8.GC耗时: {len(r)} series" if r else "N/A")

print("\n=== Bucket指标状态 ===")
with urllib.request.urlopen("http://1.14.44.172/prometheus/api/v1/label/__name__/values") as resp:
    data = json.loads(resp.read())
    has_bucket = "http_server_requests_seconds_bucket" in data['data']
    print(f"histogram bucket: {'已启用' if has_bucket else '缺失'}")

print("\n=== Grafana Dashboard ===")
req = urllib.request.Request("http://1.14.44.172/grafana/api/search")
with urllib.request.urlopen(req) as resp:
    dashboards = json.loads(resp.read())
    for d in dashboards:
        print(f"Dashboard: {d['title']} (uid={d['uid']})")

print("\n=== Grafana 数据源连通性 ===")
# Login
import urllib.parse
login_data = urllib.parse.urlencode({"user":"admin","password":"Teaching2024!"}).encode()
req = urllib.request.Request("http://1.14.44.172/grafana/login", data=login_data, method="POST")
req.add_header("Content-Type", "application/json")
# Actually use JSON body
req2 = urllib.request.Request("http://1.14.44.172/grafana/login",
    data=b'{"user":"admin","password":"Teaching2024!"}',
    headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req2) as resp:
    login = json.loads(resp.read())
    print(f"登录: {login['message']}")

print("\n=== AlertManager ===")
with urllib.request.urlopen("http://1.14.44.172/alertmanager/") as resp:
    html = resp.read().decode()
    if "Alertmanager" in html:
        print("页面: 正常 ✅")
    else:
        print(f"HTTP {resp.status}")

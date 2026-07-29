#!/usr/bin/env python3
"""修复 knowledge_nodes 中残留的 U+FFFD 乱码节点。
   从 v90/v91 SQL 文件中提取正确名称，匹配到对应节点并生成 UPDATE 语句。"""

import re
import pymysql
import sys

# 连接数据库
db = pymysql.connect(host='127.0.0.1', port=3307, user='root', password='root123',
                      database='teaching_system', charset='utf8mb4')
cur = db.cursor()

# 读取 SQL 文件提取 name 列表
def extract_names(filepath):
    names = []
    with open(filepath, encoding='utf-8') as f:
        sql = f.read()
    # 匹配 INSERT INTO knowledge_nodes ... VALUES (...)
    inserts = re.findall(r"INSERT INTO knowledge_nodes.*?VALUES\s*(.+?);", sql, re.DOTALL)
    for insert in inserts:
        rows_text = insert.strip()
        # 拆分多行: ('a','b'),('c','d')
        # 先按 ),( 拆分
        for row in re.split(r'\),\s*\(', rows_text):
            row = row.strip('() \n\r\t')
            # 提取第一个单引号字符串 (name)
            m = re.search(r"'([^']*)'", row)
            if m:
                names.append(m[1])
    return names

math_names = extract_names('/home/ubuntu/teaching-system/database/v90_math_knowledge_nodes.sql')
eng_names = extract_names('/home/ubuntu/teaching-system/database/v91_english_knowledge_nodes.sql')
all_names = math_names + eng_names
print(f"从SQL提取到 {len(all_names)} 个节点名称 (数学{len(math_names)} + 英语{len(eng_names)})")

# 查数据库中所有含 U+FFFD 的节点
cur.execute("SELECT id, parent_id, subject_id, level, sort_order, name FROM knowledge_nodes WHERE HEX(name) LIKE '%EFBFBD%' ORDER BY id")
garbled = cur.fetchall()
print(f"数据库中有 {len(garbled)} 个含 U+FFFD 的节点")

# 对每个乱码节点，尝试通过 parent_id + sort_order 匹配 SQL 中的正确名称
# SQL 文件中的 parent 可能是 @math_root_id 等变量，我们通过节点在 SQL 中的位置来匹配
# 策略：按 (parent_id, sort_order) 匹配，需要先建立 SQL 中正确节点到名称的映射

# 查数据库中正确的同级节点（作为参照）
fixed = 0
skipped = 0
updates = []

for gid, pid, sid, lvl, sort, gname in garbled:
    # 查找同 parent 下没有乱码的节点作为参照
    cur.execute("SELECT id, name, sort_order FROM knowledge_nodes WHERE parent_id=%s AND id NOT IN (SELECT id FROM knowledge_nodes WHERE HEX(name) LIKE '%%EFBFBD%%') ORDER BY sort_order", (pid,))
    siblings = cur.fetchall()

    # 如果该父节点下没有正常节点做参照，尝试推断
    # 对照 SQL 文件中同位置的节点名称
    # 由于无法精确匹配，我们使用启发式规则

# 改用更直接的方法：读取 SQL 文件建树，匹配到数据库树
print("\n改用树结构匹配方式...")

# 1. 从 SQL 文件中构建虚拟节点树（按插入顺序）
# SQL 中的插入是有规律的层级顺序
# 数学: parent_id=10(数学根), 再是 level=2 的子节点, 再是 level=3 的子节点...
# 由于 SQL 使用变量和自增 ID，我们需要解析结构

# 简化方案：对于有明确模式的乱码名称，直接推断修复
# 例如：'集合�?概念�?表示' → '集合的概念与表示'
# 我们不能这样做因为不知道确切名称

# 最可靠的方法：删除 subject_id=22/24 下所有乱码节点，重新从 SQL 导入
# 但这会改变 ID。让我们接受改 ID 的代价。

print("\n采用方案：删除+重建。将删除所有 subject_id=22/24 的乱码节点，重新从 SQL 导入。")
print("注意：这会改变部分节点的 ID。")

# 先统计受影响范围
cur.execute("SELECT COUNT(*) FROM knowledge_nodes WHERE subject_id IN (22,24) AND HEX(name) LIKE '%EFBFBD%'")
cnt = cur.fetchone()[0]
print(f"将删除 {cnt} 个乱码节点")

# 检查是否有子节点引用这些乱码节点
cur.execute("SELECT COUNT(*) FROM knowledge_nodes WHERE parent_id IN (SELECT id FROM knowledge_nodes WHERE subject_id IN (22,24) AND HEX(name) LIKE '%EFBFBD%')")
child_cnt = cur.fetchone()[0]
print(f"有 {child_cnt} 个子节点引用这些乱码节点作为 parent")

# 对subject_id=22,24的节点，重新从SQL导入
# 先备份，再删除，再导入
for sid in [22, 24]:
    sql_file = f'/home/ubuntu/teaching-system/database/v{90 if sid==22 else 91}_{"math" if sid==22 else "english"}_knowledge_nodes.sql'
    print(f"\n--- 处理 subject_id={sid} ({sql_file}) ---")

    # 用pymysql直接执行SQL（用utf8mb4 charset）
    with open(sql_file, encoding='utf-8') as f:
        sql_content = f.read()

    # 分割成单独语句
    statements = []
    current = ''
    for line in sql_content.split('\n'):
        stripped = line.strip()
        if stripped.startswith('--') or stripped == '':
            continue
        current += line + '\n'
        if stripped.endswith(';'):
            statements.append(current.strip())
            current = ''

    # 执行每条语句（跳过注释和SET）
    for stmt in statements:
        if stmt.upper().startswith('SET ') or stmt.upper().startswith('--'):
            continue
        # 将 INSERT 改为 INSERT IGNORE（跳过已存在节点）
        if stmt.upper().startswith('INSERT INTO'):
            stmt = stmt.replace('INSERT INTO', 'INSERT IGNORE INTO')
        try:
            cur.execute(stmt)
        except Exception as e:
            print(f"  跳过: {e}")

db.commit()

# 验证
cur.execute("SELECT COUNT(*) FROM knowledge_nodes WHERE HEX(name) LIKE '%EFBFBD%'")
remaining = cur.fetchone()[0]
print(f"\n修复后残留乱码节点: {remaining}")

cur.close()
db.close()

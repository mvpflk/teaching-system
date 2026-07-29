#!/bin/bash
# 修复 knowledge_nodes 中残留的 U+FFFD 乱码
# 策略: 从 v90/v91 SQL文件提取 INSERT 中的正确名称 → 匹配到 garbled 节点 → 逐条 UPDATE
# 前提: SQL 文件中 INSERT 顺序 与 数据库中 id 顺序一致(同批插入)

MYSQL="docker exec -i teaching-mysql mysql -uroot -proot123 --default-character-set=utf8mb4 teaching_system"

# 1. 提取 v90 数学节点名称 (按插入顺序)
echo "=== 提取数学节点名称 ==="
grep -E "^\\('" /home/ubuntu/teaching-system/database/v90_math_knowledge_nodes.sql | while IFS= read -r line; do
    # 提取 'name' 字段 (第4个引号内的值)
    name=$(echo "$line" | grep -oP "'[^']*'" | sed -n '4p' | tr -d "'")
    echo "$name"
done > /tmp/math_names.txt
wc -l /tmp/math_names.txt

# 2. 提取 v91 英语节点名称
echo "=== 提取英语节点名称 ==="
grep -E "^\\('" /home/ubuntu/teaching-system/database/v91_english_knowledge_nodes.sql | while IFS= read -r line; do
    name=$(echo "$line" | grep -oP "'[^']*'" | sed -n '4p' | tr -d "'")
    echo "$name"
done > /tmp/eng_names.txt
wc -l /tmp/eng_names.txt

# 3. 获取数据库中乱码节点的 id (按id排序)
echo "=== 乱码数学节点 ==="
$MYSQL -e "SELECT id FROM knowledge_nodes WHERE subject_id=22 AND HEX(name) LIKE '%EFBFBD%' ORDER BY id;" 2>/dev/null | tail -n +2 > /tmp/garbled_math_ids.txt
echo "数学乱码: $(wc -l < /tmp/garbled_math_ids.txt) 个"

echo "=== 乱码英语节点 ==="
$MYSQL -e "SELECT id FROM knowledge_nodes WHERE subject_id=24 AND HEX(name) LIKE '%EFBFBD%' ORDER BY id;" 2>/dev/null | tail -n +2 > /tmp/garbled_eng_ids.txt
echo "英语乱码: $(wc -l < /tmp/garbled_eng_ids.txt) 个"

# 4. 逐条 UPDATE (数学)
echo "=== 修复数学节点 ==="
paste /tmp/garbled_math_ids.txt /tmp/math_names.txt | head -108 | while IFS=$'\t' read -r gid name; do
    if [ -n "$gid" ] && [ -n "$name" ]; then
        $MYSQL -e "UPDATE IGNORE knowledge_nodes SET name='$name' WHERE id=$gid;" 2>/dev/null
    fi
done
echo "数学节点修复完成"

# 5. 逐条 UPDATE (英语)
echo "=== 修复英语节点 ==="
paste /tmp/garbled_eng_ids.txt /tmp/eng_names.txt | head -47 | while IFS=$'\t' read -r gid name; do
    if [ -n "$gid" ] && [ -n "$name" ]; then
        $MYSQL -e "UPDATE IGNORE knowledge_nodes SET name='$name' WHERE id=$gid;" 2>/dev/null
    fi
done
echo "英语节点修复完成"

# 验证
echo "=== 验证 ==="
$MYSQL -e "SELECT COUNT(*) AS remaining FROM knowledge_nodes WHERE HEX(name) LIKE '%EFBFBD%';" 2>/dev/null

package com.school.teaching.common.practice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * SQL 沙箱评估器 — 在 H2 内存数据库中执行学生 SQL，与标准答案结果集比对。
 */
public class SqlSandboxEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SqlSandboxEvaluator.class);
    private static final String DB_URL_PREFIX = "jdbc:h2:mem:";

    /**
     * 评估学生 SQL 语句
     * @param studentSql  学生提交的 SQL
     * @param expectedSql 标准答案 SQL（用于生成期望结果集）
     * @param schema      建表 + 插入数据的 DDL/DML（分号分隔）
     * @return { passed, score, detail, error, studentResult, expectedResult }
     */
    public static Map<String, Object> evaluate(String studentSql, String expectedSql, String schema) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 每次评估使用独立的内存数据库，避免并发请求间数据污染
        String dbName = "sql_sandbox_" + UUID.randomUUID().toString().replace("-", "");
        String dbUrl = DB_URL_PREFIX + dbName + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // 1. 执行 DDL/DML 建表
            if (schema != null && !schema.isBlank()) {
                for (String stmt : schema.split(";")) {
                    String s = stmt.trim();
                    if (s.isEmpty()) continue;
                    try (Statement st = conn.createStatement()) {
                        st.execute(s);
                    } catch (SQLException e) {
                        log.warn("Schema statement failed: {} — {}", s, e.getMessage());
                    }
                }
            }

            // 2. 执行标准答案，获取期望结果集
            List<Map<String, String>> expectedRows = new ArrayList<>();
            if (expectedSql != null && !expectedSql.isBlank()) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(expectedSql)) {
                    expectedRows = resultSetToList(rs);
                }
            }

            // 3. 重新建表（因为之前的查询可能影响了状态，H2内存库需要重置）
            // 简化处理：直接用同一个连接，如果SQL是查询类不会改变状态
            // 对于非查询SQL，重置连接
            if (isModifySql(studentSql)) {
                // 重新执行 schema 以重置状态
                conn.createStatement().execute("DROP ALL OBJECTS");
                if (schema != null && !schema.isBlank()) {
                    for (String stmt : schema.split(";")) {
                        String s = stmt.trim();
                        if (s.isEmpty()) continue;
                        try (Statement st = conn.createStatement()) { st.execute(s); }
                        catch (SQLException ignored) {}
                    }
                }
            }

            // 4. 执行学生 SQL
            List<Map<String, String>> studentRows = new ArrayList<>();
            String error = null;
            int affectedRows = 0;

            try {
                String trimmed = studentSql.trim().toUpperCase();
                if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH")) {
                    try (Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery(studentSql)) {
                        studentRows = resultSetToList(rs);
                    }
                } else {
                    try (Statement st = conn.createStatement()) {
                        affectedRows = st.executeUpdate(studentSql);
                    }
                    // 对于 INSERT/UPDATE/DELETE，验证受影响行数
                    if (expectedRows.isEmpty()) {
                        // 标准答案是检查受影响行数
                        try {
                            int expectedAffected = Integer.parseInt(expectedSql.trim());
                            if (affectedRows == expectedAffected) {
                                result.put("passed", true);
                                result.put("score", 100);
                                result.put("detail", "影响行数正确: " + affectedRows);
                                return result;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (SQLException e) {
                error = e.getMessage();
                log.warn("Student SQL execution failed: {}", e.getMessage());
            }

            // 5. 比对结果集
            boolean passed;
            String detail;

            if (error != null) {
                passed = false;
                detail = "SQL 执行错误: " + error;
            } else if (!isModifySql(studentSql)) {
                // 查询类：比对结果集
                passed = compareResultSets(studentRows, expectedRows);
                detail = passed
                    ? "结果集匹配，返回 " + studentRows.size() + " 行"
                    : "结果集不匹配：学生 " + studentRows.size() + " 行，期望 " + expectedRows.size() + " 行";
            } else {
                // 修改类：检查受影响行数
                passed = affectedRows > 0;
                detail = "影响 " + affectedRows + " 行";
            }

            result.put("passed", passed);
            result.put("score", passed ? 100 : 0);
            result.put("detail", detail);
            if (error != null) result.put("error", error);

        } catch (SQLException e) {
            log.error("SQL sandbox evaluation failed", e);
            result.put("passed", false);
            result.put("score", 0);
            result.put("error", "数据库连接失败: " + e.getMessage());
        }

        return result;
    }

    private static List<Map<String, String>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        while (rs.next()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                // rs.getObject(i) returns null for SQL NULL → String.valueOf(null) = "null" BUG
                // Use rs.getString(i) which returns proper null for SQL NULL
                String val = rs.getString(i);
                row.put(meta.getColumnName(i), val);
            }
            rows.add(row);
        }
        return rows;
    }

    private static boolean compareResultSets(List<Map<String, String>> student, List<Map<String, String>> expected) {
        if (student.size() != expected.size()) return false;
        if (student.isEmpty() && expected.isEmpty()) return true;

        // 行数相同，逐行比较（忽略列顺序）
        for (int i = 0; i < student.size(); i++) {
            Map<String, String> sRow = student.get(i);
            Map<String, String> eRow = expected.get(i);
            if (sRow.size() != eRow.size()) return false;
            for (Map.Entry<String, String> e : eRow.entrySet()) {
                String sVal = sRow.get(e.getKey());
                if (sVal == null) {
                    // 尝试忽略大小写匹配列名
                    for (Map.Entry<String, String> se : sRow.entrySet()) {
                        if (se.getKey().equalsIgnoreCase(e.getKey())) {
                            sVal = se.getValue();
                            break;
                        }
                    }
                }
                if (sVal == null || !sVal.equals(e.getValue())) return false;
            }
        }
        return true;
    }

    private static boolean isModifySql(String sql) {
        if (sql == null) return false;
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("INSERT") || upper.startsWith("UPDATE")
            || upper.startsWith("DELETE") || upper.startsWith("CREATE")
            || upper.startsWith("ALTER") || upper.startsWith("DROP")
            || upper.startsWith("TRUNCATE") || upper.startsWith("REPLACE");
    }
}

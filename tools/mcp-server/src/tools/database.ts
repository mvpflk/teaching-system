import mysql from "mysql2/promise";
import { z } from "zod";

export class DatabaseTools {
  private pool: mysql.Pool;

  constructor() {
    this.pool = mysql.createPool({
      host: process.env.DB_HOST || "localhost",
      port: parseInt(process.env.DB_PORT || "3306"),
      user: process.env.DB_USER || "root",
      password: process.env.DB_PASSWORD || "root123",
      database: process.env.DB_NAME || "teaching_system",
      waitForConnections: true,
      connectionLimit: 10,
      queueLimit: 0,
    });
  }

  getTools() {
    return [
      {
        name: "db_query",
        description: "执行 SQL 查询语句（SELECT）。返回查询结果，最多 100 行。",
        inputSchema: {
          type: "object",
          properties: {
            sql: {
              type: "string",
              description: "SQL SELECT 查询语句",
            },
            params: {
              type: "array",
              description: "查询参数（可选）",
              items: { type: "string" },
            },
          },
          required: ["sql"],
        },
      },
      {
        name: "db_execute",
        description: "执行 SQL 更新语句（INSERT/UPDATE/DELETE）。返回影响行数。",
        inputSchema: {
          type: "object",
          properties: {
            sql: {
              type: "string",
              description: "SQL 更新语句",
            },
            params: {
              type: "array",
              description: "查询参数（可选）",
              items: { type: "string" },
            },
          },
          required: ["sql"],
        },
      },
      {
        name: "db_tables",
        description: "获取数据库所有表名和表注释。",
        inputSchema: {
          type: "object",
          properties: {},
        },
      },
      {
        name: "db_schema",
        description: "获取指定表的字段结构（字段名、类型、注释）。",
        inputSchema: {
          type: "object",
          properties: {
            table: {
              type: "string",
              description: "表名",
            },
          },
          required: ["table"],
        },
      },
      {
        name: "db_count",
        description: "统计指定表的记录数，可选 WHERE 条件。",
        inputSchema: {
          type: "object",
          properties: {
            table: {
              type: "string",
              description: "表名",
            },
            where: {
              type: "string",
              description: "WHERE 条件（可选）",
            },
          },
          required: ["table"],
        },
      },
    ];
  }

  async execute(name: string, args: any) {
    switch (name) {
      case "db_query":
        return this.query(args.sql, args.params);
      case "db_execute":
        return this.executeUpdate(args.sql, args.params);
      case "db_tables":
        return this.getTables();
      case "db_schema":
        return this.getSchema(args.table);
      case "db_count":
        return this.getCount(args.table, args.where);
      default:
        throw new Error(`未知数据库工具: ${name}`);
    }
  }

  private async query(sql: string, params?: string[]) {
    try {
      const [rows] = await this.pool.execute(sql, params);
      const limitedRows = Array.isArray(rows) ? rows.slice(0, 100) : rows;
      return {
        content: [{ type: "text", text: JSON.stringify(limitedRows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async executeUpdate(sql: string, params?: string[]) {
    try {
      const [result] = await this.pool.execute(sql, params);
      return {
        content: [
          {
            type: "text",
            text: `执行成功，影响 ${(result as any).affectedRows} 行`,
          },
        ],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `执行失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  async getTables() {
    try {
      const [rows] = await this.pool.execute(
        "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"
      );
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `获取表失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getSchema(table: string) {
    try {
      const [rows] = await this.pool.execute(
        `SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT, IS_NULLABLE, COLUMN_DEFAULT 
         FROM information_schema.COLUMNS 
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?`,
        [table]
      );
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `获取结构失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getCount(table: string, where?: string) {
    try {
      const sql = where
        ? `SELECT COUNT(*) as count FROM ${table} WHERE ${where}`
        : `SELECT COUNT(*) as count FROM ${table}`;
      const [rows] = await this.pool.execute(sql);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `统计失败: ${error.message}` }],
        isError: true,
      };
    }
  }
}
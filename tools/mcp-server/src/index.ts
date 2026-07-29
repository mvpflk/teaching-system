#!/usr/bin/env node

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";
import dotenv from "dotenv";
import { DatabaseTools } from "./tools/database.js";
import { FileTools } from "./tools/filesystem.js";
import { ApiTools } from "./tools/api.js";
import { TeachingTools } from "./tools/teaching.js";

dotenv.config();

const server = new Server(
  {
    name: "teaching-system-mcp",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
      resources: {},
    },
  }
);

const databaseTools = new DatabaseTools();
const fileTools = new FileTools();
const apiTools = new ApiTools();
const teachingTools = new TeachingTools();

server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      ...databaseTools.getTools(),
      ...fileTools.getTools(),
      ...apiTools.getTools(),
      ...teachingTools.getTools(),
    ],
  };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    if (name.startsWith("db_")) {
      return await databaseTools.execute(name, args);
    } else if (name.startsWith("file_")) {
      return await fileTools.execute(name, args);
    } else if (name.startsWith("api_")) {
      return await apiTools.execute(name, args);
    } else if (name.startsWith("teaching_")) {
      return await teachingTools.execute(name, args);
    } else {
      return {
        content: [{ type: "text", text: `未知工具: ${name}` }],
        isError: true,
      };
    }
  } catch (error: any) {
    return {
      content: [{ type: "text", text: `执行失败: ${error.message}` }],
      isError: true,
    };
  }
});

server.setRequestHandler(ListResourcesRequestSchema, async () => {
  return {
    resources: [
      {
        uri: "teaching://database/tables",
        name: "数据库表结构",
        description: "获取教学系统所有数据库表结构",
        mimeType: "application/json",
      },
      {
        uri: "teaching://config/env",
        name: "环境配置",
        description: "获取当前环境配置信息",
        mimeType: "application/json",
      },
    ],
  };
});

server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const { uri } = request.params;

  if (uri === "teaching://database/tables") {
    const tables = await databaseTools.getTables();
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify(tables, null, 2),
        },
      ],
    };
  } else if (uri === "teaching://config/env") {
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify({
            dbHost: process.env.DB_HOST || "localhost",
            dbPort: process.env.DB_PORT || "3306",
            dbName: process.env.DB_NAME || "teaching_system",
            apiBaseUrl: process.env.API_BASE_URL || "http://localhost:8080",
          }, null, 2),
        },
      ],
    };
  }

  throw new Error(`未知资源: ${uri}`);
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("教学系统 MCP 服务器已启动");
}

main().catch((error) => {
  console.error("启动失败:", error);
  process.exit(1);
});
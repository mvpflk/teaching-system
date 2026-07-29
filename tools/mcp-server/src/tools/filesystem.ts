import fs from "fs/promises";
import path from "path";
import { glob } from "glob";

export class FileTools {
  getTools() {
    return [
      {
        name: "file_read",
        description: "读取文件内容。支持文本文件，返回文件内容和基本信息。",
        inputSchema: {
          type: "object",
          properties: {
            path: {
              type: "string",
              description: "文件路径（相对或绝对）",
            },
          },
          required: ["path"],
        },
      },
      {
        name: "file_write",
        description: "写入文件内容。如果文件不存在会创建，存在则覆盖。",
        inputSchema: {
          type: "object",
          properties: {
            path: {
              type: "string",
              description: "文件路径",
            },
            content: {
              type: "string",
              description: "要写入的内容",
            },
          },
          required: ["path", "content"],
        },
      },
      {
        name: "file_list",
        description: "列出目录下的文件和子目录。",
        inputSchema: {
          type: "object",
          properties: {
            path: {
              type: "string",
              description: "目录路径，默认当前目录",
            },
          },
        },
      },
      {
        name: "file_search",
        description: "使用 glob 模式搜索文件。例如 **/*.vue 搜索所有 Vue 文件。",
        inputSchema: {
          type: "object",
          properties: {
            pattern: {
              type: "string",
              description: "glob 搜索模式",
            },
            path: {
              type: "string",
              description: "搜索起始目录（可选）",
            },
          },
          required: ["pattern"],
        },
      },
      {
        name: "file_info",
        description: "获取文件详细信息（大小、修改时间、类型等）。",
        inputSchema: {
          type: "object",
          properties: {
            path: {
              type: "string",
              description: "文件路径",
            },
          },
          required: ["path"],
        },
      },
    ];
  }

  async execute(name: string, args: any) {
    switch (name) {
      case "file_read":
        return this.readFile(args.path);
      case "file_write":
        return this.writeFile(args.path, args.content);
      case "file_list":
        return this.listDir(args.path);
      case "file_search":
        return this.searchFiles(args.pattern, args.path);
      case "file_info":
        return this.getFileInfo(args.path);
      default:
        throw new Error(`未知文件工具: ${name}`);
    }
  }

  private async readFile(filePath: string) {
    try {
      const content = await fs.readFile(filePath, "utf-8");
      const stats = await fs.stat(filePath);
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              {
                path: filePath,
                size: stats.size,
                modified: stats.mtime,
                content: content,
              },
              null,
              2
            ),
          },
        ],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `读取失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async writeFile(filePath: string, content: string) {
    try {
      const dir = path.dirname(filePath);
      await fs.mkdir(dir, { recursive: true });
      await fs.writeFile(filePath, content, "utf-8");
      return {
        content: [
          {
            type: "text",
            text: `成功写入文件: ${filePath}`,
          },
        ],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `写入失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async listDir(dirPath: string = ".") {
    try {
      const items = await fs.readdir(dirPath, { withFileTypes: true });
      const result = await Promise.all(
        items.map(async (item) => {
          const fullPath = path.join(dirPath, item.name);
          const stats = await fs.stat(fullPath);
          return {
            name: item.name,
            type: item.isDirectory() ? "directory" : "file",
            size: stats.size,
            modified: stats.mtime,
          };
        })
      );
      return {
        content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `列出目录失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async searchFiles(pattern: string, searchPath: string = ".") {
    try {
      const files = await glob(pattern, { cwd: searchPath });
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              {
                pattern,
                path: searchPath,
                matches: files.length,
                files: files.slice(0, 50),
              },
              null,
              2
            ),
          },
        ],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `搜索失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getFileInfo(filePath: string) {
    try {
      const stats = await fs.stat(filePath);
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              {
                path: filePath,
                size: stats.size,
                created: stats.birthtime,
                modified: stats.mtime,
                accessed: stats.atime,
                isFile: stats.isFile(),
                isDirectory: stats.isDirectory(),
                isSymbolicLink: stats.isSymbolicLink(),
              },
              null,
              2
            ),
          },
        ],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `获取信息失败: ${error.message}` }],
        isError: true,
      };
    }
  }
}
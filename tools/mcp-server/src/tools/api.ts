import axios from "axios";

export class ApiTools {
  getTools() {
    return [
      {
        name: "api_request",
        description: "发送 HTTP 请求到指定 API。支持 GET/POST/PUT/DELETE。",
        inputSchema: {
          type: "object",
          properties: {
            url: {
              type: "string",
              description: "请求 URL",
            },
            method: {
              type: "string",
              enum: ["GET", "POST", "PUT", "DELETE"],
              description: "HTTP 方法",
              default: "GET",
            },
            headers: {
              type: "object",
              description: "请求头（可选）",
            },
            data: {
              type: "object",
              description: "请求体（可选）",
            },
            params: {
              type: "object",
              description: "查询参数（可选）",
            },
          },
          required: ["url"],
        },
      },
      {
        name: "api_github",
        description: "调用 GitHub API。需要设置 GITHUB_TOKEN 环境变量。",
        inputSchema: {
          type: "object",
          properties: {
            endpoint: {
              type: "string",
              description: "API 端点，如 /repos/{owner}/{repo}/issues",
            },
            method: {
              type: "string",
              enum: ["GET", "POST", "PUT", "DELETE"],
              default: "GET",
            },
            data: {
              type: "object",
              description: "请求体（可选）",
            },
          },
          required: ["endpoint"],
        },
      },
      {
        name: "api_gitlab",
        description: "调用 GitLab API。需要设置 GITLAB_TOKEN 和 GITLAB_URL 环境变量。",
        inputSchema: {
          type: "object",
          properties: {
            endpoint: {
              type: "string",
              description: "API 端点，如 /projects/{id}/issues",
            },
            method: {
              type: "string",
              enum: ["GET", "POST", "PUT", "DELETE"],
              default: "GET",
            },
            data: {
              type: "object",
              description: "请求体（可选）",
            },
          },
          required: ["endpoint"],
        },
      },
      {
        name: "api_fetch",
        description: "抓取网页内容并返回 HTML 或文本。",
        inputSchema: {
          type: "object",
          properties: {
            url: {
              type: "string",
              description: "网页 URL",
            },
            format: {
              type: "string",
              enum: ["html", "text", "json"],
              default: "text",
            },
          },
          required: ["url"],
        },
      },
    ];
  }

  async execute(name: string, args: any) {
    switch (name) {
      case "api_request":
        return this.request(args.url, args.method, args.headers, args.data, args.params);
      case "api_github":
        return this.githubRequest(args.endpoint, args.method, args.data);
      case "api_gitlab":
        return this.gitlabRequest(args.endpoint, args.method, args.data);
      case "api_fetch":
        return this.fetchUrl(args.url, args.format);
      default:
        throw new Error(`未知 API 工具: ${name}`);
    }
  }

  private async request(
    url: string,
    method: string = "GET",
    headers: any = {},
    data?: any,
    params?: any
  ) {
    try {
      const response = await axios({
        url,
        method,
        headers,
        data,
        params,
        timeout: 30000,
      });
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(
              {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers,
                data: response.data,
              },
              null,
              2
            ),
          },
        ],
      };
    } catch (error: any) {
      if (error.response) {
        return {
          content: [
            {
              type: "text",
              text: JSON.stringify(
                {
                  status: error.response.status,
                  statusText: error.response.statusText,
                  data: error.response.data,
                },
                null,
                2
              ),
            },
          ],
          isError: true,
        };
      }
      return {
        content: [{ type: "text", text: `请求失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async githubRequest(endpoint: string, method: string = "GET", data?: any) {
    const token = process.env.GITHUB_TOKEN;
    if (!token) {
      return {
        content: [{ type: "text", text: "未设置 GITHUB_TOKEN 环境变量" }],
        isError: true,
      };
    }

    return this.request(
      `https://api.github.com${endpoint}`,
      method,
      {
        Authorization: `Bearer ${token}`,
        Accept: "application/vnd.github.v3+json",
      },
      data
    );
  }

  private async gitlabRequest(endpoint: string, method: string = "GET", data?: any) {
    const token = process.env.GITLAB_TOKEN;
    const baseUrl = process.env.GITLAB_URL || "https://gitlab.com";

    if (!token) {
      return {
        content: [{ type: "text", text: "未设置 GITLAB_TOKEN 环境变量" }],
        isError: true,
      };
    }

    return this.request(
      `${baseUrl}/api/v4${endpoint}`,
      method,
      {
        Authorization: `Bearer ${token}`,
      },
      data
    );
  }

  private async fetchUrl(url: string, format: string = "text") {
    try {
      const response = await axios.get(url, { timeout: 30000 });
      let content: string;

      if (format === "json") {
        content = JSON.stringify(response.data, null, 2);
      } else if (format === "html") {
        content = response.data;
      } else {
        content = typeof response.data === "string" 
          ? response.data 
          : JSON.stringify(response.data, null, 2);
      }

      return {
        content: [{ type: "text", text: content }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `抓取失败: ${error.message}` }],
        isError: true,
      };
    }
  }
}
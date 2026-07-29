import mysql from "mysql2/promise";

export class TeachingTools {
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
        name: "teaching_students",
        description: "获取学生列表，可按班级、姓名筛选。",
        inputSchema: {
          type: "object",
          properties: {
            classId: {
              type: "number",
              description: "班级 ID（可选）",
            },
            name: {
              type: "string",
              description: "姓名模糊搜索（可选）",
            },
            limit: {
              type: "number",
              description: "返回数量上限，默认 20",
            },
          },
        },
      },
      {
        name: "teaching_teachers",
        description: "获取教师列表。",
        inputSchema: {
          type: "object",
          properties: {
            name: {
              type: "string",
              description: "姓名模糊搜索（可选）",
            },
          },
        },
      },
      {
        name: "teaching_classes",
        description: "获取班级列表。",
        inputSchema: {
          type: "object",
          properties: {},
        },
      },
      {
        name: "teaching_tasks",
        description: "获取任务列表（作业/考试/问卷），可按状态、类型筛选。",
        inputSchema: {
          type: "object",
          properties: {
            status: {
              type: "string",
              enum: ["DRAFT", "PUBLISHED", "CLOSED"],
              description: "任务状态（可选）",
            },
            type: {
              type: "string",
              enum: ["HOMEWORK", "EXAM", "SURVEY"],
              description: "任务类型（可选）",
            },
            limit: {
              type: "number",
              description: "返回数量上限，默认 20",
            },
          },
        },
      },
      {
        name: "teaching_questions",
        description: "搜索题库题目，可按学科、难度、题型筛选。",
        inputSchema: {
          type: "object",
          properties: {
            keyword: {
              type: "string",
              description: "关键词搜索（可选）",
            },
            subjectId: {
              type: "number",
              description: "学科 ID（可选）",
            },
            difficulty: {
              type: "number",
              description: "难度等级 1-5（可选）",
            },
            type: {
              type: "string",
              description: "题型：SINGLE_CHOICE/MULTI_CHOICE/JUDGE/FILL/CALCULATION（可选）",
            },
            limit: {
              type: "number",
              description: "返回数量上限，默认 20",
            },
          },
        },
      },
      {
        name: "teaching_knowledge_tree",
        description: "获取知识节点树结构，可按学科筛选。",
        inputSchema: {
          type: "object",
          properties: {
            subjectId: {
              type: "number",
              description: "学科 ID（可选）",
            },
            level: {
              type: "number",
              description: "节点层级 1-4（可选）",
            },
          },
        },
      },
      {
        name: "teaching_ai_outputs",
        description: "获取 AI 生成内容列表（诊断报告/巩固材料等）。",
        inputSchema: {
          type: "object",
          properties: {
            type: {
              type: "string",
              enum: ["DIAGNOSIS", "CONSOLIDATION_MATERIAL", "EXAM_PAPER"],
              description: "内容类型（可选）",
            },
            studentId: {
              type: "number",
              description: "学生 ID（可选）",
            },
            limit: {
              type: "number",
              description: "返回数量上限，默认 20",
            },
          },
        },
      },
      {
        name: "teaching_credit_stats",
        description: "获取积分统计信息，可按学生、班级筛选。",
        inputSchema: {
          type: "object",
          properties: {
            studentId: {
              type: "number",
              description: "学生 ID（可选）",
            },
            classId: {
              type: "number",
              description: "班级 ID（可选）",
            },
          },
        },
      },
      {
        name: "teaching_knowledge_search",
        description: "搜索知识树中的知识点，返回层级路径、关联题目数和子节点列表。",
        inputSchema: {
          type: "object",
          properties: {
            keyword: { type: "string", description: "搜索关键词" },
            subjectId: { type: "number", description: "学科ID（可选）" },
          },
          required: ["keyword"],
        },
      },
      {
        name: "teaching_syllabus_lookup",
        description: "按学科查考纲要求，返回知识点+掌握层级。",
        inputSchema: {
          type: "object",
          properties: {
            subject: { type: "string", description: "学科名称" },
            knowledgePoint: { type: "string", description: "知识点关键词（可选）" },
          },
          required: ["subject"],
        },
      },
      {
        name: "teaching_similar_questions",
        description: "给定知识点ID，查同知识点+相近难度的练习题（含答案解析）。",
        inputSchema: {
          type: "object",
          properties: {
            nodeId: { type: "number", description: "知识点ID" },
            difficulty: { type: "number", description: "难度1-5（可选）" },
            count: { type: "number", description: "返回数量，默认5" },
          },
          required: ["nodeId"],
        },
      },
      {
        name: "teaching_search_tasks",
        description: "按标题或班级搜索已有任务，返回任务列表（用于避重）。",
        inputSchema: {
          type: "object",
          properties: {
            keyword: { type: "string", description: "任务标题关键词（可选）" },
            classId: { type: "number", description: "班级ID（可选）" },
          },
        },
      },
      {
        name: "teaching_create_task",
        description: "创建教学任务/作业。仅教师可用。需通过HTTP API调用。",
        inputSchema: {
          type: "object",
          properties: {
            title: { type: "string", description: "任务标题" },
            taskType: { type: "string", description: "任务类型：HOMEWORK/EXAM/SURVEY，默认HOMEWORK" },
            classIds: { type: "array", items: { type: "number" }, description: "目标班级ID列表" },
            description: { type: "string", description: "任务描述（可选）" },
          },
          required: ["title", "classIds"],
        },
      },
      {
        name: "teaching_send_notification",
        description: "发送系统通知给指定班级或学生。仅教师可用。",
        inputSchema: {
          type: "object",
          properties: {
            title: { type: "string", description: "通知标题" },
            content: { type: "string", description: "通知内容" },
            classIds: { type: "array", items: { type: "number" }, description: "班级ID列表（可选）" },
            studentIds: { type: "array", items: { type: "number" }, description: "学生ID列表（可选）" },
          },
          required: ["title", "content"],
        },
      },
      {
        name: "teaching_student_wrong_book",
        description: "查学生的错题本，返回错题列表、错误次数、是否已掌握。学生只能查自己的。",
        inputSchema: {
          type: "object",
          properties: {
            studentId: { type: "number", description: "学生ID（可选）" },
          },
        },
      },
      {
        name: "teaching_student_mastery",
        description: "查学生对知识点的掌握度百分比。学生只能查自己的。",
        inputSchema: {
          type: "object",
          properties: {
            studentId: { type: "number", description: "学生ID（可选）" },
            subject: { type: "string", description: "学科名称（可选）" },
            nodeId: { type: "number", description: "知识点ID（可选）" },
          },
        },
      },
      {
        name: "teaching_student_submissions",
        description: "查学生作业提交详情（得分、用时、提交时间）。学生只能查自己的。",
        inputSchema: {
          type: "object",
          properties: {
            studentId: { type: "number", description: "学生ID（可选）" },
            taskId: { type: "number", description: "任务ID（可选）" },
          },
        },
      },
      {
        name: "teaching_question_explain",
        description: "查题目的标准答案、详细解析和涉及的知识点。",
        inputSchema: {
          type: "object",
          properties: {
            questionId: { type: "number", description: "题目ID" },
          },
          required: ["questionId"],
        },
      },
      {
        name: "teaching_class_analytics",
        description: "班级某次考试统计：均分、最高/最低分、及格率、各题得分率。仅教师可用。",
        inputSchema: {
          type: "object",
          properties: {
            classId: { type: "number", description: "班级ID" },
            taskId: { type: "number", description: "考试任务ID" },
          },
          required: ["classId", "taskId"],
        },
      },
      {
        name: "teaching_knowledge_trend",
        description: "某知识点全班掌握度变化趋势。仅教师可用。",
        inputSchema: {
          type: "object",
          properties: {
            nodeId: { type: "number", description: "知识点ID" },
          },
          required: ["nodeId"],
        },
      },
      {
        name: "teaching_student_growth",
        description: "单个学生历次成绩曲线。仅教师可用。",
        inputSchema: {
          type: "object",
          properties: {
            studentId: { type: "number", description: "学生ID" },
          },
          required: ["studentId"],
        },
      },
    ];
  }

  async execute(name: string, args: any) {
    switch (name) {
      case "teaching_students":
        return this.getStudents(args.classId, args.name, args.limit);
      case "teaching_teachers":
        return this.getTeachers(args.name);
      case "teaching_classes":
        return this.getClasses();
      case "teaching_tasks":
        return this.getTasks(args.status, args.type, args.limit);
      case "teaching_questions":
        return this.getQuestions(args.keyword, args.subjectId, args.difficulty, args.type, args.limit);
      case "teaching_knowledge_tree":
        return this.getKnowledgeTree(args.subjectId, args.level);
      case "teaching_ai_outputs":
        return this.getAiOutputs(args.type, args.studentId, args.limit);
      case "teaching_credit_stats":
        return this.getCreditStats(args.studentId, args.classId);
      case "teaching_knowledge_search":
        return this.knowledgeSearch(args.keyword, args.subjectId);
      case "teaching_syllabus_lookup":
        return this.syllabusLookup(args.subject, args.knowledgePoint);
      case "teaching_similar_questions":
        return this.similarQuestions(args.nodeId, args.difficulty, args.count);
      case "teaching_search_tasks":
        return this.searchTasks(args.keyword, args.classId);
      case "teaching_create_task":
        return this.createTask(args.title, args.taskType, args.classIds, args.description);
      case "teaching_send_notification":
        return this.sendNotification(args.title, args.content, args.classIds, args.studentIds);
      case "teaching_student_wrong_book":
        return this.wrongBook(args.studentId);
      case "teaching_student_mastery":
        return this.studentMastery(args.studentId, args.subject, args.nodeId);
      case "teaching_student_submissions":
        return this.studentSubmissions(args.studentId, args.taskId);
      case "teaching_question_explain":
        return this.questionExplain(args.questionId);
      case "teaching_class_analytics":
        return this.classAnalytics(args.classId, args.taskId);
      case "teaching_knowledge_trend":
        return this.knowledgeTrend(args.nodeId);
      case "teaching_student_growth":
        return this.studentGrowth(args.studentId);
      default:
        throw new Error(`未知教学工具: ${name}`);
    }
  }

  private async getStudents(classId?: number, name?: string, limit: number = 20) {
    try {
      let sql = `
        SELECT s.id, s.student_name, s.student_no, c.class_name, s.status
        FROM students s
        LEFT JOIN classes c ON s.class_id = c.id
        WHERE 1=1
      `;
      const params: any[] = [];

      if (classId) {
        sql += " AND s.class_id = ?";
        params.push(classId);
      }
      if (name) {
        sql += " AND s.student_name LIKE ?";
        params.push(`%${name}%`);
      }
      sql += " LIMIT ?";
      params.push(limit);

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询学生失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getTeachers(name?: string) {
    try {
      let sql = `
        SELECT t.id, t.teacher_name, t.teacher_no, t.subject, t.phone
        FROM teachers t
        WHERE 1=1
      `;
      const params: any[] = [];

      if (name) {
        sql += " AND t.teacher_name LIKE ?";
        params.push(`%${name}%`);
      }

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询教师失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getClasses() {
    try {
      const [rows] = await this.pool.execute(
        "SELECT id, class_name, grade, major, head_teacher_id FROM classes ORDER BY grade, class_name"
      );
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询班级失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getTasks(status?: string, type?: string, limit: number = 20) {
    try {
      let sql = `
        SELECT id, title, task_type, status, created_at, deadline
        FROM tasks
        WHERE 1=1
      `;
      const params: any[] = [];

      if (status) {
        sql += " AND status = ?";
        params.push(status);
      }
      if (type) {
        sql += " AND task_type = ?";
        params.push(type);
      }
      sql += " ORDER BY created_at DESC LIMIT ?";
      params.push(limit);

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询任务失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getQuestions(
    keyword?: string,
    subjectId?: number,
    difficulty?: number,
    type?: string,
    limit: number = 20
  ) {
    try {
      let sql = `
        SELECT id, question_type, question_content, difficulty, subject_id, status
        FROM question_bank
        WHERE status = 1
      `;
      const params: any[] = [];

      if (keyword) {
        sql += " AND question_content LIKE ?";
        params.push(`%${keyword}%`);
      }
      if (subjectId) {
        sql += " AND subject_id = ?";
        params.push(subjectId);
      }
      if (difficulty) {
        sql += " AND difficulty = ?";
        params.push(difficulty);
      }
      if (type) {
        sql += " AND question_type = ?";
        params.push(type);
      }
      sql += " ORDER BY created_at DESC LIMIT ?";
      params.push(limit);

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询题目失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getKnowledgeTree(subjectId?: number, level?: number) {
    try {
      let sql = `
        SELECT id, name, level, parent_id, subject_id, description
        FROM knowledge_nodes
        WHERE 1=1
      `;
      const params: any[] = [];

      if (subjectId) {
        sql += " AND subject_id = ?";
        params.push(subjectId);
      }
      if (level) {
        sql += " AND level = ?";
        params.push(level);
      }
      sql += " ORDER BY subject_id, level, sort_order";

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询知识树失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getAiOutputs(type?: string, studentId?: number, limit: number = 20) {
    try {
      let sql = `
        SELECT id, output_type, title, created_at, rating, student_id
        FROM ai_outputs
        WHERE 1=1
      `;
      const params: any[] = [];

      if (type) {
        sql += " AND output_type = ?";
        params.push(type);
      }
      if (studentId) {
        sql += " AND student_id = ?";
        params.push(studentId);
      }
      sql += " ORDER BY created_at DESC LIMIT ?";
      params.push(limit);

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询 AI 输出失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async getCreditStats(studentId?: number, classId?: number) {
    try {
      let sql = `
        SELECT 
          student_id,
          SUM(credits) as total_credits,
          COUNT(*) as transaction_count
        FROM credit_transactions
        WHERE 1=1
      `;
      const params: any[] = [];

      if (studentId) {
        sql += " AND student_id = ?";
        params.push(studentId);
      }
      if (classId) {
        sql += " AND student_id IN (SELECT id FROM students WHERE class_id = ?)";
        params.push(classId);
      }
      sql += " GROUP BY student_id ORDER BY total_credits DESC";

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `查询积分统计失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async knowledgeSearch(keyword: string, subjectId?: number) {
    try {
      let sql = `
        SELECT id, name, level, parent_id, subject_id, description
        FROM knowledge_nodes
        WHERE name LIKE ? AND status = 1
      `;
      const params: any[] = [`%${keyword}%`];
      if (subjectId) { sql += " AND subject_id = ?"; params.push(subjectId); }
      sql += " ORDER BY level, parent_id LIMIT 20";

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `知识点搜索失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async syllabusLookup(subject: string, knowledgePoint?: string) {
    try {
      let sql = `
        SELECT id, exam_type, title, knowledge_dim, content, syllabus_meta
        FROM exam_syllabus
        WHERE exam_type LIKE ?
      `;
      const params: any[] = [`%${subject}%`];
      if (knowledgePoint) {
        sql += " AND (title LIKE ? OR content LIKE ?)";
        params.push(`%${knowledgePoint}%`, `%${knowledgePoint}%`);
      }
      sql += " LIMIT 50";

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `考纲查询失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async similarQuestions(nodeId: number, difficulty?: number, count: number = 5) {
    try {
      if (count > 20) count = 20;
      let sql = `
        SELECT id, question_type, question_content, difficulty, subject_id, category_id
        FROM question_bank
        WHERE category_id = ? AND status = 1
      `;
      const params: any[] = [nodeId];
      if (difficulty) { sql += " AND difficulty = ?"; params.push(difficulty); }
      sql += " ORDER BY RAND() LIMIT ?";
      params.push(count);

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `相似题目查询失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async searchTasks(keyword?: string, classId?: number) {
    try {
      let sql = `
        SELECT id, title, task_type, status, deadline, created_at
        FROM tasks
        WHERE 1=1
      `;
      const params: any[] = [];
      if (keyword) { sql += " AND title LIKE ?"; params.push(`%${keyword}%`); }
      sql += " ORDER BY created_at DESC LIMIT 50";

      const [rows] = await this.pool.execute(sql, params);
      return {
        content: [{ type: "text", text: JSON.stringify(rows, null, 2) }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `任务搜索失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async createTask(title: string, taskType: string = "HOMEWORK", classIds?: number[], description?: string) {
    try {
      return {
        content: [{ type: "text", text: `任务创建需要在教学系统前端完成。请在系统中创建任务"${title}"，类型${taskType}。` }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `创建任务失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async sendNotification(title: string, content: string, classIds?: number[], studentIds?: number[]) {
    try {
      return {
        content: [{ type: "text", text: `通知发送需要在教学系统前端完成。拟发送通知"${title}"。` }],
      };
    } catch (error: any) {
      return {
        content: [{ type: "text", text: `发送通知失败: ${error.message}` }],
        isError: true,
      };
    }
  }

  private async wrongBook(studentId?: number) {
    try {
      let sql = `
        SELECT id, student_id, question_id, wrong_count, last_wrong_time, is_mastered, source_type
        FROM wrong_questions WHERE 1=1
      `;
      const params: any[] = [];
      if (studentId) { sql += " AND student_id = ?"; params.push(studentId); }
      sql += " ORDER BY last_wrong_time DESC LIMIT 50";
      const [rows] = await this.pool.execute(sql, params);
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `错题查询失败: ${error.message}` }], isError: true };
    }
  }

  private async studentMastery(studentId?: number, subject?: string, nodeId?: number) {
    try {
      let sql = `
        SELECT id, student_id, subject, node_id, mastery_percent, total_attempts, total_correct, status
        FROM precision_progress WHERE 1=1
      `;
      const params: any[] = [];
      if (studentId) { sql += " AND student_id = ?"; params.push(studentId); }
      if (subject) { sql += " AND subject = ?"; params.push(subject); }
      if (nodeId) { sql += " AND node_id = ?"; params.push(nodeId); }
      sql += " ORDER BY subject, node_id LIMIT 100";
      const [rows] = await this.pool.execute(sql, params);
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `掌握度查询失败: ${error.message}` }], isError: true };
    }
  }

  private async studentSubmissions(studentId?: number, taskId?: number) {
    try {
      let sql = `
        SELECT id, task_id, student_id, score, status, submitted_at
        FROM task_submissions WHERE 1=1
      `;
      const params: any[] = [];
      if (studentId) { sql += " AND student_id = ?"; params.push(studentId); }
      if (taskId) { sql += " AND task_id = ?"; params.push(taskId); }
      sql += " ORDER BY submitted_at DESC LIMIT 50";
      const [rows] = await this.pool.execute(sql, params);
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `提交查询失败: ${error.message}` }], isError: true };
    }
  }

  private async questionExplain(questionId: number) {
    try {
      const [rows] = await this.pool.execute(
        `SELECT id, question_type, question_text, options, correct_answer, explanation, difficulty_level, subject
         FROM question_bank WHERE id = ? AND status = 1`, [questionId]
      );
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `题目查询失败: ${error.message}` }], isError: true };
    }
  }

  private async classAnalytics(classId: number, taskId: number) {
    try {
      const [rows] = await this.pool.execute(
        `SELECT student_id, score, status, submitted_at
         FROM task_submissions WHERE task_id = ? LIMIT 500`, [taskId]
      );
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `班级分析失败: ${error.message}` }], isError: true };
    }
  }

  private async knowledgeTrend(nodeId: number) {
    try {
      const [rows] = await this.pool.execute(
        `SELECT sa.id, sa.question_id, sa.is_correct, sa.score, sa.create_time, qb.subject
         FROM student_answers sa LEFT JOIN question_bank qb ON sa.question_id = qb.id
         WHERE qb.category_id = ? LIMIT 500`, [nodeId]
      );
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `知识点趋势查询失败: ${error.message}` }], isError: true };
    }
  }

  private async studentGrowth(studentId: number) {
    try {
      const [rows] = await this.pool.execute(
        `SELECT task_id, score, status, submitted_at
         FROM task_submissions WHERE student_id = ? ORDER BY submitted_at LIMIT 50`, [studentId]
      );
      return { content: [{ type: "text", text: JSON.stringify(rows, null, 2) }] };
    } catch (error: any) {
      return { content: [{ type: "text", text: `学生成长查询失败: ${error.message}` }], isError: true };
    }
  }
}
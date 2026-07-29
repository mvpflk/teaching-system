package com.school.teaching.controller;

import com.school.teaching.annotation.AuditLog;
import com.school.teaching.annotation.AuditEventType;

import com.school.teaching.common.R;
import com.school.teaching.entity.KnowledgeNode;
import com.school.teaching.security.SecurityUtils;
import com.school.teaching.service.KnowledgeNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge-node")
public class KnowledgeNodeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeNodeController.class);

    @Autowired private KnowledgeNodeService knowledgeNodeService;

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> getTree(
            jakarta.servlet.http.HttpServletRequest request) {
        String sid = request.getParameter("subjectId");
        Long subjectId = (sid != null && !sid.isEmpty()) ? Long.valueOf(sid) : null;
        return R.ok(knowledgeNodeService.getTree(subjectId));
    }

    /** 返回知识树版本号（max updated_at 的时间戳），前端用 localStorage 缓存 */
    @GetMapping("/version")
    public R<Long> getVersion() {
        return R.ok(knowledgeNodeService.getTreeVersion());
    }

    @GetMapping("/list")
    public R<List<KnowledgeNode>> list(@RequestParam(required = false) Long parentId,
                                       @RequestParam(required = false) Integer level) {
        return R.ok(knowledgeNodeService.list(parentId, level));
    }

    @PostMapping
    @AuditLog(eventType = AuditEventType.OTHER, description = "创建知识点")
    public R<KnowledgeNode> create(@RequestBody KnowledgeNode node) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可编辑知识点");
        return R.ok(knowledgeNodeService.create(node), "创建成功");
    }

    @PutMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新知识点")
    public R<KnowledgeNode> update(@PathVariable Long id, @RequestBody KnowledgeNode node) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可编辑知识点");
        return R.ok(knowledgeNodeService.update(id, node), "更新成功");
    }

    @DeleteMapping("/{id}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "删除知识点")
    public R<String> delete(@PathVariable Long id) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可编辑知识点");
        knowledgeNodeService.delete(id);
        return R.ok("删除成功");
    }

    @PostMapping("/actions/import")
    @AuditLog(eventType = AuditEventType.OTHER, description = "批量导入知识点")
    public R<?> importNodes(@RequestParam("file") MultipartFile file) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可编辑知识点");
        try {
            int count = knowledgeNodeService.importFromExcel(file);
            return R.ok(Map.of("count", count), "成功导入 " + count + " 条");
        } catch (Exception e) {
            return R.error(400, "导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/content")
    public R<?> getContent(@PathVariable Long id,
                           @RequestParam(defaultValue = "false") boolean includeChildren) {
        String content = knowledgeNodeService.getContent(id, includeChildren);
        return R.ok(Map.of("nodeId", id, "content", content != null ? content : ""));
    }

    @PutMapping("/{id}/content")
    @AuditLog(eventType = AuditEventType.OTHER, description = "更新知识点知识库内容")
    public R<?> setContent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        knowledgeNodeService.setContent(id, content != null ? content : "");
        return R.ok("已更新");
    }

    @PostMapping("/actions/import-zip")
    @AuditLog(eventType = AuditEventType.OTHER, description = "导入知识库ZIP")
    public R<?> importZip(@RequestParam Long subjectId, @RequestParam("file") MultipartFile file) {
        try {
            int count = knowledgeNodeService.importFromZip(subjectId, file);
            return R.ok(Map.of("count", count), "成功导入 " + count + " 个知识点");
        } catch (Exception e) {
            log.error("导入知识库ZIP失败 subjectId={}", subjectId, e);
            return R.error(400, "导入失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @PostMapping("/actions/import-txt")
    @AuditLog(eventType = AuditEventType.OTHER, description = "导入知识库TXT")
    public R<?> importTxt(@RequestParam Long subjectId, @RequestParam("file") MultipartFile file) {
        try {
            int count = knowledgeNodeService.importFromTxt(subjectId, file);
            String msg = count > 0 ? "成功导入 " + count + " 个节点（含章节/任务/知识点）"
                : "导入完成，所有节点已存在或内容已更新";
            return R.ok(Map.of("count", count), msg);
        } catch (Exception e) {
            log.error("导入知识库TXT失败 subjectId={}", subjectId, e);
            return R.error(400, "导入失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @PostMapping("/actions/import-docx")
    @AuditLog(eventType = AuditEventType.OTHER, description = "导入知识库Word")
    public R<?> importDocx(@RequestParam Long subjectId, @RequestParam("file") MultipartFile file) {
        try {
            int count = knowledgeNodeService.importFromDocx(subjectId, file);
            String msg = count > 0 ? "成功导入 " + count + " 个节点（含章节/任务/知识点）"
                : "导入完成，所有节点已存在或内容已更新";
            return R.ok(Map.of("count", count), msg);
        } catch (Exception e) {
            log.error("导入知识库Word失败 subjectId={}", subjectId, e);
            return R.error(400, "导入失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    @GetMapping("/actions/template/download")
    public void downloadTemplate(jakarta.servlet.http.HttpServletResponse response) {
        try {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource("templates/knowledge_node_import_template.xlsx");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"知识点导入模板.xlsx\"");
            response.setHeader("Cache-Control", "no-cache");
            try (var in = resource.getInputStream(); var out = response.getOutputStream()) {
                in.transferTo(out);
            }
        } catch (Exception e) {
            throw new com.school.teaching.exception.BusinessException(500, "模板下载失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/actions/clear-by-subject/{subjectId}")
    @AuditLog(eventType = AuditEventType.OTHER, description = "清空学科知识节点")
    public R<?> clearBySubject(@PathVariable Long subjectId) {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可清空知识节点");
        try {
            knowledgeNodeService.clearBySubject(subjectId);
            return R.ok("该学科下所有知识节点已清空");
        } catch (Exception e) {
            return R.error(500, "清空失败: " + e.getMessage());
        }
    }

    @GetMapping("/actions/txt-template/download")
    public void downloadTxtTemplate(@RequestParam(defaultValue = "culture") String type,
                                    jakarta.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("text/plain;charset=utf-8");
        String filename = "professional".equals(type) ? "专业课知识点导入模板.txt" : "文化课知识点导入模板.txt";
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                java.net.URLEncoder.encode(filename, "UTF-8") + "\"");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(generateTxtTemplate("professional".equals(type)));
    }

    @GetMapping("/actions/docx-template/download")
    public void downloadDocxTemplate(@RequestParam(defaultValue = "culture") String type,
                                     jakarta.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("text/plain;charset=utf-8");
        String filename = "professional".equals(type) ? "专业课知识点导入模板.txt" : "文化课知识点导入模板.txt";
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                java.net.URLEncoder.encode(filename, "UTF-8") + "\"");
        response.setHeader("Cache-Control", "no-cache");
        // Word 模板输出为结构化 TXT（用户可在 Word 中按此结构编写后导出为 .docx）
        response.getWriter().write(generateDocxTemplate("professional".equals(type)));
    }

    @GetMapping("/actions/zip-template/download")
    public void downloadZipTemplate(jakarta.servlet.http.HttpServletResponse response) {
        try {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource("templates/knowledge_node_zip_template.zip");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"知识库ZIP导入模板.zip\"");
            response.setHeader("Cache-Control", "no-cache");
            try (var in = resource.getInputStream(); var out = response.getOutputStream()) {
                in.transferTo(out);
            }
        } catch (Exception e) {
            throw new com.school.teaching.exception.BusinessException(500, "模板下载失败: " + e.getMessage());
        }
    }

    // ── 模板内容生成 ──

    private String generateTxtTemplate(boolean professional) {
        if (professional) {
            return "# 计算机基础\r\n" +
                "## 计算机组成\r\n" +
                "### 中央处理器(CPU)\r\n" +
                "CPU是计算机的核心部件，由运算器和控制器组成。运算器负责算术和逻辑运算，控制器负责从内存中取出指令并执行。\r\n" +
                "主要性能指标包括：主频、核心数、缓存大小。\r\n" +
                "\r\n" +
                "### 存储器\r\n" +
                "计算机存储器分为内存储器和外存储器。内存(主存)是CPU能直接访问的存储空间，存取速度快但容量有限。\r\n" +
                "常见类型：RAM(随机存取存储器)、ROM(只读存储器)、Cache(高速缓冲存储器)。\r\n" +
                "\r\n" +
                "## 操作系统\r\n" +
                "### 操作系统概述\r\n" +
                "操作系统是管理和控制计算机硬件与软件资源的系统软件，是用户和计算机之间的接口。\r\n" +
                "五大功能：处理机管理、存储器管理、设备管理、文件管理、作业管理。\r\n" +
                "\r\n" +
                "### Windows基本操作\r\n" +
                "Windows的文件管理功能包括：创建、复制、移动、删除、重命名文件和文件夹。\r\n" +
                "系统设置包括：显示设置、网络设置、账户管理、安全设置。\r\n" +
                "\r\n" +
                "# 计算机网络\r\n" +
                "## 网络基础\r\n" +
                "### OSI七层模型\r\n" +
                "OSI参考模型将网络通信分为七层：物理层、数据链路层、网络层、传输层、会话层、表示层、应用层。\r\n" +
                "每层都有特定的功能和协议，便于标准化和故障排查。\r\n" +
                "\r\n" +
                "### TCP/IP协议\r\n" +
                "TCP/IP协议族是Internet的基础，包含TCP(传输控制协议)和IP(互联网协议)。\r\n" +
                "TCP提供可靠的、面向连接的数据传输；IP负责数据包的路由和转发。\r\n" +
                "\r\n" +
                "## 网络安全\r\n" +
                "### 网络安全威胁\r\n" +
                "常见网络安全威胁包括：病毒、木马、蠕虫、钓鱼攻击、DDoS攻击。\r\n" +
                "防护措施：安装杀毒软件、定期更新系统补丁、使用防火墙、数据加密、备份重要数据。\r\n" +
                "\r\n" +
                "### 密码学基础\r\n" +
                "密码学是研究信息安全保护的科学，包括对称加密(AES、DES)和非对称加密(RSA、ECC)。\r\n" +
                "数字签名用于验证数据来源和完整性，哈希函数(MD5、SHA)用于数据完整性校验。";
        } else {
            return "# 语文基础知识\r\n" +
                "## 现代文阅读\r\n" +
                "### 记叙文阅读\r\n" +
                "记叙文是以记人、叙事、写景、状物为主要内容，以叙述、描写为主要表达方式的文章。\r\n" +
                "阅读要点：把握六要素(时间、地点、人物、起因、经过、结果)，理清叙述顺序和线索。\r\n" +
                "\r\n" +
                "### 说明文阅读\r\n" +
                "说明文是以说明为主要表达方式，介绍事物、阐明事理的文章。\r\n" +
                "常见说明方法：举例子、列数字、作比较、下定义、分类别、打比方。\r\n" +
                "\r\n" +
                "## 文言文阅读\r\n" +
                "### 常见文言虚词\r\n" +
                "常见文言虚词包括：之、其、而、以、于、为、者、所、何、乃等。\r\n" +
                "每个虚词有多种用法，需结合具体语境分析。例如'之'可作为代词、助词、动词。\r\n" +
                "\r\n" +
                "### 文言文翻译技巧\r\n" +
                "翻译原则：信(准确)、达(通顺)、雅(优美)。\r\n" +
                "基本方法：留(保留专有名词)、删(删除无义虚词)、补(补充省略成分)、换(古语换今语)、调(调整语序)。\r\n" +
                "\r\n" +
                "# 数学基础\r\n" +
                "## 集合与函数\r\n" +
                "### 集合的概念与运算\r\n" +
                "集合是具有某种特定性质的对象组成的整体。集合的元素具有确定性、互异性、无序性。\r\n" +
                "基本运算：并集(∪)、交集(∩)、补集、差集。常用公式：|A∪B|=|A|+|B|-|A∩B|。\r\n" +
                "\r\n" +
                "### 函数的性质\r\n" +
                "函数的单调性描述函数值随自变量增大而变化的趋势：增函数(f(x₁)<f(x₂)当x₁<x₂)、减函数(f(x₁)>f(x₂))。\r\n" +
                "函数的奇偶性：偶函数满足f(-x)=f(x)、奇函数满足f(-x)=-f(x)。\r\n" +
                "\r\n" +
                "## 数列\r\n" +
                "### 等差数列\r\n" +
                "等差数列的通项公式：an = a₁ + (n-1)d，前n项和公式：Sn = n(a₁+an)/2 = na₁ + n(n-1)d/2。\r\n" +
                "等差中项：若a, b, c成等差数列，则2b = a + c。\r\n" +
                "\r\n" +
                "### 等比数列\r\n" +
                "等比数列的通项公式：an = a₁q^(n-1)，前n项和公式(当q≠1)：Sn = a₁(1-q^n)/(1-q)。\r\n" +
                "等比中项：若a, b, c成等比数列，则b² = a·c。";
        }
    }

    private String generateDocxTemplate(boolean professional) {
        // Word模板以结构化Markdown格式输出（用户可参照此结构在Word中编写，导出为.docx后导入）
        StringBuilder sb = new StringBuilder();
        sb.append("# Word 文档导入说明\r\n");
        sb.append("# ====================\r\n");
        sb.append("# 1. 在Word中使用「标题1」「标题2」「标题3」样式标记层级\r\n");
        sb.append("# 2. 标题1 = 章节名称，标题2 = 任务名称，标题3 = 知识点名称\r\n");
        sb.append("# 3. 正文段落 = 该知识点的详细内容\r\n");
        sb.append("# 4. 编写完成后保存为 .docx 格式，在本系统上传导入\r\n");
        sb.append("# ====================\r\n\r\n");
        sb.append(generateTxtTemplate(professional));
        return sb.toString();
    }

    // ── AI 学习资源 ──

    /** AI 生成学习资源 */
    @PostMapping("/{id}/generate-resources")
    @AuditLog(eventType = AuditEventType.OTHER, description = "AI生成学习资源")
    public R<Map<String, Object>> generateResources(@PathVariable Long id) {
        if (!SecurityUtils.isTeacherOrAdmin())
            return R.error(403, "仅管理员和教师可生成学习资源");
        try {
            Map<String, Object> result = knowledgeNodeService.generateLearningResources(id);
            return R.ok(result, "学习资源已生成，待审核");
        } catch (com.school.teaching.exception.BusinessException e) {
            return R.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("生成学习资源失败 nodeId={}", id, e);
            return R.error(500, "生成失败: " + e.getMessage());
        }
    }

    /** 教师审核学习资源 */
    @PutMapping("/{id}/resource-status")
    @AuditLog(eventType = AuditEventType.OTHER, description = "审核学习资源")
    public R<?> reviewResource(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isHeadTeacher())
            return R.error(403, "仅管理员和班主任可审核学习资源");
        String status = body.get("status");
        String rejectReason = body.get("rejectReason");
        try {
            knowledgeNodeService.reviewResource(id, status, rejectReason);
            return R.ok("审核完成");
        } catch (com.school.teaching.exception.BusinessException e) {
            return R.error(e.getCode(), e.getMessage());
        }
    }

    /** 视频链接有效性检查 */
    @GetMapping("/actions/check-video-links")
    @AuditLog(eventType = AuditEventType.OTHER, description = "检查视频链接有效性")
    public R<List<Map<String, Object>>> checkVideoLinks() {
        if (!SecurityUtils.isAdmin()) return R.error(403, "仅管理员可执行");
        List<Map<String, Object>> deadLinks = knowledgeNodeService.checkVideoLinks();
        return R.ok(deadLinks, "检查完成，发现 " + deadLinks.size() + " 个失效链接");
    }

    /** 获取节点学习资源（学生端） */
    @GetMapping("/{id}/learning-resources")
    public R<Map<String, Object>> getLearningResources(@PathVariable Long id) {
        Map<String, Object> result = knowledgeNodeService.getNodeLearningResources(id);
        return R.ok(result);
    }
}

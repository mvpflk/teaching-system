package com.school.teaching.geometry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/**
 * Phase 0 实验：验证 DeepSeek 能否可靠输出 GeometrySpec JSON。
 *
 * 运行方式：mvn test -Dtest=Phase0DiagramExperiment -pl . 或直接在 IDE 中运行
 *
 * 5 道测试题（2 三角 + 2 解析几何 + 1 函数图像），每道跑 3 次（temperature=0.3）。
 * 统计 4 项指标：格式正确率 / 引用完整性 / 可解性 / 数值一致性。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase0DiagramExperiment {

    private static final String API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final ObjectMapper OM = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private static final GeometrySpecParser parser = new GeometrySpecParser();
    private static final ConstraintSolver solver = new ConstraintSolver();
    private static final SvgRenderer renderer = new SvgRenderer();

    // ── 5 道测试题 ──
    static final String[][] TEST_CASES = {
        // [题目描述, 期望的 diagram 检查项（逗号分隔）]
        {
            "在△ABC中，已知AB=6，AC=5，BC=4。求△ABC的面积。",
            "point:3, segment:3, label:3, dimension:AB:6, dimension:AC:5"
        },
        {
            "在Rt△ABC中，∠C=90°，AC=3，BC=4。求AB的长及sinA的值。",
            "point:3, segment:3, right-angle:1, label:3, dimension:BC:4"
        },
        {
            "已知圆C的圆心为O(2,-1)，半径为3。求圆C的标准方程。",
            "point:1, circle:1, label:1, grid:1"
        },
        {
            "已知椭圆x²/4+y²=1，过右焦点F的直线l与椭圆交于A、B两点。",
            "grid:1, axis:1, label:2"
        },
        {
            "已知二次函数f(x)=x²-2x+3，求其图像的顶点坐标和对称轴方程。",
            "grid:1, axis:1, function-plot:1"
        }
    };

    static final int RUNS_PER_CASE = 3;
    static final List<TestResult> allResults = new ArrayList<>();

    record TestResult(int caseIdx, int run, boolean formatOk, boolean refsOk, boolean solvable,
                      boolean numsOk, String svgPreview, String error) {}

    @BeforeAll
    static void checkApiKey() {
        Assumptions.assumeTrue(API_KEY != null && !API_KEY.isBlank(),
            "跳过：DEEPSEEK_API_KEY 未设置");
        System.out.println("=== Phase 0 实验开始 ===");
        System.out.println("API: " + API_URL);
        System.out.println("测试题数: " + TEST_CASES.length + " × " + RUNS_PER_CASE + " 次 = "
            + (TEST_CASES.length * RUNS_PER_CASE) + " 次调用\n");
    }

    @Test
    @Order(1)
    void testEngineOnly() {
        System.out.println("--- 引擎自测 ---");
        // 用 §6.4 的三角形示例验证引擎
        String testJson = """
            {
              "elements": [
                {"type":"point","id":"A","x":0,"y":0},
                {"type":"point","id":"B","x":6,"y":0},
                {"type":"point","id":"C","constraints":[{"distance":["A",5]},{"distance":["B",4]}]},
                {"type":"segment","id":"AB","from":"A","to":"B"},
                {"type":"segment","from":"A","to":"C"},
                {"type":"segment","from":"B","to":"C"},
                {"type":"label","at":"A","text":"A","offset":[-8,14]},
                {"type":"label","at":"B","text":"B","offset":[8,14]},
                {"type":"label","at":"C","text":"C","offset":[0,-14]},
                {"type":"dimension","segment":"AB","label":"6","offset":[0,20]}
              ]
            }""";

        GeometrySpec spec = parser.parse(testJson);
        Assertions.assertNotNull(spec, "解析成功");
        Assertions.assertEquals(10, spec.getElements().size(), "10 个元素");

        solver.solve(spec);
        double[] c = solver.getPoint("C");
        // △ABC: AB=6, AC=5, BC=4 → C=(3.75, ±3.307)，index=0 取 y 正半轴
        Assertions.assertTrue(Math.abs(c[0] - 3.75) < 0.05, "C.x ≈ 3.75, 实际=" + c[0]);
        Assertions.assertTrue(Math.abs(c[1] - 3.307) < 0.05, "C.y ≈ 3.307, 实际=" + c[1]);

        String svg = renderer.render(spec);
        Assertions.assertTrue(svg.contains("<svg"), "包含 <svg>");
        Assertions.assertTrue(svg.contains(">6<"), "包含 dimension 标注");
        Assertions.assertTrue(svg.contains(">A<"), "包含 label A");
        Assertions.assertFalse(svg.contains("<script"), "无 script 标签");
        Assertions.assertFalse(svg.contains("onclick"), "无 onclick");

        System.out.println("引擎自测通过: C=(" + c[0] + "," + c[1] + "), SVG="
            + svg.length() + " chars\n");
    }

    @Test
    @Order(2)
    void testAiDiagramGeneration() throws Exception {
        System.out.println("--- AI 生成测试 ---");
        for (int i = 0; i < TEST_CASES.length; i++) {
            for (int run = 0; run < RUNS_PER_CASE; run++) {
                System.out.print("  [" + (i + 1) + "/" + TEST_CASES.length
                    + "] run" + (run + 1) + ": " + TEST_CASES[i][0].substring(0, 20) + "... → ");
                TestResult r = runOneTest(i, run);
                allResults.add(r);
                System.out.println((r.formatOk ? "✅" : "❌") + "格式 "
                    + (r.refsOk ? "✅" : "❌") + "引用 "
                    + (r.solvable ? "✅" : "❌") + "可解 "
                    + (r.numsOk ? "✅" : "❌") + "数值"
                    + (r.error != null ? " (" + r.error + ")" : "")
                    + (r.svgPreview != null ? " SVG" : ""));
                Thread.sleep(500); // rate limit
            }
        }
    }

    @AfterAll
    static void printReport() {
        if (allResults.isEmpty()) return;

        long formatOk = allResults.stream().filter(r -> r.formatOk).count();
        long refsOk = allResults.stream().filter(r -> r.refsOk).count();
        long solvable = allResults.stream().filter(r -> r.solvable).count();
        long numsOk = allResults.stream().filter(r -> r.numsOk).count();
        int total = allResults.size();

        System.out.println("\n========== Phase 0 实验报告 ==========");
        System.out.println("总测试次数: " + total);
        System.out.println();
        System.out.printf("格式正确率:   %d/%d = %.0f%%%n", formatOk, total, 100.0 * formatOk / total);
        System.out.printf("引用完整性:   %d/%d = %.0f%%%n", refsOk, total, 100.0 * refsOk / total);
        System.out.printf("可解性:       %d/%d = %.0f%%%n", solvable, total, 100.0 * solvable / total);
        System.out.printf("数值一致性:   %d/%d = %.0f%%%n", numsOk, total, 100.0 * numsOk / total);
        System.out.println();

        // 判断
        double fr = 100.0 * formatOk / total;
        double rr = 100.0 * refsOk / total;
        double sr = 100.0 * solvable / total;
        double nr = 100.0 * numsOk / total;

        boolean pass = fr >= 90 && rr >= 95 && sr >= 85 && nr >= 90;
        boolean partial = fr >= 70 && rr >= 80 && sr >= 60 && nr >= 70;

        System.out.println("判定: " + (pass ? "✅ 全部通过 → 继续 Phase 1" :
            partial ? "⚠️ 部分通过 → 改进 Prompt（增加 few-shot 示例到 3 个），重测" :
            "❌ 不通过 → 启用预定义模板方案"));
        System.out.println("=======================================");
    }

    // ── helpers ──

    private TestResult runOneTest(int caseIdx, int run) {
        try {
            String problemText = TEST_CASES[caseIdx][0];
            String[] expectedChecks = TEST_CASES[caseIdx][1].split(",\\s*");

            Map<String, Object> aiQuestion = callDeepSeek(problemText);
            Object diagramObj = aiQuestion.get("diagram");

            boolean formatOk = diagramObj instanceof Map;
            boolean refsOk = false, solvable = false, numsOk = false;
            String svgPreview = null;
            String error = null;

            if (formatOk) {
                @SuppressWarnings("unchecked")
                Map<String, Object> diagram = (Map<String, Object>) diagramObj;
                try {
                    GeometrySpec spec = parser.parse(diagram);
                    refsOk = true; // 解析成功 = 引用完整

                    try {
                        solver.solve(spec);
                        solvable = true;

                        String svg = renderer.render(spec);
                        svgPreview = svg.substring(0, Math.min(80, svg.length()));

                        // 数值一致性检查
                        numsOk = checkNumericalConsistency(svg, expectedChecks);
                    } catch (GeometryException e) {
                        error = "求解失败: " + e.getMessage();
                    }
                } catch (GeometryException e) {
                    error = "解析失败: " + e.getMessage();
                }
            } else {
                error = "diagram 缺失或格式错误";
            }
            return new TestResult(caseIdx, run, formatOk, refsOk, solvable, numsOk, svgPreview, error);

        } catch (Exception e) {
            return new TestResult(caseIdx, run, false, false, false, false, null,
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callDeepSeek(String problemText) throws Exception {
        String systemPrompt = buildGeometryPrompt();
        String userPrompt = "请为下面这道数学题生成题目JSON（含 diagram 字段）：\n" + problemText
            + "\n\n输出格式：纯 JSON 对象，包含 questionText、questionType、correctAnswer、explanation、difficultyLevel，如果涉及几何图形则附加 diagram 字段。";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "deepseek-chat");
        body.put("temperature", 0.3);
        body.put("max_tokens", 2048);
        body.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userPrompt)
        ));

        String json = OM.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("API error " + resp.statusCode() + ": " + resp.body());
        }

        Map<String, Object> respMap = OM.readValue(resp.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        // 提取 JSON 对象
        String jsonStr = content;
        int braceStart = content.indexOf('{');
        int braceEnd = content.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            jsonStr = content.substring(braceStart, braceEnd + 1);
        }
        return OM.readValue(jsonStr, Map.class);
    }

    private String buildGeometryPrompt() {
        return "你是四川省对口升学考试数学命题教师。如果题目涉及几何图形（三角形、四边形、圆、坐标系、函数图像等），请在题目JSON中附加 \"diagram\" 字段。\n\n"
            + DIAGRAM_INSTRUCTIONS;
    }

    private boolean checkNumericalConsistency(String svg, String[] checks) {
        for (String check : checks) {
            String[] parts = check.split(":");
            if (parts.length == 2) {
                // 类型检查: "point:3" → SVG 中应有 3 个 <circle（点）
                String type = parts[0];
                int expectedCount = Integer.parseInt(parts[1]);
                long actualCount = countInSvg(svg, type);
                if (actualCount < expectedCount) return false;
            } else if (parts.length == 3) {
                // 数值检查: "dimension:AB:6" → SVG 中应有文本 "6"
                String label = parts[2];
                if (!svg.contains(">" + label + "<") && !svg.contains(">" + label + ".")) return false;
            }
        }
        return true;
    }

    private long countInSvg(String svg, String type) {
        return switch (type) {
            case "point" -> svg.split("<circle").length - 1;
            case "segment" -> (svg.split("<line").length - 1) / 2; // roughly
            case "label" -> svg.split("<text").length - 1;
            case "circle" -> (svg.split("<circle").length - 1) - (svg.split("r=\"3\"").length - 1);
            case "right-angle" -> svg.split("right-angle").length;
            case "grid" -> svg.contains("stroke-width=\"0.3\"") ? 1 : 0;
            case "axis" -> svg.contains("stroke-width=\"1.2\"") ? 1 : 0;
            case "function-plot" -> svg.contains("<polyline") ? 1 : 0;
            case "dimension" -> svg.contains("class=\"dim\"") ? 1 : 0;
            default -> 1;
        };
    }

    static final String DIAGRAM_INSTRUCTIONS = "【几何图形支持——必须严格遵守】\n"
        + "diagram 字段遵循 JSON 规范，包括 elements 数组。\n\n"
        + "⚠️ 关键规则：\n"
        + "1. 每个被引用的点必须先定义（如 label 的 at 引用 O，必须先有 {\"type\":\"point\",\"id\":\"O\",...}）\n"
        + "2. label 必须有 at 和 text 字段，缺一不可\n"
        + "3. dimension 引用线段时，用两点连写形式（如 \"AB\" 表示从A到B的线段），不需要给线段加 id\n"
        + "4. 坐标系用 grid+axis 组合：grid 画网格，axis 画坐标轴和刻度\n\n"
        + "元素规范：\n"
        + "- point: {\"type\":\"point\",\"id\":\"A\",\"x\":0,\"y\":0}\n"
        + "- point(约束): {\"type\":\"point\",\"id\":\"C\",\"constraints\":[{\"distance\":[\"A\",5]},{\"distance\":[\"B\",4]}]}\n"
        + "- segment: {\"type\":\"segment\",\"from\":\"A\",\"to\":\"B\"}\n"
        + "- circle: {\"type\":\"circle\",\"center\":\"O\",\"radius\":3}\n"
        + "- polygon: {\"type\":\"polygon\",\"vertices\":[\"A\",\"B\",\"C\"]}\n"
        + "- label: {\"type\":\"label\",\"at\":\"A\",\"text\":\"A\",\"offset\":[-8,12]}   ← at 和 text 必填！\n"
        + "- right-angle: {\"type\":\"right-angle\",\"vertex\":\"C\",\"leg1\":\"A\",\"leg2\":\"B\"}\n"
        + "- angle-arc: {\"type\":\"angle-arc\",\"vertex\":\"A\",\"from\":\"B\",\"to\":\"C\",\"label\":\"60°\"}\n"
        + "- dimension: {\"type\":\"dimension\",\"segment\":\"AB\",\"label\":\"6\",\"offset\":[0,18]}\n"
        + "- grid: {\"type\":\"grid\",\"xRange\":[-5,5],\"yRange\":[-5,5],\"step\":1}\n"
        + "- axis: {\"type\":\"axis\",\"xLabel\":\"x\",\"yLabel\":\"y\",\"tickStep\":1}\n"
        + "- function-plot: {\"type\":\"function-plot\",\"expr\":\"x^2-2*x+3\",\"xRange\":[-1,3],\"samples\":200}\n\n"
        + "约束点支持 7 种约束: midpoint, foot, intersection, circleIntersection, onCircle, onSegment, distance\n\n"
        + "⚠️ 常见错误（必须避免）：\n"
        + "- label 缺少 at 或 text → 渲染失败\n"
        + "- 引用了未定义的点 ID → 渲染失败\n"
        + "- 不需要 diagram 的题（代数/概率/数列）强行加 diagram → 浪费\n\n"
        + "完整示例（△ABC，AB=6，AC=5，BC=4）：\n"
        + "{\"elements\":[\n"
        + "  {\"type\":\"point\",\"id\":\"A\",\"x\":0,\"y\":0},\n"
        + "  {\"type\":\"point\",\"id\":\"B\",\"x\":6,\"y\":0},\n"
        + "  {\"type\":\"point\",\"id\":\"C\",\"constraints\":[{\"distance\":[\"A\",5]},{\"distance\":[\"B\",4]}]},\n"
        + "  {\"type\":\"segment\",\"from\":\"A\",\"to\":\"B\"},\n"
        + "  {\"type\":\"segment\",\"from\":\"A\",\"to\":\"C\"},\n"
        + "  {\"type\":\"segment\",\"from\":\"B\",\"to\":\"C\"},\n"
        + "  {\"type\":\"label\",\"at\":\"A\",\"text\":\"A\",\"offset\":[-8,14]},\n"
        + "  {\"type\":\"label\",\"at\":\"B\",\"text\":\"B\",\"offset\":[8,14]},\n"
        + "  {\"type\":\"label\",\"at\":\"C\",\"text\":\"C\",\"offset\":[0,-14]},\n"
        + "  {\"type\":\"dimension\",\"segment\":\"AB\",\"label\":\"6\",\"offset\":[0,18]}\n"
        + "]}\n\n"
        + "输出纯 JSON 对象，不要用 markdown 代码块包裹。";
}

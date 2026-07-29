package com.school.teaching.geometry;

import java.nio.file.*;

public class DiagramViewer {
    public static void main(String[] args) throws Exception {
        GeometrySpecParser parser = new GeometrySpecParser();
        ConstraintSolver solver = new ConstraintSolver();
        SvgRenderer renderer = new SvgRenderer();

        // ── 图1：三角形 ABC（AB=6, AC=5, BC=4）──
        String triangleJson = """
            {
              "elements": [
                {"type":"point","id":"A","x":0,"y":0},
                {"type":"point","id":"B","x":6,"y":0},
                {"type":"point","id":"C","constraints":[{"distance":["A",5]},{"distance":["B",4]}]},
                {"type":"segment","from":"A","to":"B"},
                {"type":"segment","from":"A","to":"C"},
                {"type":"segment","from":"B","to":"C"},
                {"type":"label","at":"A","text":"A","offset":[-0.4,0.4]},
                {"type":"label","at":"B","text":"B","offset":[0.4,0.4]},
                {"type":"label","at":"C","text":"C","offset":[0,-0.4]}
              ]
            }""";

        // ── 图2：直角三角形 Rt△ABC（∠A=90°, AC=3, AB=4）──
        String rightTriJson = """
            {
              "elements": [
                {"type":"point","id":"A","x":0,"y":0},
                {"type":"point","id":"B","x":4,"y":0},
                {"type":"point","id":"C","x":0,"y":3},
                {"type":"segment","from":"A","to":"B"},
                {"type":"segment","from":"A","to":"C"},
                {"type":"segment","from":"B","to":"C"},
                {"type":"label","at":"A","text":"A","offset":[-0.4,0.4]},
                {"type":"label","at":"B","text":"B","offset":[0.4,0.4]},
                {"type":"label","at":"C","text":"C","offset":[-0.4,-0.4]},
                {"type":"right-angle","vertex":"A","leg1":"C","leg2":"B"}
              ]
            }""";

        // ── 图3：圆（圆心O(2,-1), r=3）+ 坐标系 ──
        String circleJson = """
            {
              "elements": [
                {"type":"grid","xRange":[-4,6],"yRange":[-5,3],"step":1},
                {"type":"axis","xLabel":"x","yLabel":"y","tickStep":1},
                {"type":"point","id":"O","x":2,"y":-1},
                {"type":"circle","center":"O","radius":3},
                {"type":"label","at":"O","text":"O","offset":[0.4,-0.5]}
              ]
            }""";

        // ── 图4：二次函数 y=x²-2x+3 ──
        String funcJson = """
            {
              "elements": [
                {"type":"grid","xRange":[-3,5],"yRange":[-1,9],"step":1},
                {"type":"axis","xLabel":"x","yLabel":"y","tickStep":1},
                {"type":"function-plot","expr":"x^2-2*x+3","xRange":[-1.5,3.5],"color":"#e91e63","samples":200},
                {"type":"point","id":"V","x":1,"y":2},
                {"type":"label","at":"V","text":"V(1,2)","offset":[0.4,-0.4]}
              ]
            }""";

        String outDir = "target/diagrams";
        Files.createDirectories(Path.of(outDir));

        render("三角形 (AB=6,AC=5,BC=4)", triangleJson, parser, solver, renderer, outDir + "/1-triangle.svg");
        render("直角三角形 (∠A=90°)", rightTriJson, parser, solver, renderer, outDir + "/2-right-tri.svg");
        render("圆 + 坐标系", circleJson, parser, solver, renderer, outDir + "/3-circle.svg");
        render("二次函数 y=x^2-2x+3", funcJson, parser, solver, renderer, outDir + "/4-parabola.svg");

        // ── 图5：复杂组合 — 向量 + 虚线圈 + 双函数 ──
        String complexJson = """
            {
              "elements": [
                {"type":"grid","xRange":[-4,4],"yRange":[-4,4],"step":1},
                {"type":"axis","xLabel":"x","yLabel":"y","tickStep":1},
                {"type":"point","id":"O","x":0,"y":0},
                {"type":"point","id":"A","x":2,"y":1},
                {"type":"point","id":"B","x":-1,"y":3},
                {"type":"segment","from":"O","to":"A","style":"arrow"},
                {"type":"segment","from":"O","to":"B","style":"arrow"},
                {"type":"label","at":"O","text":"O","offset":[-0.3,0.3]},
                {"type":"label","at":"A","text":"A","offset":[0.3,-0.2]},
                {"type":"label","at":"B","text":"B","offset":[-0.3,-0.2]},
                {"type":"line","through":["A","B"],"style":"dashed"},
                {"type":"circle","center":"O","radius":1.5,"style":"dashed"},
                {"type":"function-plot","expr":"x^2-2","xRange":[-2.5,2.5],"color":"#e91e63","samples":200},
                {"type":"function-plot","expr":"-x^2+2","xRange":[-2.5,2.5],"color":"#2ecc71","samples":200}
              ]
            }""";
        render("复杂组合 — 向量+虚线圈+双函数", complexJson, parser, solver, renderer, outDir + "/5-complex.svg");

        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8"><title>Phase 0 图形预览</title>
            <style>body{font-family:system-ui,sans-serif;background:#f5f5f7;padding:20px}
            h2{color:#4361ee}.card{background:#fff;border:0.5px solid #e0e0e0;border-radius:8px;
            padding:16px;margin:16px 0;}
            img{max-width:100%;height:auto;border:1px solid #eee}</style></head><body>
            <h1>Phase 0 — 数学图形渲染预览</h1>
            <div class="card"><h2>1. 三角形 (AB=6, AC=5, BC=4)</h2><img src="1-triangle.svg"></div>
            <div class="card"><h2>2. 直角三角形 (∠A=90°, AC=3, AB=4)</h2><img src="2-right-tri.svg"></div>
            <div class="card"><h2>3. 圆 + 坐标系 (圆心O(2,-1), r=3)</h2><img src="3-circle.svg"></div>
            <div class="card"><h2>4. 二次函数 y=x²-2x+3, 顶点V(1,2)</h2><img src="4-parabola.svg"></div>
            <div class="card"><h2>5. 复杂组合 — 向量箭头 + 虚线辅助线 + 双函数曲线</h2><img src="5-complex.svg"></div>
            </body></html>""";
        Files.writeString(Path.of(outDir, "index.html"), html);

        System.out.println("Done: " + Path.of(outDir, "index.html").toAbsolutePath());
    }

    static void render(String name, String json, GeometrySpecParser parser,
                       ConstraintSolver solver, SvgRenderer renderer, String path) throws Exception {
        GeometrySpec spec = parser.parse(json);
        solver.solve(spec);
        String svg = renderer.render(spec);
        Files.writeString(Path.of(path), svg);
        System.out.println(name + " → " + path + " (" + svg.length() + " chars)");
    }
}

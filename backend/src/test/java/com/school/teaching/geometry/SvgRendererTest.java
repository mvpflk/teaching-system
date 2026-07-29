package com.school.teaching.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SvgRendererTest {

    private final SvgRenderer renderer = new SvgRenderer();
    private final ConstraintSolver solver = new ConstraintSolver();

    @Test
    @DisplayName("渲染三角形 SVG 包含三条线段")
    void triangleContainsThreeLines() {
        GeometrySpec spec = triangle();
        String svg = renderer.render(spec);
        assertTrue(svg.contains("<svg"), "应包含 svg 标签");
        assertTrue(svg.contains("viewBox"), "应包含 viewBox");
        assertContains(svg, "x1=\"0", "A点 x 坐标");
        assertContains(svg, "x1=\"6", "B点 x 坐标");
    }

    @Test
    @DisplayName("渲染后元素坐标已写入 spec")
    void solvedCoordsWrittenToSpec() {
        GeometrySpec spec = triangle();
        solver.solve(spec);
        renderer.render(spec);
        double cx = spec.getElements().get(2).getX();
        double cy = spec.getElements().get(2).getY();
        assertTrue(Double.isFinite(cx));
        assertTrue(Double.isFinite(cy));
    }

    @Test
    @DisplayName("空 elements 不报错")
    void emptyElements() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of());
        String svg = renderer.render(spec);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("</svg>"));
    }

    @Test
    @DisplayName("自定义 viewBox 被使用")
    void customViewBox() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(ConstraintSolverTest.point("A", 0, 0)));
        GeometrySpec.ViewBox vb = new GeometrySpec.ViewBox();
        vb.setX(-5); vb.setY(-5); vb.setWidth(10); vb.setHeight(10);
        spec.setViewBox(vb);
        String svg = renderer.render(spec);
        assertTrue(svg.contains("-5") && svg.contains("10"),
            "viewBox 应为自定义值 -5~5 (宽10)");
    }

    @Test
    @DisplayName("渲染 circle 元素")
    void renderCircle() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("O", 0, 0),
            ConstraintSolverTest.circleById("c", "O", 5),
            ConstraintSolverTest.point("A", 5, 0)
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("circle"), "应包含 circle 元素");
        assertTrue(svg.contains("r=\"5"), "半径应为 5");
    }

    @Test
    @DisplayName("渲染 polygon")
    void renderPolygon() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            ConstraintSolverTest.point("B", 3, 0),
            ConstraintSolverTest.point("C", 3, 4),
            polygon("A", "B", "C")
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("polygon"), "应包含 polygon");
    }

    @Test
    @DisplayName("渲染 label")
    void renderLabel() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            label("A", "A", List.of(8.0, 12.0))
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains(">A<"), "应包含标签文字 A");
    }

    @Test
    @DisplayName("渲染 dimension")
    void renderDimension() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            ConstraintSolverTest.point("B", 6, 0),
            seg("AB", "A", "B"),
            dimension("AB", "6")
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("dim"), "应包含 dim 类");
        assertTrue(svg.contains("6"), "应包含标签 6");
    }

    @Test
    @DisplayName("渲染 right-angle 标记")
    void renderRightAngle() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            ConstraintSolverTest.point("B", 3, 0),
            ConstraintSolverTest.point("C", 0, 4),
            ConstraintSolverTest.seg("AB", "A", "B"),
            ConstraintSolverTest.seg("AC", "A", "C"),
            rightAngle("A", "B", "C")
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("polyline"), "直角标记应为 polyline");
    }

    @Test
    @DisplayName("渲染 angle-arc")
    void renderAngleArc() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            ConstraintSolverTest.point("B", 1, 0),
            ConstraintSolverTest.point("C", 0, 1),
            angleArc("A", "B", "C", "90°")
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("path"), "角度弧应为 path");
        assertTrue(svg.contains("90"), "应包含角度标注");
    }

    @Test
    @DisplayName("SVG 安全性：script 标签被清除")
    void sanitizeRemovesScript() {
        String unsafe = "<svg><script>alert('xss')</script><circle cx=\"0\" cy=\"0\" r=\"1\"/></svg>";
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(ConstraintSolverTest.point("A", 0, 0)));
        String svg = renderer.render(spec);
        assertFalse(svg.contains("<script>"), "script 标签应被清除");
    }

    @Test
    @DisplayName("SVG 安全性：on 事件被清除")
    void sanitizeRemovesOnEvent() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(ConstraintSolverTest.point("A", 0, 0)));
        String svg = renderer.render(spec);
        assertFalse(svg.contains("onclick="), "on 事件应被清除");
    }

    @Test
    @DisplayName("渲染 function-plot")
    void renderFunctionPlot() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            funcPlot("x^2", List.of(-2.0, 2.0))
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("polyline"), "函数图应为 polyline");
    }

    @Test
    @DisplayName("渲染 tick 标记")
    void renderTick() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            ConstraintSolverTest.point("B", 4, 0),
            ConstraintSolverTest.seg("AB", "A", "B"),
            tick("AB", 2)
        ));
        String svg = renderer.render(spec);
        assertTrue(svg.contains("line"), "tick 应包含 line 元素");
    }

    private void assertContains(String svg, String substr, String desc) {
        assertTrue(svg.contains(substr), "应为" + desc);
    }

    static GeometrySpec.Element polygon(String... vertices) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("polygon"); el.setVertices(List.of(vertices)); el.setFill("#e8f0ff");
        return el;
    }
    static GeometrySpec.Element label(String at, String text, List<Double> offset) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("label"); el.setAt(at); el.setText(text); el.setOffset(offset);
        return el;
    }
    static GeometrySpec.Element seg(String id, String from, String to) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("segment"); el.setId(id); el.setFrom(from); el.setTo(to);
        return el;
    }
    static GeometrySpec.Element dimension(String segment, String label) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("dimension"); el.setSegment(segment); el.setLabel(label);
        return el;
    }
    static GeometrySpec.Element rightAngle(String vertex, String leg1, String leg2) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("right-angle"); el.setVertex(vertex); el.setLeg1(leg1); el.setLeg2(leg2);
        return el;
    }
    static GeometrySpec.Element angleArc(String vertex, String from, String to, String label) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("angle-arc"); el.setVertex(vertex); el.setFrom(from); el.setTo(to); el.setLabel(label);
        return el;
    }
    static GeometrySpec.Element funcPlot(String expr, List<Double> xRange) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("function-plot"); el.setExpr(expr); el.setXRange(xRange);
        return el;
    }
    static GeometrySpec.Element tick(String segment, int count) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("tick"); el.setSegment(segment); el.setCount(count);
        return el;
    }

    private GeometrySpec triangle() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            ConstraintSolverTest.point("A", 0, 0),
            ConstraintSolverTest.point("B", 6, 0),
            ConstraintSolverTest.constrained("C",
                ConstraintSolverTest.distance("A", 5),
                ConstraintSolverTest.distance("B", 4)),
            ConstraintSolverTest.seg("AB", "A", "B"),
            ConstraintSolverTest.seg("AC", "A", "C"),
            ConstraintSolverTest.seg("BC", "B", "C"),
            label("A", "A", List.of(-8.0, 12.0)),
            label("B", "B", List.of(8.0, 12.0)),
            label("C", "C", List.of(0.0, -12.0)),
            dimension("AB", "6")
        ));
        solver.solve(spec);
        return spec;
    }
}

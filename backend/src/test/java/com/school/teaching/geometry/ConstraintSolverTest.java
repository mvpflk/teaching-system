package com.school.teaching.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintSolverTest {

    private final ConstraintSolver solver = new ConstraintSolver();

    @Test
    @DisplayName("三角形：两距离约束求解第三点")
    void triangleByDistance() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0),
            point("B", 6, 0),
            constrained("C", distance("A", 5), distance("B", 4))
        ));
        solver.solve(spec);
        double[] c = solver.getPoint("C");
        // A(0,0), B(6,0), AC=5, BC=4 → circle intersection, index=0 gives lower point
        assertEquals(3.75, c[0], 1e-6);
        assertEquals(-Math.sqrt(5*5 - 3.75*3.75), c[1], 1e-6);
    }

    @Test
    @DisplayName("中点约束")
    void midpoint() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0),
            point("B", 8, 4),
            constrained("M", midpoint("A", "B"))
        ));
        solver.solve(spec);
        double[] m = solver.getPoint("M");
        assertEquals(4.0, m[0], 1e-6);
        assertEquals(2.0, m[1], 1e-6);
    }

    @Test
    @DisplayName("垂足约束")
    void foot() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0),
            point("B", 4, 0),
            seg("AB", "A", "B"),
            point("C", 2, 3),
            constrained("F", foot("C", "AB"))
        ));
        solver.solve(spec);
        double[] f = solver.getPoint("F");
        assertEquals(2.0, f[0], 1e-6);
        assertEquals(0.0, f[1], 1e-6);
    }

    @Test
    @DisplayName("线段交点")
    void intersection() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0), point("B", 4, 4),
            point("C", 0, 4), point("D", 4, 0),
            seg("s1", "A", "B"),
            seg("s2", "C", "D"),
            constrained("I", intersection("s1", "s2"))
        ));
        solver.solve(spec);
        double[] i = solver.getPoint("I");
        assertEquals(2.0, i[0], 1e-6);
        assertEquals(2.0, i[1], 1e-6);
    }

    @Test
    @DisplayName("圆上点（极坐标）")
    void onCircle() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("O", 0, 0),
            circleById("c", "O", 5),
            constrained("P", onCircle("c", 90))
        ));
        solver.solve(spec);
        double[] p = solver.getPoint("P");
        assertEquals(0.0, p[0], 1e-6);
        assertEquals(5.0, p[1], 1e-6);
    }

    @Test
    @DisplayName("线段上点（比例）")
    void onSegment() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0), point("B", 10, 0),
            seg("AB", "A", "B"),
            constrained("M", onSegment("AB", 0.3))
        ));
        solver.solve(spec);
        double[] m = solver.getPoint("M");
        assertEquals(3.0, m[0], 1e-6);
        assertEquals(0.0, m[1], 1e-6);
    }

    @Test
    @DisplayName("纯循环依赖无锚点应抛异常")
    void circularDependencyThrows() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0),
            constrained("B", midpoint("C", "A")),
            constrained("C", midpoint("B", "A"))
        ));
        assertThrows(GeometryException.class, () -> solver.solve(spec));
    }

    @Test
    @DisplayName("无法求解的约束应抛异常（三角形不等式不成立）")
    void unsolvableConstraint() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0),
            point("B", 2, 0),
            constrained("C", distance("A", 10), distance("B", 1))
        ));
        assertThrows(GeometryException.class, () -> solver.solve(spec));
    }

    @Test
    @DisplayName("平行线无交点应抛异常")
    void parallelLinesNoIntersection() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("A", 0, 0), point("B", 1, 0),
            point("C", 0, 1), point("D", 1, 1),
            seg("s1", "A", "B"),
            seg("s2", "C", "D"),
            constrained("I", intersection("s1", "s2"))
        ));
        assertThrows(GeometryException.class, () -> solver.solve(spec));
    }

    @Test
    @DisplayName("两圆交点")
    void circleIntersection() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(
            point("O1", 0, 0), point("O2", 4, 0),
            circleById("c1", "O1", 3),
            circleById("c2", "O2", 3),
            constrained("P", circleIntersection("c1", "c2", 1))
        ));
        solver.solve(spec);
        double[] p = solver.getPoint("P");
        assertEquals(2.0, p[0], 1e-6);
        assertEquals(Math.sqrt(5), p[1], 1e-6);
    }

    @Test
    @DisplayName("viewBox 和 style 透传")
    void viewBoxAndStylePassthrough() {
        GeometrySpec spec = new GeometrySpec();
        spec.setElements(List.of(point("A", 0, 0)));
        GeometrySpec.ViewBox vb = new GeometrySpec.ViewBox();
        vb.setX(-1); vb.setY(-1); vb.setWidth(8); vb.setHeight(6);
        spec.setViewBox(vb);
        GeometrySpec.Style style = new GeometrySpec.Style();
        style.setStrokeColor("#ff0000");
        style.setLabelFontSize(16.0);
        spec.setStyle(style);
        solver.solve(spec);
        assertEquals(-1, spec.getViewBox().getX());
        assertEquals("#ff0000", spec.getStyle().getStrokeColor());
    }

    static Map<String, Object> distance(String target, double d) {
        return Map.of("distance", List.of(target, d));
    }
    static Map<String, Object> midpoint(String p1, String p2) {
        return Map.of("midpoint", List.of(p1, p2));
    }
    static Map<String, Object> foot(String point, String seg) {
        return Map.of("foot", List.of(point, seg));
    }
    static Map<String, Object> intersection(String l1, String l2) {
        return Map.of("intersection", List.of(l1, l2));
    }
    static Map<String, Object> onCircle(String circle, double angleDeg) {
        return Map.of("onCircle", List.of(circle, angleDeg));
    }
    static Map<String, Object> onSegment(String seg, double t) {
        return Map.of("onSegment", List.of(seg, t));
    }
    static Map<String, Object> circleIntersection(String c1, String c2, int index) {
        return Map.of("circleIntersection", List.of(c1, c2, index));
    }
    static GeometrySpec.Element point(String id, double x, double y) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("point"); el.setId(id); el.setX(x); el.setY(y);
        return el;
    }
    static GeometrySpec.Element constrained(String id, Map<String, Object>... constraints) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("point"); el.setId(id);
        el.setConstraints(List.of(constraints));
        return el;
    }
    static GeometrySpec.Element seg(String id, String from, String to) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("segment"); el.setId(id); el.setFrom(from); el.setTo(to);
        return el;
    }
    static GeometrySpec.Element circleById(String id, String center, double radius) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType("circle"); el.setId(id); el.setCenter(center); el.setRadius(radius);
        return el;
    }
}

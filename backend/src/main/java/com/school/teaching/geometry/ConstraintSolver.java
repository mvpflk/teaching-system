package com.school.teaching.geometry;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ConstraintSolver {

    private static final int MAX_ITER = 20;
    private final Map<String, double[]> solved = new LinkedHashMap<>();
    private final Map<String, GeometrySpec.Element> pointMap = new LinkedHashMap<>();
    private final Map<String, GeometrySpec.Element> segMap = new LinkedHashMap<>();
    private final Map<String, GeometrySpec.Element> circleMap = new LinkedHashMap<>();

    public void solve(GeometrySpec spec) {
        for (GeometrySpec.Element el : spec.getElements()) {
            if ("point".equals(el.getType())) pointMap.put(el.getId(), el);
            if (el.getId() != null && "segment".equals(el.getType())) segMap.put(el.getId(), el);
            if (el.getId() != null && "circle".equals(el.getType())) circleMap.put(el.getId(), el);
        }
        for (GeometrySpec.Element el : spec.getElements()) {
            if ("point".equals(el.getType())) {
                if (el.getX() != null && el.getY() != null) {
                    solved.put(el.getId(), new double[]{el.getX(), el.getY()});
                }
            }
        }
        boolean progress = true;
        for (int iter = 0; iter < MAX_ITER && progress; iter++) {
            progress = false;
            for (GeometrySpec.Element el : spec.getElements()) {
                if (!"point".equals(el.getType())) continue;
                if (solved.containsKey(el.getId())) continue;
                if (el.getConstraints() == null || el.getConstraints().isEmpty()) continue;
                if (trySolve(el)) {
                    progress = true;
                }
            }
        }
        for (GeometrySpec.Element el : spec.getElements()) {
            if ("point".equals(el.getType()) && !solved.containsKey(el.getId())) {
                if (el.getConstraints() != null && !el.getConstraints().isEmpty()) {
                    throw new GeometryException("无法求解点 " + el.getId() + " 的约束（数据不足或循环依赖）");
                }
            }
        }
        for (GeometrySpec.Element el : spec.getElements()) {
            if ("point".equals(el.getType()) && solved.containsKey(el.getId())) {
                double[] xy = solved.get(el.getId());
                el.setX(xy[0]);
                el.setY(xy[1]);
            }
        }
    }

    public double[] getPoint(String id) {
        double[] p = solved.get(id);
        if (p == null) throw new GeometryException("点 " + id + " 未求解");
        return p;
    }

    private boolean trySolve(GeometrySpec.Element el) {
        if (el.getConstraints() == null || el.getConstraints().isEmpty()) return false;
        for (Map<String, Object> c : el.getConstraints()) {
            if (c.containsKey("midpoint")) return solveMidpoint(el, c);
            if (c.containsKey("distance")) return solveDistance(el, c);
            if (c.containsKey("foot")) return solveFoot(el, c);
            if (c.containsKey("intersection")) return solveIntersection(el, c);
            if (c.containsKey("circleIntersection")) return solveCircleIntersection(el, c);
            if (c.containsKey("onCircle")) return solveOnCircle(el, c);
            if (c.containsKey("onSegment")) return solveOnSegment(el, c);
        }
        return false;
    }

    private boolean solveMidpoint(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "midpoint");
        if (params == null || params.size() < 2) return false;
        String p1 = params.get(0).toString(), p2 = params.get(1).toString();
        if (!solved.containsKey(p1) || !solved.containsKey(p2)) return false;
        double[] a = solved.get(p1), b = solved.get(p2);
        solved.put(el.getId(), new double[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2});
        return true;
    }

    private boolean solveDistance(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "distance");
        if (params == null || params.size() < 2) return false;
        String p1 = params.get(0).toString();
        double d = ((Number) params.get(1)).doubleValue();
        if (d <= 0) return false;
        if (!solved.containsKey(p1)) return false;
        double[] a = solved.get(p1);

        if (el.getConstraints().size() >= 2) {
            Map<String, Object> c2 = null;
            for (Map<String, Object> cc : el.getConstraints()) {
                if (cc != c && cc.containsKey("distance")) { c2 = cc; break; }
            }
            if (c2 != null) {
                List<?> p2 = getListParam(c2, "distance");
                if (p2 != null && p2.size() >= 2) {
                    String p2Id = p2.get(0).toString();
                    double d2 = ((Number) p2.get(1)).doubleValue();
                    if (solved.containsKey(p2Id)) {
                        double[] b = solved.get(p2Id);
                        double ab = dist(a, b);
                        if (ab > d + d2 || ab < Math.abs(d - d2)) return false;
                        double[] result = circleIntersection(a, b, d, d2, 0);
                        if (result != null) { solved.put(el.getId(), result); return true; }
                    }
                }
            }
        }
        return false;
    }

    private boolean solveFoot(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "foot");
        if (params == null || params.size() < 2) return false;
        String pointId = params.get(0).toString();
        String segId = params.get(1).toString();
        if (!solved.containsKey(pointId)) return false;
        GeometrySpec.Element seg = segMap.get(segId);
        if (seg == null) return false;
        if (!solved.containsKey(seg.getFrom()) || !solved.containsKey(seg.getTo())) return false;
        double[] p = solved.get(pointId);
        double[] a = solved.get(seg.getFrom()), b = solved.get(seg.getTo());
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return false;
        double t = ((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / lenSq;
        double fx = a[0] + t * dx, fy = a[1] + t * dy;
        solved.put(el.getId(), new double[]{fx, fy});
        return true;
    }

    private boolean solveIntersection(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "intersection");
        if (params == null || params.size() < 2) return false;
        String id1 = params.get(0).toString(), id2 = params.get(1).toString();
        GeometrySpec.Element line1 = findLine(id1);
        GeometrySpec.Element line2 = findLine(id2);
        if (line1 == null || line2 == null) return false;
        double[] p1 = getLinePoint(line1, 0), p2 = getLinePoint(line1, 1);
        double[] q1 = getLinePoint(line2, 0), q2 = getLinePoint(line2, 1);
        if (p1 == null || p2 == null || q1 == null || q2 == null) return false;
        double denom = (p1[0] - p2[0]) * (q1[1] - q2[1]) - (p1[1] - p2[1]) * (q1[0] - q2[0]);
        if (Math.abs(denom) < 1e-10) return false;
        double ix = ((p1[0] * p2[1] - p1[1] * p2[0]) * (q1[0] - q2[0]) - (p1[0] - p2[0]) * (q1[0] * q2[1] - q1[1] * q2[0])) / denom;
        double iy = ((p1[0] * p2[1] - p1[1] * p2[0]) * (q1[1] - q2[1]) - (p1[1] - p2[1]) * (q1[0] * q2[1] - q1[1] * q2[0])) / denom;
        solved.put(el.getId(), new double[]{ix, iy});
        return true;
    }

    private boolean solveCircleIntersection(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "circleIntersection");
        if (params == null || params.size() < 3) return false;
        String c1Id = params.get(0).toString(), c2Id = params.get(1).toString();
        int index = ((Number) params.get(2)).intValue();
        GeometrySpec.Element c1 = circleMap.get(c1Id);
        GeometrySpec.Element c2 = circleMap.get(c2Id);
        if (c1 == null || c2 == null) return false;
        if (!solved.containsKey(c1.getCenter()) || !solved.containsKey(c2.getCenter())) return false;
        double[] o1 = solved.get(c1.getCenter()), o2 = solved.get(c2.getCenter());
        double r1 = c1.getRadius() != null ? c1.getRadius() : (c1.getDiameter() != null ? dist(solved.get(c1.getDiameter().get(0)), solved.get(c1.getDiameter().get(1))) / 2 : 0);
        double r2 = c2.getRadius() != null ? c2.getRadius() : (c2.getDiameter() != null ? dist(solved.get(c2.getDiameter().get(0)), solved.get(c2.getDiameter().get(1))) / 2 : 0);
        double[] result = circleIntersection(o1, o2, r1, r2, index);
        if (result == null) return false;
        solved.put(el.getId(), result);
        return true;
    }

    private boolean solveOnCircle(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "onCircle");
        if (params == null || params.size() < 2) return false;
        String circleId = params.get(0).toString();
        double angleDeg = ((Number) params.get(1)).doubleValue();
        GeometrySpec.Element circ = circleMap.get(circleId);
        if (circ == null || !solved.containsKey(circ.getCenter())) return false;
        double[] center = solved.get(circ.getCenter());
        double r = circ.getRadius() != null ? circ.getRadius() : (circ.getDiameter() != null ? dist(solved.get(circ.getDiameter().get(0)), solved.get(circ.getDiameter().get(1))) / 2 : 0);
        double rad = Math.toRadians(angleDeg);
        solved.put(el.getId(), new double[]{center[0] + r * Math.cos(rad), center[1] + r * Math.sin(rad)});
        return true;
    }

    private boolean solveOnSegment(GeometrySpec.Element el, Map<String, Object> c) {
        List<?> params = getListParam(c, "onSegment");
        if (params == null || params.size() < 2) return false;
        String segId = params.get(0).toString();
        double t = ((Number) params.get(1)).doubleValue();
        GeometrySpec.Element seg = segMap.get(segId);
        if (seg == null || !solved.containsKey(seg.getFrom()) || !solved.containsKey(seg.getTo())) return false;
        double[] a = solved.get(seg.getFrom()), b = solved.get(seg.getTo());
        solved.put(el.getId(), new double[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1])});
        return true;
    }

    private double[] circleIntersection(double[] o1, double[] o2, double r1, double r2, int index) {
        double dx = o2[0] - o1[0], dy = o2[1] - o1[1];
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d > r1 + r2 || d < Math.abs(r1 - r2) || d == 0) return null;
        double a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
        double h = Math.sqrt(Math.max(0, r1 * r1 - a * a));
        double mx = o1[0] + a * dx / d, my = o1[1] + a * dy / d;
        double sign = index == 0 ? 1 : -1;  // index=0 取下侧交点, index=1 取上侧交点
        return new double[]{mx + sign * h * dy / d, my - sign * h * dx / d};
    }

    private GeometrySpec.Element findLine(String id) {
        GeometrySpec.Element el = segMap.get(id);
        if (el != null) return el;
        for (GeometrySpec.Element e : pointMap.values()) {
            if (id.equals(e.getId())) {
                GeometrySpec.Element fake = new GeometrySpec.Element();
                fake.setId(id);
                fake.setFrom(id);
                if (solved.containsKey(id)) {
                    String other = pointMap.keySet().stream().filter(k -> !k.equals(id) && solved.containsKey(k)).findFirst().orElse(null);
                    if (other != null) fake.setTo(other);
                }
                return fake;
            }
        }
        return null;
    }

    private double[] getLinePoint(GeometrySpec.Element line, int idx) {
        if ("segment".equals(line.getType()) || "line".equals(line.getType()) || "ray".equals(line.getType())) {
            String id = idx == 0 ? line.getFrom() : line.getTo();
            if (id != null && solved.containsKey(id)) return solved.get(id);
            if (line.getThrough() != null && line.getThrough().size() > idx) {
                String tid = line.getThrough().get(idx);
                if (solved.containsKey(tid)) return solved.get(tid);
            }
        }
        if (line.getThrough() != null && line.getThrough().size() > idx) {
            String tid = line.getThrough().get(idx);
            if (solved.containsKey(tid)) return solved.get(tid);
        }
        if (idx == 0 && line.getId() != null && solved.containsKey(line.getId())) return solved.get(line.getId());
        if (idx == 1) {
            String other = pointMap.keySet().stream().filter(k -> !k.equals(line.getId()) && solved.containsKey(k)).findFirst().orElse(null);
            if (other != null) return solved.get(other);
        }
        return null;
    }

    private List<?> getListParam(Map<String, Object> c, String key) {
        Object val = c.get(key);
        if (val instanceof List<?> l) return l;
        return null;
    }

    static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }
}

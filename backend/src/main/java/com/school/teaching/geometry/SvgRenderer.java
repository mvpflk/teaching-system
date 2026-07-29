package com.school.teaching.geometry;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SvgRenderer {

    private static final Set<String> SAFE_FUNCS = Set.of(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sqrt", "abs", "log", "log10", "pow", "ceil", "floor"
    );

    public String render(GeometrySpec spec) {
        List<GeometrySpec.Element> elements = spec.getElements();
        Map<String, double[]> pointCoords = new LinkedHashMap<>();
        Map<String, GeometrySpec.Element> segmentsById = new LinkedHashMap<>();
        for (GeometrySpec.Element el : elements) {
            if ("point".equals(el.getType()) && el.getX() != null && el.getY() != null) {
                pointCoords.put(el.getId(), new double[]{el.getX(), el.getY()});
            }
            if ("segment".equals(el.getType()) && el.getId() != null) {
                segmentsById.put(el.getId(), el);
            }
        }

        // 翻转 y 轴：数学坐标系 (y↑) → SVG 坐标系 (y↓)
        for (double[] xy : pointCoords.values()) {
            xy[1] = -xy[1];
        }

        double[] bbox = computeBBox(elements, pointCoords);
        double minX = bbox[0], minY = bbox[1], maxX = bbox[2], maxY = bbox[3];
        double padX = Math.max((maxX - minX) * SvgStyles.PADDING_RATIO, 0.5);
        double padY = Math.max((maxY - minY) * SvgStyles.PADDING_RATIO, 0.5);
        if (spec.getViewBox() != null) {
            minX = spec.getViewBox().getX();
            minY = spec.getViewBox().getY();
            padX = 0; padY = 0;
            maxX = minX + spec.getViewBox().getWidth();
            maxY = minY + spec.getViewBox().getHeight();
        }

        String sc = spec.getStyle() != null && spec.getStyle().getStrokeColor() != null
            ? spec.getStyle().getStrokeColor() : SvgStyles.PRIMARY_COLOR;
        double lfs = spec.getStyle() != null && spec.getStyle().getLabelFontSize() != null
            ? spec.getStyle().getLabelFontSize() : SvgStyles.LABEL_FONT_SIZE;
        String gc = spec.getStyle() != null && spec.getStyle().getGridColor() != null
            ? spec.getStyle().getGridColor() : SvgStyles.GRID_COLOR;

        StringBuilder svg = new StringBuilder();
        // SVG 箭头标记（供线段/射线复用）
        String arrowMarker = "<marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"8\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\">"
            + "<path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"" + sc + "\"/></marker>";
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"")
            .append(minX - padX).append(" ").append(minY - padY).append(" ")
            .append(maxX - minX + padX * 2).append(" ").append(maxY - minY + padY * 2)
            .append("\" font-family=\"").append(SvgStyles.FONT_FAMILY).append("\">\n");
        svg.append("<defs>").append(arrowMarker).append("<style>\n")
            .append("  @media print { .dim { stroke: #000 !important; fill: #000 !important; } }\n")
            .append("</style></defs>\n");

        for (GeometrySpec.Element el : elements) {
            switch (el.getType()) {
                case "grid" -> renderGrid(svg, el, gc);
                case "axis" -> renderAxis(svg, el, sc, lfs, elements);
                case "function-plot" -> renderFunctionPlot(svg, el, pointCoords, bbox);
                case "circle" -> renderCircle(svg, el, pointCoords, sc);
                case "arc" -> renderArc(svg, el, pointCoords, sc);
                case "polygon" -> renderPolygon(svg, el, pointCoords, sc);
                case "segment" -> renderSegment(svg, el, pointCoords, sc);
                case "line" -> renderLine(svg, el, pointCoords, sc, bbox);
                case "ray" -> renderRay(svg, el, pointCoords, sc, bbox);
                case "angle-arc" -> renderAngleArc(svg, el, pointCoords, sc, lfs);
                case "right-angle" -> renderRightAngle(svg, el, pointCoords, sc);
                case "dimension" -> renderDimension(svg, el, pointCoords, sc, segmentsById);
                case "tick" -> renderTick(svg, el, pointCoords, sc, segmentsById);
                case "label" -> renderLabel(svg, el, pointCoords, sc, lfs);
            }
        }

        svg.append("</svg>");
        return sanitize(svg.toString());
    }

    private void renderGrid(StringBuilder svg, GeometrySpec.Element el, String color) {
        List<Double> xr = el.getXRange(), yr = el.getYRange();
        double step = el.getStep() != null ? el.getStep() : 1;
        if (xr == null || xr.size() < 2 || yr == null || yr.size() < 2) return;
        // y 轴已翻转 → 网格线 y 范围取反
        double y1 = -yr.get(0), y2 = -yr.get(1);
        double yMin = Math.min(y1, y2), yMax = Math.max(y1, y2);
        for (double x = Math.ceil(xr.get(0) / step) * step; x <= xr.get(1); x += step) {
            svg.append(format("<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"%.3f\"/>\n",
                x, yMin, x, yMax, color, SvgStyles.GRID_STROKE_WIDTH));
        }
        for (double y = Math.ceil(yMin / step) * step; y <= yMax; y += step) {
            svg.append(format("<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"%.3f\"/>\n",
                xr.get(0), y, xr.get(1), y, color, SvgStyles.GRID_STROKE_WIDTH));
        }
    }

    private void renderAxis(StringBuilder svg, GeometrySpec.Element el, String color, double fontSize,
                            List<GeometrySpec.Element> allElements) {
        List<Double> xr = el.getXRange(), yr = el.getYRange();
        // 自动从 grid 元素继承范围
        if ((xr == null || yr == null) && allElements != null) {
            for (GeometrySpec.Element other : allElements) {
                if ("grid".equals(other.getType())) {
                    if (xr == null) xr = other.getXRange();
                    if (yr == null) yr = other.getYRange();
                    if (xr != null && yr != null) break;
                }
            }
        }
        if (xr != null && xr.size() >= 2) {
            svg.append(format("<line x1=\"%.1f\" y1=\"0\" x2=\"%.1f\" y2=\"0\" stroke=\"%s\" stroke-width=\"%.3f\"/>\n",
                xr.get(0), xr.get(1), color, SvgStyles.AXIS_STROKE_WIDTH));
        }
        if (yr != null && yr.size() >= 2) {
            double fy0 = -yr.get(0), fy1 = -yr.get(1);
            svg.append(format("<line x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"%.3f\"/>\n",
                Math.min(fy0, fy1), Math.max(fy0, fy1), color, SvgStyles.AXIS_STROKE_WIDTH));
        }
        // 坐标轴标签
        if (el.getXLabel() != null && xr != null && xr.size() >= 2) {
            svg.append(format("<text x=\"%.1f\" y=\"%.2f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"end\">%s</text>\n",
                xr.get(1), SvgStyles.TICK_LENGTH + SvgStyles.TICK_FONT_SIZE * 2.5, SvgStyles.LABEL_FONT_SIZE, color, el.getXLabel()));
        }
        if (el.getYLabel() != null && yr != null && yr.size() >= 2) {
            double fyTop = -Math.max(yr.get(0), yr.get(1)); // y 轴顶端（翻转后）
            svg.append(format("<text x=\"%.2f\" y=\"%.1f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"start\">%s</text>\n",
                SvgStyles.TICK_LENGTH + 0.05, fyTop - 0.2, SvgStyles.LABEL_FONT_SIZE, color, el.getYLabel()));
        }
        double tickStep = el.getTickStep() != null ? el.getTickStep() : 0;
        if (tickStep > 0) {
            double rx1 = xr != null ? xr.get(0) : -5, rx2 = xr != null ? xr.get(1) : 5;
            double ry1 = yr != null ? -yr.get(0) : -5, ry2 = yr != null ? -yr.get(1) : 5;
            double ryMin = Math.min(ry1, ry2), ryMax = Math.max(ry1, ry2);
            double tl = SvgStyles.TICK_LENGTH;
            for (double x = Math.ceil(rx1 / tickStep) * tickStep; x <= rx2; x += tickStep) {
                if (Math.abs(x) < 1e-10) continue;
                svg.append(format("<line x1=\"%.1f\" y1=\"%.3f\" x2=\"%.1f\" y2=\"%.3f\" stroke=\"%s\" stroke-width=\"%.3f\"/>\n",
                    x, -tl, x, tl, color, SvgStyles.GRID_STROKE_WIDTH));
                svg.append(format("<text x=\"%.1f\" y=\"%.2f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"middle\">%s</text>\n",
                    x, tl + SvgStyles.TICK_FONT_SIZE * 1.5, SvgStyles.TICK_FONT_SIZE, color, fmtNum(x)));
            }
            for (double y = Math.ceil(ryMin / tickStep) * tickStep; y <= ryMax; y += tickStep) {
                if (Math.abs(y) < 1e-10) continue;
                // y 标签显示原始数学值（翻转回来）
                double mathY = -y;
                svg.append(format("<line x1=\"%.3f\" y1=\"%.1f\" x2=\"%.3f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"%.3f\"/>\n",
                    -tl, y, tl, y, color, SvgStyles.GRID_STROKE_WIDTH));
                svg.append(format("<text x=\"%.3f\" y=\"%.1f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"end\">%s</text>\n",
                    -tl - 0.05, y + SvgStyles.TICK_FONT_SIZE * 0.35, SvgStyles.TICK_FONT_SIZE, color, fmtNum(mathY)));
            }
        }
    }

    private void renderFunctionPlot(StringBuilder svg, GeometrySpec.Element el,
                                     Map<String, double[]> pointCoords, double[] bbox) {
        String expr = el.getExpr();
        if (expr == null || expr.isBlank()) return;
        List<Double> xr = el.getXRange() != null ? el.getXRange() : List.of(-5.0, 5.0);
        int samples = el.getSamples() != null ? el.getSamples() : 200;
        String color = el.getColor() != null ? el.getColor() : SvgStyles.ACCENT_COLOR;
        try {
            List<String> points = new ArrayList<>();
            for (int i = 0; i <= samples; i++) {
                double x = xr.get(0) + (xr.get(1) - xr.get(0)) * i / samples;
                double y = -SimpleExprEval.eval(expr, x);  // flip to SVG coords
                if (Double.isFinite(y)) {
                    points.add(format("%.4f,%.4f", x, y));
                }
            }
            if (points.size() >= 2) {
                svg.append("<polyline points=\"").append(String.join(" ", points))
                    .append("\" stroke=\"").append(color).append("\" stroke-width=\"")
                    .append(SvgStyles.STROKE_WIDTH_BOLD).append("\" fill=\"none\"/>\n");
            }
        } catch (Exception ignored) {
        }
    }

    private void renderCircle(StringBuilder svg, GeometrySpec.Element el,
                               Map<String, double[]> pointCoords, String color) {
        double[] center = getCoord(el.getCenter(), pointCoords);
        if (center == null) return;
        double r = el.getRadius() != null ? el.getRadius() : 0;
        if (r == 0 && el.getDiameter() != null && el.getDiameter().size() >= 2) {
            double[] a = getCoord(el.getDiameter().get(0), pointCoords);
            double[] b = getCoord(el.getDiameter().get(1), pointCoords);
            if (a != null && b != null) r = dist(a, b) / 2;
        }
        if (r == 0) return;
        String sw = String.valueOf(SvgStyles.STROKE_WIDTH_NORMAL);
        String dash = "dashed".equals(el.getStyle()) ? " stroke-dasharray=\"0.3,0.2\"" : "dotted".equals(el.getStyle()) ? " stroke-dasharray=\"0.1,0.15\"" : "";
        svg.append(format("<circle cx=\"%.4f\" cy=\"%.4f\" r=\"%.4f\" stroke=\"%s\" stroke-width=\"%s\" fill=\"none\"%s/>\n",
            center[0], center[1], r, color, sw, dash));
    }

    private void renderArc(StringBuilder svg, GeometrySpec.Element el,
                            Map<String, double[]> pointCoords, String color) {
        double[] c = getCoord(el.getCenter(), pointCoords);
        double[] from = getCoord(el.getFrom(), pointCoords);
        double[] to = getCoord(el.getTo(), pointCoords);
        if (c == null || from == null || to == null) return;
        double r = dist(c, from);
        double a1 = Math.toDegrees(Math.atan2(from[1] - c[1], from[0] - c[0]));
        double a2 = Math.toDegrees(Math.atan2(to[1] - c[1], to[0] - c[0]));
        if (a2 < a1) a2 += 360;
        String dash = "dashed".equals(el.getStyle()) ? " stroke-dasharray=\"0.3,0.2\"" : "";
        svg.append(format("<path d=\"M %.4f %.4f A %.4f %.4f 0 %d 1 %.4f %.4f\" stroke=\"%s\" stroke-width=\"%.2f\" fill=\"none\"%s/>\n",
            from[0], from[1], r, r, (a2 - a1 >= 180 ? 1 : 0),
            to[0], to[1], color, SvgStyles.STROKE_WIDTH_NORMAL, dash));
    }

    private void renderPolygon(StringBuilder svg, GeometrySpec.Element el,
                                Map<String, double[]> pointCoords, String color) {
        List<String> pts = new ArrayList<>();
        for (String v : el.getVertices()) {
            double[] c = getCoord(v, pointCoords);
            if (c == null) return;
            pts.add(format("%.4f,%.4f", c[0], c[1]));
        }
        if (pts.size() < 3) return;
        String fill = el.getFill() != null ? el.getFill() : SvgStyles.FILL_COLOR;
        svg.append("<polygon points=\"").append(String.join(" ", pts))
            .append("\" stroke=\"").append(color).append("\" stroke-width=\"")
            .append(SvgStyles.STROKE_WIDTH_NORMAL).append("\" fill=\"").append(fill).append("\"/>\n");
    }

    private void renderSegment(StringBuilder svg, GeometrySpec.Element el,
                                Map<String, double[]> pointCoords, String color) {
        double[] from = getCoord(el.getFrom(), pointCoords);
        double[] to = getCoord(el.getTo(), pointCoords);
        if (from == null || to == null) return;
        String style = el.getStyle() != null ? el.getStyle() : "solid";
        String dash = "dashed".equals(style) ? " stroke-dasharray=\"0.3,0.2\"" : "dotted".equals(style) ? " stroke-dasharray=\"0.1,0.15\"" : "";
        // 箭头：style 含 "arrow" 或 "vector" 时加箭头
        boolean arrow = style != null && (style.contains("arrow") || style.contains("vector"));
        String marker = arrow ? " marker-end=\"url(#arrow)\"" : "";
        svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\"%s%s/>\n",
            from[0], from[1], to[0], to[1], color, SvgStyles.STROKE_WIDTH_NORMAL, dash, marker));
    }

    private void renderLine(StringBuilder svg, GeometrySpec.Element el,
                             Map<String, double[]> pointCoords, String color, double[] bbox) {
        double[] a = null, b = null;
        if (el.getThrough() != null && el.getThrough().size() >= 2) {
            a = getCoord(el.getThrough().get(0), pointCoords);
            b = getCoord(el.getThrough().get(1), pointCoords);
        } else if (el.getFrom() != null && el.getTo() != null) {
            a = getCoord(el.getFrom(), pointCoords);
            b = getCoord(el.getTo(), pointCoords);
        }
        if (a == null || b == null) return;
        double[] ext = extendLine(a, b, bbox);
        String dash = "dashed".equals(el.getStyle()) ? " stroke-dasharray=\"0.3,0.2\"" : "";
        svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\"%s/>\n",
            ext[0], ext[1], ext[2], ext[3], color, SvgStyles.STROKE_WIDTH_NORMAL, dash));
    }

    private void renderRay(StringBuilder svg, GeometrySpec.Element el,
                            Map<String, double[]> pointCoords, String color, double[] bbox) {
        double[] from = getCoord(el.getFrom(), pointCoords);
        double[] to = getCoord(el.getTo(), pointCoords);
        if (from == null || to == null) {
            if (el.getThrough() != null && el.getThrough().size() >= 2) {
                from = getCoord(el.getThrough().get(0), pointCoords);
                to = getCoord(el.getThrough().get(1), pointCoords);
            }
        }
        if (from == null || to == null) return;
        double dx = to[0] - from[0], dy = to[1] - from[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        double maxDim = Math.max(bbox[2] - bbox[0], bbox[3] - bbox[1]) * 2;
        double ex = from[0] + dx / len * maxDim, ey = from[1] + dy / len * maxDim;
        svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\"/>\n",
            from[0], from[1], ex, ey, color, SvgStyles.STROKE_WIDTH_NORMAL));
    }

    private void renderAngleArc(StringBuilder svg, GeometrySpec.Element el,
                                 Map<String, double[]> pointCoords, String color, double fontSize) {
        double[] v = getCoord(el.getVertex(), pointCoords);
        double[] f = getCoord(el.getFrom(), pointCoords);
        double[] t = getCoord(el.getTo(), pointCoords);
        if (v == null || f == null || t == null) return;
        double a1 = Math.atan2(f[1] - v[1], f[0] - v[0]);
        double a2 = Math.atan2(t[1] - v[1], t[0] - v[0]);
        double r = SvgStyles.ANGLE_ARC_RADIUS;
        int large = 0; double start = a1, end = a2;
        double diff = end - start;
        if (diff < 0) diff += 2 * Math.PI;
        if (diff > Math.PI) large = 1;
        double sx = v[0] + r * Math.cos(a1), sy = v[1] + r * Math.sin(a1);
        double ex = v[0] + r * Math.cos(a2), ey = v[1] + r * Math.sin(a2);
        svg.append(format("<path d=\"M %.4f %.4f A %.1f %.1f 0 %d 1 %.4f %.4f\" stroke=\"%s\" stroke-width=\"%.2f\" fill=\"none\"/>\n",
            sx, sy, r, r, large, ex, ey, color, SvgStyles.STROKE_WIDTH_NORMAL));
        String label = el.getLabel() != null ? el.getLabel() : "";
        if (!label.isEmpty()) {
            double mid = (a1 + a2) / 2;
            if (mid < 0) mid += Math.PI;
            double lx = v[0] + (r + fontSize / 2) * Math.cos(mid);
            double ly = v[1] + (r + fontSize / 2) * Math.sin(mid);
            svg.append(format("<text x=\"%.4f\" y=\"%.4f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"middle\">%s</text>\n",
                lx, ly, fontSize - 2, color, label));
        }
    }

    private void renderRightAngle(StringBuilder svg, GeometrySpec.Element el,
                                   Map<String, double[]> pointCoords, String color) {
        double[] v = getCoord(el.getVertex(), pointCoords);
        double[] l1 = getCoord(el.getLeg1(), pointCoords);
        double[] l2 = getCoord(el.getLeg2(), pointCoords);
        if (v == null || l1 == null || l2 == null) return;
        double s = SvgStyles.RIGHT_ANGLE_SIZE;
        double d1x = normalize(l1[0] - v[0], l1[1] - v[1])[0] * s;
        double d1y = normalize(l1[0] - v[0], l1[1] - v[1])[1] * s;
        double d2x = normalize(l2[0] - v[0], l2[1] - v[1])[0] * s;
        double d2y = normalize(l2[0] - v[0], l2[1] - v[1])[1] * s;
        double p1x = v[0] + d1x, p1y = v[1] + d1y;
        double p2x = v[0] + d1x + d2x, p2y = v[1] + d1y + d2y;
        double p3x = v[0] + d2x, p3y = v[1] + d2y;
        svg.append(format("<polyline points=\"%.4f,%.4f %.4f,%.4f %.4f,%.4f\" stroke=\"%s\" stroke-width=\"%.2f\" fill=\"none\"/>\n",
            p1x, p1y, p2x, p2y, p3x, p3y, color, SvgStyles.STROKE_WIDTH_NORMAL));
    }

    private void renderDimension(StringBuilder svg, GeometrySpec.Element el,
                                   Map<String, double[]> pointCoords, String color,
                                   Map<String, GeometrySpec.Element> segmentsById) {
        GeometrySpec.Element seg = findSegment(el.getSegment(), pointCoords, segmentsById);
        if (seg == null) return;
        double[] a = getCoord(seg.getFrom(), pointCoords);
        double[] b = getCoord(seg.getTo(), pointCoords);
        if (a == null || b == null) return;
        // 单位法向量（垂直于线段）
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        double nx = -dy / len, ny = dx / len;
        // 偏移距离
        double dist = 0.4;
        double[] off = el.getOffset() != null && el.getOffset().size() >= 2
            ? new double[]{el.getOffset().get(0), el.getOffset().get(1)} : null;
        if (off != null) { dist = Math.hypot(off[0], off[1]); }
        double ox = nx * dist, oy = ny * dist;
        double ax = a[0] + ox, ay = a[1] + oy;
        double bx = b[0] + ox, by = b[1] + oy;
        double mx = (ax + bx) / 2, my = (ay + by) / 2;
        String label = el.getLabel() != null ? el.getLabel() : (el.getText() != null ? el.getText() : "");
        // 两端小标记线 + 主标注线
        double tx = dx / len * 0.10, ty = dy / len * 0.10;
        svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\" class=\"dim\"/>\n",
            ax - tx - ox*0.1, ay - ty - oy*0.1, ax + tx - ox*0.1, ay + ty - oy*0.1, SvgStyles.DIM_COLOR, 0.04));
        svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\" class=\"dim\"/>\n",
            bx - tx - ox*0.1, by - ty - oy*0.1, bx + tx - ox*0.1, by + ty - oy*0.1, SvgStyles.DIM_COLOR, 0.04));
        svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\" class=\"dim\"/>\n",
            ax, ay, bx, by, SvgStyles.DIM_COLOR, 0.03));
        svg.append(format("<text x=\"%.4f\" y=\"%.4f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"middle\" class=\"dim\">%s</text>\n",
            mx + ox * 0.2, my + oy * 0.2, SvgStyles.DIM_FONT_SIZE, SvgStyles.DIM_COLOR, label));
    }

    private void renderTick(StringBuilder svg, GeometrySpec.Element el,
                              Map<String, double[]> pointCoords, String color,
                              Map<String, GeometrySpec.Element> segmentsById) {
        GeometrySpec.Element seg = findSegment(el.getSegment(), pointCoords, segmentsById);
        if (seg == null) return;
        double[] a = getCoord(seg.getFrom(), pointCoords);
        double[] b = getCoord(seg.getTo(), pointCoords);
        if (a == null || b == null) return;
        int count = el.getCount() != null ? el.getCount() : 1;
        double mx = (a[0] + b[0]) / 2, my = (a[1] + b[1]) / 2;
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        double nx = -dy / len * SvgStyles.TICK_SIZE, ny = dx / len * SvgStyles.TICK_SIZE;
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0) * 4;
            double ox = -dy / len * offset, oy = dx / len * offset;
            svg.append(format("<line x1=\"%.4f\" y1=\"%.4f\" x2=\"%.4f\" y2=\"%.4f\" stroke=\"%s\" stroke-width=\"%.2f\"/>\n",
                mx + ox - nx / 2, my + oy - ny / 2,
                mx + ox + nx / 2, my + oy + ny / 2,
                color, SvgStyles.STROKE_WIDTH_NORMAL));
        }
    }

    private void renderLabel(StringBuilder svg, GeometrySpec.Element el,
                              Map<String, double[]> pointCoords, String color, double fontSize) {
        double[] p = getCoord(el.getAt(), pointCoords);
        if (p == null) return;
        double ox = el.getOffset() != null && el.getOffset().size() >= 1 ? el.getOffset().get(0) : autoOffsetX(pointCoords, el.getAt(), p);
        double oy = el.getOffset() != null && el.getOffset().size() >= 2 ? el.getOffset().get(1) : autoOffsetY(pointCoords, el.getAt(), p);
        boolean latex = el.getLatex() != null && el.getLatex();
        String text = el.getText() != null ? el.getText() : "";
        svg.append(format("<text x=\"%.4f\" y=\"%.4f\" font-size=\"%.2f\" fill=\"%s\" text-anchor=\"middle\">%s</text>\n",
            p[0] + ox, p[1] + oy, fontSize, color, text));
    }

    private double autoOffsetX(Map<String, double[]> pointCoords, String id, double[] p) {
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> e : pointCoords.entrySet()) {
            if (e.getKey().equals(id)) continue;
            double d = Math.abs(e.getValue()[0] - p[0]);
            if (d < minDist) minDist = d;
        }
        // 偏移量与最近邻点距离成比例，避免标签飞出 viewBox
        double scale = clamp(minDist * 0.15, 0.3, 1.5);
        return minDist < 3 ? -scale : scale;
    }

    private double autoOffsetY(Map<String, double[]> pointCoords, String id, double[] p) {
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> e : pointCoords.entrySet()) {
            if (e.getKey().equals(id)) continue;
            double d = Math.abs(e.getValue()[1] - p[1]);
            if (d < minDist) minDist = d;
        }
        double scale = clamp(minDist * 0.15, 0.3, 1.2);
        return minDist < 3 ? -scale : scale;
    }

    private double[] extendLine(double[] a, double[] b, double[] bbox) {
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return new double[]{a[0], a[1], b[0], b[1]};
        double maxDim = Math.max(bbox[2] - bbox[0], bbox[3] - bbox[1]) * 2;
        return new double[]{
            a[0] - dx / len * maxDim, a[1] - dy / len * maxDim,
            b[0] + dx / len * maxDim, b[1] + dy / len * maxDim
        };
    }

    private double[] normalize(double x, double y) {
        double len = Math.sqrt(x * x + y * y);
        return len == 0 ? new double[]{0, 0} : new double[]{x / len, y / len};
    }

    private double[] getCoord(String id, Map<String, double[]> pointCoords) {
        if (id != null && pointCoords.containsKey(id)) return pointCoords.get(id);
        return null;
    }

    private GeometrySpec.Element findSegment(String id, Map<String, double[]> pointCoords,
                                              Map<String, GeometrySpec.Element> segmentsById) {
        if (id == null) return null;
        GeometrySpec.Element seg = segmentsById.get(id);
        if (seg != null) return seg;
        GeometrySpec.Element fake = new GeometrySpec.Element();
        if (id.length() >= 2) {
            fake.setFrom(id.substring(0, 1));
            fake.setTo(id.substring(1));
            if (pointCoords.containsKey(fake.getFrom()) && pointCoords.containsKey(fake.getTo())) return fake;
        }
        return null;
    }

    private double[] computeBBox(List<GeometrySpec.Element> elements, Map<String, double[]> pointCoords) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (GeometrySpec.Element el : elements) {
            if ("point".equals(el.getType()) && el.getX() != null && el.getY() != null) {
                minX = Math.min(minX, el.getX()); maxX = Math.max(maxX, el.getX());
                minY = Math.min(minY, el.getY()); maxY = Math.max(maxY, el.getY());
            }
            if ("segment".equals(el.getType())) {
                double[] f = getCoord(el.getFrom(), pointCoords);
                double[] t = getCoord(el.getTo(), pointCoords);
                if (f != null) { minX = Math.min(minX, f[0]); maxX = Math.max(maxX, f[0]); minY = Math.min(minY, f[1]); maxY = Math.max(maxY, f[1]); }
                if (t != null) { minX = Math.min(minX, t[0]); maxX = Math.max(maxX, t[0]); minY = Math.min(minY, t[1]); maxY = Math.max(maxY, t[1]); }
            }
            if ("circle".equals(el.getType())) {
                double[] c = getCoord(el.getCenter(), pointCoords);
                double r = el.getRadius() != null ? el.getRadius() : 0;
                if (c != null) { minX = Math.min(minX, c[0] - r); maxX = Math.max(maxX, c[0] + r); minY = Math.min(minY, c[1] - r); maxY = Math.max(maxY, c[1] + r); }
            }
            if ("grid".equals(el.getType()) || "axis".equals(el.getType())) {
                List<Double> xr = el.getXRange(), yr = el.getYRange();
                if (xr != null && xr.size() >= 2) { minX = Math.min(minX, xr.get(0)); maxX = Math.max(maxX, xr.get(1)); }
                if (yr != null && yr.size() >= 2) { double fy0 = -yr.get(0), fy1 = -yr.get(1);
                    minY = Math.min(minY, Math.min(fy0, fy1)); maxY = Math.max(maxY, Math.max(fy0, fy1)); }
            }
            if ("function-plot".equals(el.getType())) {
                List<Double> xr = el.getXRange();
                if (xr != null && xr.size() >= 2) { minX = Math.min(minX, xr.get(0)); maxX = Math.max(maxX, xr.get(1)); }
            }
        }
        if (minX == Double.MAX_VALUE) { minX = -1; maxX = 1; minY = -1; maxY = 1; }
        if (maxX - minX < 1) { maxX += 1; minX -= 1; }
        if (maxY - minY < 1) { maxY += 1; minY -= 1; }
        return new double[]{minX, minY, maxX, maxY};
    }

    private String sanitize(String svg) {
        svg = svg.replaceAll("(?i)<\\s*script[^>]*>.*?</\\s*script\\s*>", "");
        svg = svg.replaceAll("(?i)\\s+on\\w+\\s*=\\s*\"[^\"]*\"", "");
        svg = svg.replaceAll("(?i)\\s+on\\w+\\s*=\\s*'[^']*'", "");
        svg = svg.replaceAll("(?i)javascript\\s*:", "");
        return svg;
    }

    private String format(String fmt, Object... args) {
        return String.format(Locale.US, fmt, args);
    }

    private String fmtNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((int) v);
        return String.format(Locale.US, "%.1f", v);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    static double dist(double[] a, double[] b) {
        return ConstraintSolver.dist(a, b);
    }
}

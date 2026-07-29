package com.school.teaching.geometry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GeometrySpecParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public GeometrySpec parse(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) throw new GeometryException("diagram 为空");
        GeometrySpec spec = new GeometrySpec();
        List<Map<String, Object>> rawElements = castList(raw.get("elements"));
        if (rawElements == null || rawElements.isEmpty()) throw new GeometryException("elements 为空");
        List<GeometrySpec.Element> elements = new ArrayList<>();
        for (Map<String, Object> rawEl : rawElements) {
            elements.add(parseElement(rawEl));
        }
        spec.setElements(elements);
        // 校验引用完整性
        validateReferences(elements);
        // 校验约束可行性
        validateConstraints(elements);
        spec.setViewBox(parseViewBox(raw.get("viewBox")));
        spec.setStyle(parseStyle(raw.get("style")));
        return spec;
    }

    private GeometrySpec.Element parseElement(Map<String, Object> raw) {
        GeometrySpec.Element el = new GeometrySpec.Element();
        el.setType(asString(raw.get("type")));
        if (el.getType() == null || el.getType().isEmpty()) throw new GeometryException("element 缺少 type");
        el.setId(asString(raw.get("id")));
        el.setX(toDouble(raw.get("x")));
        el.setY(toDouble(raw.get("y")));
        el.setFrom(asString(raw.get("from")));
        el.setTo(asString(raw.get("to")));
        el.setStyle(asString(raw.get("style")));
        el.setFill(asString(raw.get("fill")));
        el.setCenter(asString(raw.get("center")));
        el.setRadius(toDouble(raw.get("radius")));
        el.setAt(asString(raw.get("at")));
        el.setText(asString(raw.get("text")));
        el.setLatex(toBoolean(raw.get("latex")));
        el.setVertex(asString(raw.get("vertex")));
        el.setLeg1(asString(raw.get("leg1")));
        el.setLeg2(asString(raw.get("leg2")));
        el.setSegment(asString(raw.get("segment")));
        el.setExpr(asString(raw.get("expr")));
        el.setColor(asString(raw.get("color")));
        el.setXLabel(asString(raw.get("xLabel")));
        el.setYLabel(asString(raw.get("yLabel")));
        el.setLabel(asString(raw.get("label")));
        if (raw.get("constraints") instanceof List<?> cl) {
            List<Map<String, Object>> constraints = new ArrayList<>();
            for (Object c : cl) {
                if (c instanceof Map<?, ?> m) {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        cm.put(asString(e.getKey()), e.getValue());
                    }
                    constraints.add(cm);
                }
            }
            el.setConstraints(constraints);
        }
        if (raw.get("through") instanceof List<?> t) {
            el.setThrough(t.stream().map(this::asString).filter(Objects::nonNull).toList());
        }
        if (raw.get("vertices") instanceof List<?> v) {
            el.setVertices(v.stream().map(this::asString).filter(Objects::nonNull).toList());
        }
        if (raw.get("diameter") instanceof List<?> d) {
            el.setDiameter(d.stream().map(this::asString).filter(Objects::nonNull).toList());
        }
        if (raw.get("offset") instanceof List<?> o) {
            List<Double> offset = new ArrayList<>();
            for (Object val : o) {
                if (val instanceof Number n) offset.add(n.doubleValue());
                else offset.add(0.0);
            }
            el.setOffset(offset);
        }
        if (raw.get("xRange") instanceof List<?> xr) {
            el.setXRange(xr.stream().filter(Number.class::isInstance).map(n -> ((Number) n).doubleValue()).toList());
        }
        if (raw.get("yRange") instanceof List<?> yr) {
            el.setYRange(yr.stream().filter(Number.class::isInstance).map(n -> ((Number) n).doubleValue()).toList());
        }
        el.setStep(toDouble(raw.get("step")));
        el.setTickStep(toDouble(raw.get("tickStep")));
        el.setSamples(raw.get("samples") instanceof Number n ? n.intValue() : null);
        el.setCount(raw.get("count") instanceof Number n ? n.intValue() : null);
        // 校验必填字段
        validateRequired(el);
        return el;
    }

    private void validateRequired(GeometrySpec.Element el) {
        switch (el.getType()) {
            case "point" -> {
                if (el.getId() == null) throw new GeometryException("point 缺少 id");
                if (el.getConstraints() == null && (el.getX() == null || el.getY() == null))
                    throw new GeometryException("point " + el.getId() + " 缺少坐标或 constraints");
            }
            case "segment", "line", "ray" -> {
                if (el.getType().equals("segment") && el.getFrom() == null && el.getTo() == null
                    && el.getThrough() == null)
                    throw new GeometryException("segment 缺少 from/to 或 through");
                if (el.getType().equals("line") && el.getThrough() == null && (el.getFrom() == null || el.getTo() == null))
                    throw new GeometryException("line 缺少 through 或 from/to");
            }
            case "circle" -> {
                if (el.getCenter() == null) throw new GeometryException("circle 缺少 center");
                if (el.getRadius() == null && el.getDiameter() == null)
                    throw new GeometryException("circle 缺少 radius 或 diameter");
            }
            case "label" -> {
                if (el.getAt() == null) throw new GeometryException("label 缺少 at");
                if (el.getText() == null) throw new GeometryException("label 缺少 text");
            }
            case "angle-arc" -> {
                if (el.getVertex() == null) throw new GeometryException("angle-arc 缺少 vertex");
                if (el.getFrom() == null) throw new GeometryException("angle-arc 缺少 from");
                if (el.getTo() == null) throw new GeometryException("angle-arc 缺少 to");
            }
            case "right-angle" -> {
                if (el.getVertex() == null) throw new GeometryException("right-angle 缺少 vertex");
                if (el.getLeg1() == null || el.getLeg2() == null)
                    throw new GeometryException("right-angle 缺少 leg1/leg2");
            }
            case "dimension" -> {
                if (el.getSegment() == null) throw new GeometryException("dimension 缺少 segment");
                if (el.getLabel() == null && el.getText() == null)
                    throw new GeometryException("dimension 缺少 label");
            }
            case "tick" -> {
                if (el.getSegment() == null) throw new GeometryException("tick 缺少 segment");
            }
            case "function-plot" -> {
                if (el.getExpr() == null) throw new GeometryException("function-plot 缺少 expr");
            }
            case "polygon" -> {
                if (el.getVertices() == null || el.getVertices().size() < 3)
                    throw new GeometryException("polygon 至少需要 3 个顶点");
            }
        }
    }

    private void validateReferences(List<GeometrySpec.Element> elements) {
        Set<String> definedIds = new HashSet<>();
        for (GeometrySpec.Element el : elements) {
            if (el.getId() != null) definedIds.add(el.getId());
            if ("point".equals(el.getType()) && el.getId() != null) definedIds.add(el.getId());
        }
        for (GeometrySpec.Element el : elements) {
            List<String> refs = new ArrayList<>();
            switch (el.getType()) {
                case "segment", "ray" -> { if (el.getFrom() != null) refs.add(el.getFrom()); if (el.getTo() != null) refs.add(el.getTo()); }
                case "line" -> { if (el.getThrough() != null) refs.addAll(el.getThrough()); }
                case "polygon" -> { if (el.getVertices() != null) refs.addAll(el.getVertices()); }
                case "circle" -> { if (el.getCenter() != null) refs.add(el.getCenter()); if (el.getDiameter() != null) refs.addAll(el.getDiameter()); }
                case "label" -> { if (el.getAt() != null) refs.add(el.getAt()); }
                case "angle-arc" -> { if (el.getVertex() != null) refs.add(el.getVertex()); if (el.getFrom() != null) refs.add(el.getFrom()); if (el.getTo() != null) refs.add(el.getTo()); }
                case "right-angle" -> { if (el.getVertex() != null) refs.add(el.getVertex()); if (el.getLeg1() != null) refs.add(el.getLeg1()); if (el.getLeg2() != null) refs.add(el.getLeg2()); }
                case "dimension" -> { if (el.getSegment() != null) refs.add(el.getSegment()); }
                case "tick" -> { if (el.getSegment() != null) refs.add(el.getSegment()); }
            }
            for (String ref : refs) {
                if (!definedIds.contains(ref)) {
                    // 允许 shorthand 形式: "AB" = 从 A 到 B 的线段，只要 A 和 B 都已定义
                    if (ref.length() >= 2) {
                        String a = ref.substring(0, 1);
                        String b = ref.substring(1);
                        if (definedIds.contains(a) && definedIds.contains(b)) continue;
                    }
                    throw new GeometryException("元素 " + el.getType() + " 引用了未定义的点/线段: " + ref);
                }
            }
        }
    }

    private void validateConstraints(List<GeometrySpec.Element> elements) {
        for (GeometrySpec.Element el : elements) {
            if (!"point".equals(el.getType()) || el.getConstraints() == null) continue;
            for (Map<String, Object> c : el.getConstraints()) {
                if (c.containsKey("distance")) {
                    List<?> params = c.get("distance") instanceof List<?> l ? l : null;
                    if (params != null && params.size() == 2 && params.get(1) instanceof Number d && d.doubleValue() <= 0)
                        throw new GeometryException("距离必须为正数，但得到: " + d);
                }
            }
        }
    }

    public GeometrySpec parse(String json) {
        try {
            Map<String, Object> raw = MAPPER.readValue(json, new TypeReference<>() {});
            return parse(raw);
        } catch (Exception e) {
            throw new GeometryException("diagram JSON 解析失败: " + e.getMessage());
        }
    }

    private GeometrySpec.ViewBox parseViewBox(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) return null;
        GeometrySpec.ViewBox vb = new GeometrySpec.ViewBox();
        vb.setX(toDouble(m.get("x")));
        vb.setY(toDouble(m.get("y")));
        vb.setWidth(toDouble(m.get("width")));
        vb.setHeight(toDouble(m.get("height")));
        return vb;
    }

    private GeometrySpec.Style parseStyle(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) return new GeometrySpec.Style();
        GeometrySpec.Style s = new GeometrySpec.Style();
        if (m.get("strokeColor") instanceof String sc) s.setStrokeColor(sc);
        if (m.get("labelFontSize") instanceof Number n) s.setLabelFontSize(n.doubleValue());
        if (m.get("gridColor") instanceof String gc) s.setGridColor(gc);
        return s;
    }

    private String asString(Object o) { return o instanceof String s ? s : o instanceof Number ? o.toString() : null; }
    private Double toDouble(Object o) { return o instanceof Number n ? n.doubleValue() : null; }
    private Boolean toBoolean(Object o) { return o instanceof Boolean b ? b : null; }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object o) {
        if (o instanceof List<?> l) {
            for (Object item : l) { if (!(item instanceof Map)) return null; }
            return (List<Map<String, Object>>) l;
        }
        return null;
    }
}

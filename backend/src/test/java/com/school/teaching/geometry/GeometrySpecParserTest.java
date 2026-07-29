package com.school.teaching.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GeometrySpecParserTest {

    private final GeometrySpecParser parser = new GeometrySpecParser();

    @Test
    @DisplayName("解析有效的三角形 spec")
    void parseTriangle() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "id", "A", "x", 0, "y", 0),
            Map.of("type", "point", "id", "B", "x", 6, "y", 0),
            Map.of("type", "segment", "from", "A", "to", "B"),
            Map.of("type", "label", "at", "A", "text", "A", "offset", List.of(-8, 12))
        ));
        GeometrySpec spec = parser.parse(raw);
        assertNotNull(spec);
        assertEquals(4, spec.getElements().size());
    }

    @Test
    @DisplayName("空 elements 应抛异常")
    void emptyElements() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of());
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("null diagram 应抛异常")
    void nullDiagram() {
        assertThrows(GeometryException.class, () -> parser.parse((Map<String, Object>) null));
    }

    @Test
    @DisplayName("point 缺少 id 应抛异常")
    void pointMissingId() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "x", 1, "y", 1)
        ));
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("point 缺少坐标也缺少 constraints 应抛异常")
    void pointMissingCoords() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "id", "A")
        ));
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("circle 缺少 center 应抛异常")
    void circleMissingCenter() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "id", "O", "x", 0, "y", 0),
            Map.of("type", "circle", "radius", 3)
        ));
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("引用不存在的点应抛异常")
    void referenceNotFound() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "id", "A", "x", 0, "y", 0),
            Map.of("type", "segment", "from", "A", "to", "X")
        ));
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("约束距离为负数应抛异常")
    void negativeDistance() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "id", "A", "x", 0, "y", 0),
            Map.of("type", "point", "id", "B", "constraints", List.of(
                Map.of("distance", List.of("A", -5))
            ))
        ));
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("polygon 顶点不足3个应抛异常")
    void polygonNotEnoughVertices() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("elements", List.of(
            Map.of("type", "point", "id", "A", "x", 0, "y", 0),
            Map.of("type", "point", "id", "B", "x", 1, "y", 0),
            Map.of("type", "polygon", "vertices", List.of("A", "B"))
        ));
        assertThrows(GeometryException.class, () -> parser.parse(raw));
    }

    @Test
    @DisplayName("parse JSON 字符串")
    void parseJsonString() {
        String json = "{\"elements\":[{\"type\":\"point\",\"id\":\"A\",\"x\":0,\"y\":0}]}";
        GeometrySpec spec = parser.parse(json);
        assertNotNull(spec);
        assertEquals(1, spec.getElements().size());
    }

    @Test
    @DisplayName("不合法 JSON 字符串应抛异常")
    void parseInvalidJsonString() {
        assertThrows(GeometryException.class, () -> parser.parse("{invalid"));
    }
}

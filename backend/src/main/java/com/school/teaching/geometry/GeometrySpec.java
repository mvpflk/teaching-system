package com.school.teaching.geometry;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GeometrySpec {
    private List<Element> elements;
    private ViewBox viewBox;
    private Style style;

    @Data
    public static class Element {
        private String type;
        private String id;
        private Double x, y;
        private List<Map<String, Object>> constraints;
        private String from, to;
        private List<String> through;
        private String style;
        private List<String> vertices;
        private String fill;
        private String center;
        private Double radius;
        private List<String> diameter;
        private String at;
        private String text;
        private List<Double> offset;
        private Boolean latex;
        private String vertex;
        private String leg1, leg2;
        private String segment;
        private Integer count;
        private List<Double> xRange, yRange;
        private Double step;
        private String xLabel, yLabel;
        private Double tickStep;
        private String expr;
        private String color;
        private Integer samples;
        private String label;
    }

    @Data
    public static class ViewBox {
        private double x, y, width, height;
    }

    @Data
    public static class Style {
        private String strokeColor;
        private Double labelFontSize;
        private String gridColor;
    }
}

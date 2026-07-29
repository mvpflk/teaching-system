package com.school.teaching.geometry;

public final class SvgStyles {
    private SvgStyles() {}

    public static final String PRIMARY_COLOR = "#4361ee";
    public static final String ACCENT_COLOR = "#e91e63";
    public static final String FILL_COLOR = "#e8f0ff";
    public static final String GRID_COLOR = "#e8e8e8";
    public static final String DIM_COLOR = "#888888";
    public static final String FONT_FAMILY = "system-ui, -apple-system, sans-serif";

    // 几何坐标系（1 unit ≈ 1 数学单位如 cm），非像素坐标系
    // 典型图 5-10 units 宽，渲染到 ~600px → 1 unit ≈ 60-120px
    public static final double STROKE_WIDTH_NORMAL = 0.06;
    public static final double STROKE_WIDTH_BOLD = 0.10;
    public static final double POINT_RADIUS = 0.10;
    public static final double LABEL_FONT_SIZE = 0.35;
    public static final double DIM_FONT_SIZE = 0.28;
    public static final double TICK_SIZE = 0.4;
    public static final double ANGLE_ARC_RADIUS = 0.6;
    public static final double RIGHT_ANGLE_SIZE = 0.35;
    public static final double PADDING_RATIO = 0.15;

    // 网格/坐标轴
    public static final double GRID_STROKE_WIDTH = 0.02;
    public static final double AXIS_STROKE_WIDTH = 0.06;
    public static final double TICK_LENGTH = 0.15;
    public static final double TICK_FONT_SIZE = 0.25;
}

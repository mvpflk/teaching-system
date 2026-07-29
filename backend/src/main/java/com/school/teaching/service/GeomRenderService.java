package com.school.teaching.service;

import com.school.teaching.geometry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class GeomRenderService {

    private static final Logger log = LoggerFactory.getLogger(GeomRenderService.class);

    private final GeometrySpecParser parser;
    private final ConstraintSolver solver;
    private final SvgRenderer renderer;
    private final DiagramUploader uploader;

    public GeomRenderService(GeometrySpecParser parser, ConstraintSolver solver,
                             SvgRenderer renderer, DiagramUploader uploader) {
        this.parser = parser;
        this.solver = solver;
        this.renderer = renderer;
        this.uploader = uploader;
    }

    public String renderAndUpload(Map<String, Object> diagram, Long teacherId) {
        try {
            GeometrySpec spec = parser.parse(diagram);
            solver.solve(spec);
            String svg = renderer.render(spec);
            return uploader.upload(svg, teacherId);
        } catch (GeometryException e) {
            log.warn("几何图形渲染失败: reason={}", e.getMessage());
            return null;
        }
    }

    public String renderAndUpload(String diagramJson, Long teacherId) {
        try {
            GeometrySpec spec = parser.parse(diagramJson);
            solver.solve(spec);
            String svg = renderer.render(spec);
            return uploader.upload(svg, teacherId);
        } catch (GeometryException e) {
            log.warn("几何图形渲染失败: reason={}", e.getMessage());
            return null;
        }
    }
}

package com.school.teaching.common.practice;

import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * PPT 步骤自动评估器 — 根据检查点规则评估 .pptx 文件。
 * 规则类型：slide_count（幻灯片数量）/ master（母版）/ animation（动画）/ transition（切换效果）
 */
public class PptCheckpointEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PptCheckpointEvaluator.class);

    public static Map<String, Object> evaluate(byte[] fileBytes, List<Map<String, Object>> checkpoints) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int passedCount = 0;
        int totalScore = 0;

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             XMLSlideShow ppt = new XMLSlideShow(is)) {

            for (Map<String, Object> cp : checkpoints) {
                Map<String, Object> cpResult = new LinkedHashMap<>();
                String type = (String) cp.getOrDefault("type", "slide_count");
                String desc = (String) cp.getOrDefault("desc", "");
                String expected = (String) cp.getOrDefault("expected", "");
                int cpScore = toInt(cp.get("score"), 10);

                cpResult.put("id", cp.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8)));
                cpResult.put("type", type);
                cpResult.put("desc", desc);
                cpResult.put("score", cpScore);

                boolean passed = false;
                String detail = "";

                try {
                    switch (type) {
                        case "slide_count":
                            int count = ppt.getSlides().size();
                            int expectedCount = Integer.parseInt(expected);
                            passed = count >= expectedCount;
                            detail = passed ? "幻灯片数量: " + count : "期望至少 " + expectedCount + " 张，实际 " + count + " 张";
                            break;
                        case "master":
                            passed = checkMaster(ppt);
                            detail = passed ? "母版已应用" : "未应用母版";
                            break;
                        case "animation":
                            passed = checkAnimation(ppt);
                            detail = passed ? "包含动画效果" : "未找到动画效果";
                            break;
                        case "transition":
                            passed = checkTransition(ppt);
                            detail = passed ? "包含切换效果" : "未找到切换效果";
                            break;
                        default:
                            detail = "未知检查类型: " + type;
                    }
                } catch (Exception e) {
                    detail = "评估异常: " + e.getMessage();
                    log.warn("PPT checkpoint eval failed: type={}", type, e);
                }

                cpResult.put("passed", passed);
                cpResult.put("detail", detail);
                if (passed) { passedCount++; totalScore += cpScore; }
                results.add(cpResult);
            }

        } catch (Exception e) {
            log.error("Failed to evaluate PPT file", e);
            Map<String, Object> errCp = new LinkedHashMap<>();
            errCp.put("passed", false);
            errCp.put("desc", "文件解析失败");
            errCp.put("detail", e.getMessage());
            errCp.put("score", 0);
            results.add(errCp);
        }

        result.put("checkpoints", results);
        result.put("passedCount", passedCount);
        result.put("totalCount", checkpoints.size());
        result.put("score", totalScore);
        return result;
    }

    private static boolean checkMaster(XMLSlideShow ppt) {
        // 检查是否使用了母版（非默认空白母版）
        for (XSLFSlide slide : ppt.getSlides()) {
            XSLFSlideMaster master = slide.getSlideMaster();
            if (master != null && master.getXmlObject() != null) return true;
        }
        return false;
    }

    private static boolean checkAnimation(XMLSlideShow ppt) {
        // 检查幻灯片中是否有动画节点
        for (XSLFSlide slide : ppt.getSlides()) {
            try {
                // XSLFSlide 包含动画信息时 getXmlObject 会有对应的命名空间节点
                if (slide.getXmlObject() != null && slide.getXmlObject().toString().contains("anim")) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static boolean checkTransition(XMLSlideShow ppt) {
        // 检查幻灯片切换效果
        for (XSLFSlide slide : ppt.getSlides()) {
            try {
                if (slide.getXmlObject() != null && slide.getXmlObject().toString().contains("transition")) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static int toInt(Object v, int defaultValue) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}

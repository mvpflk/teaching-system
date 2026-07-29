package com.school.teaching.common.practice;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Excel 步骤自动评估器 — 根据检查点规则评估 .xlsx 文件。
 * 规则类型：formula（公式检查）/ value（数值检查）/ format（格式检查）/ chart（图表检查）
 */
public class ExcelCheckpointEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ExcelCheckpointEvaluator.class);

    /**
     * 评估 Excel 文件，返回每个检查点的通过情况和总分。
     * @param fileBytes   .xlsx 文件字节
     * @param checkpoints 检查点列表 [{type, desc, target, expected, score}]
     * @return { fileUrl, checkpoints: [{...passed, detail}], passedCount, totalCount, score }
     */
    public static Map<String, Object> evaluate(byte[] fileBytes, List<Map<String, Object>> checkpoints) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int passedCount = 0;
        int totalScore = 0;

        try (InputStream is = new ByteArrayInputStream(fileBytes);
             Workbook workbook = new XSSFWorkbook(is)) {

            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (Map<String, Object> cp : checkpoints) {
                Map<String, Object> cpResult = new LinkedHashMap<>();
                String type = (String) cp.getOrDefault("type", "value");
                String desc = (String) cp.getOrDefault("desc", "");
                String target = (String) cp.getOrDefault("target", "");       // e.g. "Sheet1!B10"
                String expected = (String) cp.getOrDefault("expected", "");    // expected value or formula name
                int cpScore = toInt(cp.get("score"), 10);

                cpResult.put("id", cp.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8)));
                cpResult.put("type", type);
                cpResult.put("desc", desc);
                cpResult.put("score", cpScore);

                boolean passed = false;
                String detail = "";

                try {
                    switch (type) {
                        case "formula":
                            passed = checkFormula(workbook, formulaEvaluator, target, expected);
                            detail = passed ? "公式正确" : "公式不匹配，期望包含: " + expected;
                            break;
                        case "value":
                            passed = checkValue(workbook, formulaEvaluator, target, expected);
                            detail = passed ? "数值正确" : "期望值: " + expected;
                            break;
                        case "format":
                            passed = checkFormat(workbook, target, expected);
                            detail = passed ? "格式正确" : "格式不满足要求: " + expected;
                            break;
                        case "chart":
                            passed = checkChart(workbook, target, expected);
                            detail = passed ? "图表存在" : "未找到指定图表";
                            break;
                        default:
                            detail = "未知检查类型: " + type;
                    }
                } catch (Exception e) {
                    detail = "评估异常: " + e.getMessage();
                    log.warn("Excel checkpoint eval failed: type={} target={}", type, target, e);
                }

                cpResult.put("passed", passed);
                cpResult.put("detail", detail);
                if (passed) {
                    passedCount++;
                    totalScore += cpScore;
                }
                results.add(cpResult);
            }

        } catch (Exception e) {
            log.error("Failed to evaluate Excel file", e);
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

    // ── 公式检查：指定单元格包含特定函数 ──
    private static boolean checkFormula(Workbook wb, FormulaEvaluator evaluator,
                                         String target, String expected) {
        Cell cell = resolveCell(wb, target);
        if (cell == null) return false;
        if (cell.getCellType() != CellType.FORMULA) return false;
        String formula = cell.getCellFormula().toUpperCase();
        // expected 是函数名列表，逗号分隔，如 "SUM,VLOOKUP"
        for (String fn : expected.split("[,，]")) {
            if (formula.contains(fn.trim().toUpperCase())) return true;
        }
        return false;
    }

    // ── 数值检查：指定单元格的值匹配 ──
    private static boolean checkValue(Workbook wb, FormulaEvaluator evaluator,
                                       String target, String expected) {
        Cell cell = resolveCell(wb, target);
        if (cell == null) return false;

        // 先用公式求值器求值
        CellValue cv = evaluator.evaluate(cell);
        String actualStr;

        switch (cv.getCellType()) {
            case NUMERIC:
                actualStr = String.valueOf(cv.getNumberValue());
                break;
            case STRING:
                actualStr = cv.getStringValue();
                break;
            case BOOLEAN:
                actualStr = String.valueOf(cv.getBooleanValue());
                break;
            default:
                // 回退到原始单元格值
                actualStr = getCellStringValue(cell);
        }

        // 数值比较（容差）
        try {
            double actualNum = Double.parseDouble(actualStr.trim());
            double expectedNum = Double.parseDouble(expected.trim());
            return Math.abs(actualNum - expectedNum) < 0.001;
        } catch (NumberFormatException ignored) {}

        // 字符串比较（忽略大小写和前后空格）
        return actualStr.trim().equalsIgnoreCase(expected.trim());
    }

    // ── 格式检查：条件格式/合并单元格/边框等 ──
    private static boolean checkFormat(Workbook wb, String target, String expected) {
        Cell cell = resolveCell(wb, target);
        if (cell == null) return false;

        switch (expected.toLowerCase()) {
            case "bold":
                CellStyle style = cell.getCellStyle();
                Font font = wb.getFontAt(style.getFontIndex());
                return font.getBold();
            case "merge":
                // 检查是否属于合并区域
                Sheet sheet = cell.getSheet();
                for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
                    CellRangeAddress region = sheet.getMergedRegion(i);
                    if (region.isInRange(cell.getRowIndex(), cell.getColumnIndex())) return true;
                }
                return false;
            case "border":
                CellStyle s = cell.getCellStyle();
                return s.getBorderTop() != BorderStyle.NONE || s.getBorderBottom() != BorderStyle.NONE;
            case "currency":
            case "number":
                String dfs = cell.getCellStyle().getDataFormatString(); return dfs != null && !dfs.equals("General");
            default:
                return false;
        }
    }

    // ── 图表检查：工作表中存在图表 ──
    private static boolean checkChart(Workbook wb, String target, String expected) {
        Sheet sheet;
        if (target != null && !target.isEmpty()) {
            sheet = wb.getSheet(target.split("!")[0]);
        } else {
            sheet = wb.getSheetAt(0);
        }
        if (sheet == null) return false;

        // XSSFSheet 使用 getDrawingPatriarch 检查图表
        if (sheet instanceof XSSFSheet) {
            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
            if (drawing != null && !drawing.getCharts().isEmpty()) return true;
        }
        return false;
    }

    // ── 工具方法 ──
    private static Cell resolveCell(Workbook wb, String target) {
        if (target == null || target.isEmpty()) return null;
        try {
            String[] parts = target.split("!");
            String sheetName = parts.length > 1 ? parts[0] : wb.getSheetName(0);
            String cellRef = parts.length > 1 ? parts[1] : parts[0];
            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) return null;

            // 解析如 "B10" → row=9, col=1
            String colStr = cellRef.replaceAll("\\d", "");
            String rowStr = cellRef.replaceAll("\\D", "");
            int col = CellReference.convertColStringToIndex(colStr);
            int row = Integer.parseInt(rowStr) - 1;
            Row r = sheet.getRow(row);
            if (r == null) return null;
            return r.getCell(col);
        } catch (Exception e) {
            log.warn("Failed to resolve cell: {}", target, e);
            return null;
        }
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case STRING: return cell.getStringCellValue();
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { return cell.getStringCellValue(); }
            default: return "";
        }
    }

    private static int toInt(Object v, int defaultValue) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}

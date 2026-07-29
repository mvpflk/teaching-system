package com.school.teaching.service.impl;

import com.school.teaching.exception.BusinessException;
import com.school.teaching.service.OfficeCompareService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class OfficeCompareServiceImpl implements OfficeCompareService {

    @Override
    public Map<String, Object> compare(String templatePath, String studentFilePath, String fileType) {
        if ("xlsx".equalsIgnoreCase(fileType) || "xls".equalsIgnoreCase(fileType)) {
            return compareExcel(templatePath, studentFilePath);
        } else if ("docx".equalsIgnoreCase(fileType) || "doc".equalsIgnoreCase(fileType)) {
            return compareWord(templatePath, studentFilePath);
        }
        throw new BusinessException(400, "不支持的文件类型: " + fileType);
    }

    private Map<String, Object> compareWord(String templatePath, String studentPath) {
        try {
            String templateText = extractWordText(templatePath);
            String studentText = extractWordText(studentPath);
            double similarity = cosineSimilarity(templateText, studentText);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("fileType", "docx");
            report.put("similarity", BigDecimal.valueOf(similarity).setScale(2, RoundingMode.HALF_UP));
            report.put("templateLength", templateText.length());
            report.put("studentLength", studentText.length());
            int score;
            if (similarity >= 0.9) score = 95; else if (similarity >= 0.7) score = 80;
            else if (similarity >= 0.5) score = 65; else if (similarity >= 0.3) score = 50;
            else score = 30;
            report.put("suggestedScore", score);
            return report;
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(500, "Word比对失败: " + e.getMessage()); }
    }

    private String extractWordText(String filePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> { if (p.getText() != null) sb.append(p.getText()).append(" "); });
            doc.getTables().forEach(table ->
                table.getRows().forEach(row ->
                    row.getTableCells().forEach(cell -> sb.append(cell.getText()).append(" "))
                )
            );
            return sb.toString().trim();
        }
    }

    private Map<String, Object> compareExcel(String templatePath, String studentPath) {
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("fileType", "xlsx");
            List<Map<String, Object>> cellDiffs = new ArrayList<>();
            List<Map<String, Object>> formulaDiffs = new ArrayList<>();
            int totalCells = 0, matchCells = 0;

            try (Workbook templateWb = new XSSFWorkbook(new FileInputStream(templatePath));
                 Workbook studentWb = new XSSFWorkbook(new FileInputStream(studentPath))) {

                Sheet templateSheet = templateWb.getSheetAt(0);
                Sheet studentSheet = studentWb.getSheetAt(0);
                int maxRows = Math.max(templateSheet.getLastRowNum(), studentSheet.getLastRowNum());

                for (int r = 0; r <= maxRows; r++) {
                    Row tRow = templateSheet.getRow(r);
                    Row sRow = studentSheet.getRow(r);
                    if (tRow == null && sRow == null) continue;
                    int maxCols = 0;
                    if (tRow != null) maxCols = Math.max(maxCols, tRow.getLastCellNum());
                    if (sRow != null) maxCols = Math.max(maxCols, sRow.getLastCellNum());

                    for (int c = 0; c < maxCols; c++) {
                        Cell tCell = tRow != null ? tRow.getCell(c) : null;
                        Cell sCell = sRow != null ? sRow.getCell(c) : null;
                        if (tCell == null && sCell == null) continue;
                        totalCells++;
                        String cellRef = cellRef(r, c);

                        if (tCell != null && tCell.getCellType() == CellType.FORMULA) {
                            String tFormula = tCell.getCellFormula();
                            if (sCell != null && sCell.getCellType() == CellType.FORMULA) {
                                String sFormula = sCell.getCellFormula();
                                if (!formulasEquivalent(tFormula, sFormula)) {
                                    formulaDiffs.add(Map.of("cell", cellRef, "expected", tFormula, "actual", sFormula));
                                } else { matchCells++; }
                            } else {
                                formulaDiffs.add(Map.of("cell", cellRef, "expected", tFormula, "actual", getCellDisplayValue(sCell)));
                            }
                        } else {
                            String tVal = getCellDisplayValue(tCell);
                            String sVal = getCellDisplayValue(sCell);
                            if (Objects.equals(tVal, sVal)) { matchCells++; }
                            else if (tVal != null && !tVal.isEmpty()) {
                                cellDiffs.add(Map.of("cell", cellRef, "expected", tVal, "actual", sVal != null ? sVal : "（空）"));
                            }
                        }
                    }
                }
            }

            double accuracy = totalCells > 0 ? (double) matchCells / totalCells : 0;
            report.put("totalCells", totalCells);
            report.put("matchCells", matchCells);
            report.put("accuracy", BigDecimal.valueOf(accuracy).setScale(2, RoundingMode.HALF_UP));
            report.put("cellDiffs", cellDiffs.size() > 20 ? cellDiffs.subList(0, 20) : cellDiffs);
            report.put("formulaDiffs", formulaDiffs);
            report.put("totalDiffs", cellDiffs.size() + formulaDiffs.size());
            int score;
            if (accuracy >= 0.95) score = 95; else if (accuracy >= 0.8) score = 80;
            else if (accuracy >= 0.6) score = 65; else if (accuracy >= 0.4) score = 50;
            else score = 30;
            report.put("suggestedScore", score);
            return report;
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw new BusinessException(500, "Excel比对失败: " + e.getMessage()); }
    }

    private String cellRef(int row, int col) {
        return "" + (char) ('A' + col) + (row + 1);
    }

    private String getCellDisplayValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getLocalDateTimeCellValue().toString();
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) && !Double.isInfinite(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            case BLANK -> "";
            default -> "";
        };
    }

    private boolean formulasEquivalent(String f1, String f2) {
        return f1.replace(" ", "").toUpperCase().equals(f2.replace(" ", "").toUpperCase());
    }

    private double cosineSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Map<String, Integer> freqA = new HashMap<>();
        Map<String, Integer> freqB = new HashMap<>();
        String[] wordsA = a.split("[\\s,，。.!！?？;；:：、]+");
        String[] wordsB = b.split("[\\s,，。.!！?？;；:：、]+");
        for (String w : wordsA) {
            if (w.isEmpty()) continue;
            for (int i = 0; i < w.length(); i++) {
                freqA.merge(String.valueOf(w.charAt(i)), 1, Integer::sum);
                if (i + 1 < w.length()) freqA.merge(w.substring(i, i + 2), 1, Integer::sum);
            }
        }
        for (String w : wordsB) {
            if (w.isEmpty()) continue;
            for (int i = 0; i < w.length(); i++) {
                freqB.merge(String.valueOf(w.charAt(i)), 1, Integer::sum);
                if (i + 1 < w.length()) freqB.merge(w.substring(i, i + 2), 1, Integer::sum);
            }
        }
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(freqA.keySet());
        allTerms.addAll(freqB.keySet());
        double dot = 0, normA = 0, normB = 0;
        for (String t : allTerms) {
            int ca = freqA.getOrDefault(t, 0), cb = freqB.getOrDefault(t, 0);
            dot += ca * cb; normA += ca * ca; normB += cb * cb;
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

package com.example.demo;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

public class Test {

    private static final String DOC_DIR = "src/main/java/com/example/demo/doc/";
    private static final String DEFAULT_SIGNATURE_PATH = DOC_DIR + "single.png";

    public static void main(String[] args) {
        String inputPath = DOC_DIR + "111111111光启激励与纪律处分管理制度-修订-2025.8-V4.docx";
        String outputPath = DOC_DIR + "KC-AD-DA-001物流管理规范0821_已签署.docx";
        Map<String, List<String>> fileMap = new HashMap<>();
        fileMap.put("编写", Arrays.asList(DEFAULT_SIGNATURE_PATH, DEFAULT_SIGNATURE_PATH));
        fileMap.put("审核", Arrays.asList(DEFAULT_SIGNATURE_PATH, DEFAULT_SIGNATURE_PATH));
        fileMap.put("批准", Arrays.asList(DEFAULT_SIGNATURE_PATH, DEFAULT_SIGNATURE_PATH));
        try {
            createFile(inputPath, outputPath, fileMap);
            System.out.println("处理成功！文件已保存为: " + outputPath);
        } catch (Exception e) {
            System.err.println("处理失败:");
            e.printStackTrace();
        }
    }

    public static boolean createFile(String inputPath, String outputPath, Map<String, List<String>> fileMap) {
        boolean flag = false;
        try (FileInputStream fis = new FileInputStream(inputPath);
             FileOutputStream fos = new FileOutputStream(outputPath)) {
            XWPFDocument document = new XWPFDocument(fis);
            String currentDateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            List<XWPFTable> tables = document.getTables();
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            for (int i = 0; i < Math.min(15, paragraphs.size()); i++) {
                XWPFParagraph paragraph = paragraphs.get(i);
                String text = paragraph.getText().replaceAll("\\s+", "");
                for (String regex : fileMap.keySet()) {
                    if (text.contains(regex) && (text.contains("日期") || text.endsWith("：") || text.endsWith(":"))) {
                        XWPFRun run = paragraph.getRuns().get(0);
                        CTRPr rPr = run.getCTR().getRPr();
                        int fontSizePt = 12;
                        if (rPr != null && rPr.getSz() != null) {
                            fontSizePt = rPr.getSz().getVal().intValue() / 2;
                        }
                        int imgWidthEMU = Units.toEMU(80);
                        int imgHeightEMU = Units.toEMU(40);

                        int index;
                        String afterRegex = text.substring(text.indexOf(regex) + regex.length(),
                                text.indexOf(regex) + regex.length() + 1);
                        if (afterRegex.equals("：") || afterRegex.equals(":")) {
                            index = text.indexOf(regex) + regex.length() + 1;
                        } else {
                            index = text.indexOf(regex) + regex.length();
                        }

                        String prefixText = text.substring(0, index);
                        boolean hasDateField = text.contains("日期");

                        List<String> signaturePaths = fileMap.get(regex);

// 控制段落行距
                        CTPPr pPr = paragraph.getCTP().isSetPPr()
                                ? paragraph.getCTP().getPPr()
                                : paragraph.getCTP().addNewPPr();
                        CTSpacing spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
                        spacing.setLine(java.math.BigInteger.valueOf(240));
                        spacing.setLineRule(STLineSpacingRule.AUTO);
                        paragraph.setSpacingBefore(0);
                        paragraph.setSpacingAfter(0);

                        // 把首行缩进合并到左缩进，确保换行后所有行缩进一致
                        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
                        int totalLeft = 0;
                        if (ind.isSetLeft()) totalLeft += ind.getLeft().intValue();
                        if (ind.isSetFirstLine()) totalLeft += ind.getFirstLine().intValue();
                        ind.setLeft(java.math.BigInteger.valueOf(totalLeft));
                        if (ind.isSetFirstLine()) ind.unsetFirstLine();
                        if (ind.isSetFirstLineChars()) ind.unsetFirstLineChars();
                        if (ind.isSetLeftChars()) ind.unsetLeftChars();

                        // 清掉段落里的修订标记（del/ins），避免残留内容影响布局
                        CTP ctp = paragraph.getCTP();
                        while (ctp.sizeOfDelArray() > 0) {
                            ctp.removeDel(0);
                        }
                        while (ctp.sizeOfInsArray() > 0) {
                            ctp.removeIns(0);
                        }
                        // 清掉 bookmarkStart/End
                        while (ctp.sizeOfBookmarkStartArray() > 0) {
                            ctp.removeBookmarkStart(0);
                        }
                        while (ctp.sizeOfBookmarkEndArray() > 0) {
                            ctp.removeBookmarkEnd(0);
                        }

                        // 清掉多余的 run，只保留第一个
                        while (ctp.sizeOfRArray() > 1) {
                            ctp.removeR(ctp.sizeOfRArray() - 1);
                        }
                        run = paragraph.getRuns().get(0);

                        // 保存原始 run 的字体信息
                        String fontFamily = run.getFontFamily();

                        // 第一行 run1: 前缀文字
                        run.setText("", 0);
                        run.setText(prefixText);

                        // 第一行 run2: 签名图片（单独 run）
                        XWPFRun imgRun1 = paragraph.createRun();
                        imgRun1.setFontSize(fontSizePt);
                        String firstFilePath = signaturePaths.get(0);
                        ByteArrayInputStream firstBais = new ByteArrayInputStream(
                                Files.readAllBytes(Paths.get(firstFilePath)));
                        imgRun1.addPicture(firstBais, Document.PICTURE_TYPE_PNG, "image.png",
                                imgWidthEMU, imgHeightEMU);
                        flag = true;

                        // 如果有日期字段，在签名后追加日期
                        if (hasDateField) {
                            XWPFRun dateRun = paragraph.createRun();
                            dateRun.setFontSize(fontSizePt);
                            if (fontFamily != null) dateRun.setFontFamily(fontFamily);
                            String remainText = text.substring(index);
                            dateRun.setText(remainText + currentDateStr);
                        }

                        // 后续签名：每个用 换行run + 占位run + 图片run
                        if (signaturePaths.size() > 1) {
                            // 直接用原始前缀文字做占位，但设成白色不可见
                            // 这样字体、字宽完全一致，不存在对不齐的问题
                            for (int s = 1; s < signaturePaths.size(); s++) {
                                // 换行 run
                                XWPFRun brRun = paragraph.createRun();
                                brRun.addBreak();

                                // 占位 run：用原文前缀，白色隐藏
                                XWPFRun spaceRun = paragraph.createRun();
                                spaceRun.setFontSize(fontSizePt);
                                if (fontFamily != null) spaceRun.setFontFamily(fontFamily);
                                // 复制原始 run 的东亚字体设置
                                CTRPr spaceRPr = spaceRun.getCTR().isSetRPr()
                                        ? spaceRun.getCTR().getRPr()
                                        : spaceRun.getCTR().addNewRPr();
                                if (rPr != null && rPr.isSetRFonts()) {
                                    CTFonts srcFonts = rPr.getRFonts();
                                    CTFonts dstFonts = spaceRPr.isSetRFonts()
                                            ? spaceRPr.getRFonts()
                                            : spaceRPr.addNewRFonts();
                                    if (srcFonts.isSetEastAsia()) dstFonts.setEastAsia(srcFonts.getEastAsia());
                                }
                                spaceRun.setText(prefixText);
                                spaceRun.setColor("FFFFFF");

                                // 签名图片 run
                                XWPFRun imgRun = paragraph.createRun();
                                imgRun.setFontSize(fontSizePt);
                                String filePath = signaturePaths.get(s);
                                ByteArrayInputStream bais = new ByteArrayInputStream(
                                        Files.readAllBytes(Paths.get(filePath)));
                                imgRun.addPicture(bais, Document.PICTURE_TYPE_PNG, "image.png",
                                        imgWidthEMU, imgHeightEMU);
                            }
                        }
                    }
                }
            }

// ========== 处理表格 ==========
            String tableDateStr = new SimpleDateFormat("yyyy年MM月dd日").format(new Date());
            for (int t = 0; t < Math.min(5, tables.size()); t++) {
                XWPFTable table = tables.get(t);
                boolean tableHasMatch = false;

                List<XWPFTableRow> rows = table.getRows();
                int rowIndex = 0;
                while (rowIndex < rows.size()) {
                    XWPFTableRow row = rows.get(rowIndex);
                    List<XWPFTableCell> cells = row.getTableCells();
                    if (cells.size() >= 3) {
                        String firstCellText = cells.get(0).getText().trim();
                        boolean matched = false;
                        for (String regex : fileMap.keySet()) {
                            if (firstCellText.replaceAll("\\s+", "").contains(regex)) {
                                matched = true;
                                if (!tableHasMatch) {
                                    tableHasMatch = true;
                                    setTableBordersToSolid(table);
                                }
                                List<String> signatureList = fileMap.get(regex);
                                int signatureCount = signatureList.size();
                                Integer fontSize = getFontSize(cells.get(0).getParagraphs());
                                System.out.println("找到目标行: " + firstCellText + ", 签名数量: " + signatureCount);

                                if (signatureCount > 1) {
                                    int tableColCount = table.getRow(0).getTableCells().size();
                                    for (int k = 0; k < signatureCount - 1; k++) {
                                        XWPFTableRow newRow = table.insertNewTableRow(rowIndex + 1);
                                        for (int c = 0; c < tableColCount; c++) {
                                            XWPFTableCell newCell = newRow.createCell();
                                            removeCellMargin(newCell);
                                            setCellBordersToSolid(newCell);
                                        }
                                    }
                                    rows = table.getRows();
                                    mergeCellsVertically(table, 0, rowIndex, rowIndex + signatureCount - 1);

                                    XWPFTableCell firstRowRoleCell = rows.get(rowIndex).getCell(0);
                                    if (firstRowRoleCell != null) {
                                        setCellCenter(firstRowRoleCell, fontSize);
                                    }

                                    for (int s = 0; s < signatureCount; s++) {
                                        XWPFTableRow sigRow = rows.get(rowIndex + s);

                                        // 设置行高为固定值，避免原始行被撑大
                                        CTTrPr trPr = sigRow.getCtRow().isSetTrPr()
                                                ? sigRow.getCtRow().getTrPr()
                                                : sigRow.getCtRow().addNewTrPr();
                                        CTHeight ht = trPr.sizeOfTrHeightArray() > 0
                                                ? trPr.getTrHeightArray(0)
                                                : trPr.addNewTrHeight();
                                        ht.setVal(java.math.BigInteger.valueOf(560));
                                        ht.setHRule(STHeightRule.AT_LEAST);

                                        XWPFTableCell imgCell = sigRow.getCell(1);
                                        if (imgCell != null) {
                                            removeCellMargin(imgCell);
                                            imgCell.removeParagraph(0);
                                            XWPFParagraph imgPara = imgCell.addParagraph();
                                            imgPara.setAlignment(ParagraphAlignment.CENTER);
                                            imgPara.setSpacingBefore(0);
                                            imgPara.setSpacingAfter(0);
                                            XWPFRun imgRun = imgPara.createRun();
                                            String filePath = signatureList.get(s);
                                            imgRun.addPicture(
                                                    new ByteArrayInputStream(Files.readAllBytes(Paths.get(filePath))),
                                                    Document.PICTURE_TYPE_PNG, "signature",
                                                    80 * 12700, 40 * 12700);
                                            flag = true;
                                            setCellVerticalCenter(imgCell);
                                        }

                                        XWPFTableCell dateCell = sigRow.getCell(2);
                                        if (dateCell != null) {
                                            removeCellMargin(dateCell);
                                            dateCell.removeParagraph(0);
                                            XWPFParagraph datePara = dateCell.addParagraph();
                                            datePara.setAlignment(ParagraphAlignment.CENTER);
                                            datePara.setSpacingBefore(0);
                                            datePara.setSpacingAfter(0);
                                            XWPFRun dateRun = datePara.createRun();
                                            dateRun.setText(tableDateStr);
                                            int dateFontSize = (fontSize != null && fontSize > 10) ? fontSize - 2 : (fontSize != null ? fontSize : 10);
                                            dateRun.setFontSize(dateFontSize);
                                            dateRun.setFontFamily("宋体");
                                            setCellVerticalCenter(dateCell);
                                        }
                                    }
                                    rowIndex += signatureCount;
                                } else {
                                    XWPFTableCell roleCell = cells.get(0);
                                    if (roleCell != null) {
                                        setCellCenter(roleCell, fontSize);
                                    }

                                    XWPFTableCell imgCell = cells.get(1);
                                    removeCellMargin(imgCell);
                                    imgCell.removeParagraph(0);
                                    XWPFParagraph imgPara = imgCell.addParagraph();
                                    imgPara.setAlignment(ParagraphAlignment.CENTER);
                                    imgPara.setSpacingBefore(0);
                                    imgPara.setSpacingAfter(0);
                                    XWPFRun imgRun = imgPara.createRun();
                                    imgRun.addPicture(
                                            new ByteArrayInputStream(Files.readAllBytes(Paths.get(signatureList.get(0)))),
                                            Document.PICTURE_TYPE_PNG, "signature",
                                            80 * 12700, 40 * 12700);
                                    flag = true;
                                    setCellVerticalCenter(imgCell);

                                    XWPFTableCell dateCell = cells.get(2);
                                    removeCellMargin(dateCell);
                                    List<XWPFParagraph> dateCellParagraphs = dateCell.getParagraphs();
                                    XWPFParagraph datePara;
                                    if (dateCellParagraphs != null && !dateCellParagraphs.isEmpty()) {
                                        datePara = dateCellParagraphs.get(0);
                                        datePara.removeRun(0);
                                    } else {
                                        datePara = dateCell.addParagraph();
                                    }
                                    datePara.setAlignment(ParagraphAlignment.CENTER);
                                    datePara.setSpacingBefore(0);
                                    datePara.setSpacingAfter(0);
                                    XWPFRun dateRun = datePara.createRun();
                                    dateRun.setText(tableDateStr);
                                    dateRun.setFontSize(fontSize);
                                    dateRun.setFontFamily("宋体");
                                    setCellVerticalCenter(dateCell);

                                    rowIndex++;
                                }
                                break;
                            }
                        }
                        if (!matched) {
                            rowIndex++;
                        }
                    } else {
                        rowIndex++;
                    }
                }
            }

            document.write(fos);
            document.close();
            System.out.println("Word 文档处理完成，已保存至: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    private static Integer getFontSize(List<XWPFParagraph> paragraphs) {
        Integer fontSize = null;
        for (XWPFParagraph para : paragraphs) {
            List<XWPFRun> runs = para.getRuns();
            for (XWPFRun run : runs) {
                String text = run.getText(0);
                if (text != null && !text.trim().isEmpty()) {
                    int size = run.getFontSize();
                    if (size != -1) {
                        fontSize = size;
                        break;
                    }
                }
            }
            if (fontSize != null) break;
        }
        return fontSize;
    }

    private static void mergeCellsVertically(XWPFTable table, int col, int fromRow, int toRow) {
        for (int rowIndex = fromRow; rowIndex <= toRow; rowIndex++) {
            XWPFTableRow row = table.getRow(rowIndex);
            if (row == null) continue;
            while (row.getTableCells().size() <= col) {
                XWPFTableCell newCell = row.createCell();
                setCellBordersToSolid(newCell);
            }
            XWPFTableCell cell = row.getCell(col);
            if (cell == null) continue;
            CTTcPr tcPr = cell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = cell.getCTTc().addNewTcPr();
            }
            if (tcPr.isSetVMerge()) {
                tcPr.unsetVMerge();
            }
            if (rowIndex == fromRow) {
                tcPr.addNewVMerge().setVal(STMerge.RESTART);
            } else {
                tcPr.addNewVMerge().setVal(STMerge.CONTINUE);
            }
        }
    }

    private static void removeCellMargin(XWPFTableCell cell) {
        for (XWPFParagraph para : cell.getParagraphs()) {
            para.setIndentationLeft(0);
            para.setIndentationRight(0);
            para.setSpacingBefore(0);
            para.setSpacingAfter(0);
        }
    }

    private static void setTableBordersToSolid(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();

        CTBorder top = borders.isSetTop() ? borders.getTop() : borders.addNewTop();
        top.setVal(STBorder.SINGLE);
        top.setSz(java.math.BigInteger.valueOf(4));
        top.setSpace(java.math.BigInteger.ZERO);
        top.setColor("000000");

        CTBorder bottom = borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(java.math.BigInteger.valueOf(4));
        bottom.setSpace(java.math.BigInteger.ZERO);
        bottom.setColor("000000");

        CTBorder left = borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft();
        left.setVal(STBorder.SINGLE);
        left.setSz(java.math.BigInteger.valueOf(4));
        left.setSpace(java.math.BigInteger.ZERO);
        left.setColor("000000");

        CTBorder right = borders.isSetRight() ? borders.getRight() : borders.addNewRight();
        right.setVal(STBorder.SINGLE);
        right.setSz(java.math.BigInteger.valueOf(4));
        right.setSpace(java.math.BigInteger.ZERO);
        right.setColor("000000");

        CTBorder insideH = borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH();
        insideH.setVal(STBorder.SINGLE);
        insideH.setSz(java.math.BigInteger.valueOf(4));
        insideH.setSpace(java.math.BigInteger.ZERO);
        insideH.setColor("000000");

        CTBorder insideV = borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV();
        insideV.setVal(STBorder.SINGLE);
        insideV.setSz(java.math.BigInteger.valueOf(4));
        insideV.setSpace(java.math.BigInteger.ZERO);
        insideV.setColor("000000");

        CTTblCellMar cellMar = tblPr.isSetTblCellMar() ? tblPr.getTblCellMar() : tblPr.addNewTblCellMar();
        cellMar.addNewTop().setW(java.math.BigInteger.ZERO);
        cellMar.addNewBottom().setW(java.math.BigInteger.ZERO);
        cellMar.addNewLeft().setW(java.math.BigInteger.ZERO);
        cellMar.addNewRight().setW(java.math.BigInteger.ZERO);

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                setCellBordersToSolid(cell);
            }
        }
    }

    private static void setCellBordersToSolid(XWPFTableCell cell) {
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        CTTcBorders cellBorders = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();

        CTBorder cellTop = cellBorders.isSetTop() ? cellBorders.getTop() : cellBorders.addNewTop();
        cellTop.setVal(STBorder.SINGLE);
        cellTop.setSz(java.math.BigInteger.valueOf(4));
        cellTop.setSpace(java.math.BigInteger.ZERO);
        cellTop.setColor("000000");

        CTBorder cellBottom = cellBorders.isSetBottom() ? cellBorders.getBottom() : cellBorders.addNewBottom();
        cellBottom.setVal(STBorder.SINGLE);
        cellBottom.setSz(java.math.BigInteger.valueOf(4));
        cellBottom.setSpace(java.math.BigInteger.ZERO);
        cellBottom.setColor("000000");

        CTBorder cellLeft = cellBorders.isSetLeft() ? cellBorders.getLeft() : cellBorders.addNewLeft();
        cellLeft.setVal(STBorder.SINGLE);
        cellLeft.setSz(java.math.BigInteger.valueOf(4));
        cellLeft.setSpace(java.math.BigInteger.ZERO);
        cellLeft.setColor("000000");

        CTBorder cellRight = cellBorders.isSetRight() ? cellBorders.getRight() : cellBorders.addNewRight();
        cellRight.setVal(STBorder.SINGLE);
        cellRight.setSz(java.math.BigInteger.valueOf(4));
        cellRight.setSpace(java.math.BigInteger.ZERO);
        cellRight.setColor("000000");
    }

    private static void setCellCenter(XWPFTableCell cell, Integer fontSize) {
        if (cell == null) return;
        removeCellMargin(cell);
        setCellVerticalCenter(cell);
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        for (XWPFParagraph para : paragraphs) {
            para.setAlignment(ParagraphAlignment.CENTER);
            para.setSpacingBefore(0);
            para.setSpacingAfter(0);
            for (XWPFRun run : para.getRuns()) {
                if (fontSize != null) {
                    run.setFontSize(fontSize);
                }
                run.setFontFamily("宋体");
            }
        }
    }

    private static void setCellVerticalCenter(XWPFTableCell cell) {
        if (cell == null) return;
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        CTVerticalJc vAlign = tcPr.isSetVAlign() ? tcPr.getVAlign() : tcPr.addNewVAlign();
        vAlign.setVal(STVerticalJc.CENTER);
    }
}

package com.xiongx9527.fw.plus.template.openpdf;

import com.xiongx9527.fw.plus.template.filechange.AbstractExcel2Pdf;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dom4j.DocumentException;
import com.lowagie.text.pdf.PdfPCell;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * OpenPdfExcel2Pdf类实现了AbstractExcel2Pdf接口，用于将Excel文件转换为PDF格式的功能。
 *
 * <p>该类作为Excel转PDF功能的具体实现，负责解析Excel文件的内容并将其转换为PDF文档。
 * 它利用OpenPDF库处理PDF文档的生成，支持将Excel工作表转换为PDF表格。</p>
 *
 * @since 1.0
 */
public class OpenPdfExcel2Pdf extends AbstractExcel2Pdf {

    /** 标题字体样式：Helvetica字体，10号大小，粗体，白色 */
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

    /** 标题背景颜色：RGB值为(70, 130, 180)的钢蓝色 */
    private static final Color HEADER_BACKGROUND_COLOR = new Color(70, 130, 180);

    /** 标题内边距：8像素 */
    private static final int HEADER_PADDING = 8;


    /**
     * 添加文档头部信息
     *
     * @param document   PDF文档对象，用于添加头部内容
     * @param excelPath  Excel文件路径，用于提取文件名显示在头部
     * @param sheetCount 工作表数量，显示在头部信息中
     * @throws DocumentException 当添加文档内容失败时抛出此异常
     */
    private void addDocumentHeader(Document document, String excelPath, int sheetCount)
            throws DocumentException {

        String fileName = new File(excelPath).getName();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 主标题
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 0, 128));
        Paragraph title = new Paragraph("Excel文档导出", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 文件信息
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
        Paragraph info = new Paragraph();
        info.add(new Chunk("文件: ", infoFont));
        info.add(new Chunk(fileName + " ", new Font(Font.HELVETICA, 10, Font.BOLD)));
        info.add(new Chunk("工作表: ", infoFont));
        info.add(new Chunk(sheetCount + "个 ", new Font(Font.HELVETICA, 10, Font.BOLD)));
        info.add(new Chunk("生成时间: ", infoFont));
        info.add(new Chunk(timestamp, new Font(Font.HELVETICA, 10, Font.BOLD)));
        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingAfter(30);
        document.add(info);
    }

    /**
     * 添加表头
     * @param table 表格对象，用于添加表头单元格
     * @param columnCount 列数，指定要创建的表头列数量
     */
    private void addTableHeader(PdfPTable table, int columnCount) {
        // 参数验证
        if (table == null) {
            throw new IllegalArgumentException("表格对象不能为空");
        }
        if (columnCount < 0) {
            throw new IllegalArgumentException("列数不能为负数");
        }

        // 循环创建并添加表头单元格
        for (int i = 0; i < columnCount; i++) {
            PdfPCell header = new PdfPCell(new Paragraph("列 " + (i + 1), HEADER_FONT));
            header.setBackgroundColor(HEADER_BACKGROUND_COLOR); // 钢蓝色
            header.setPadding(HEADER_PADDING);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header);
        }
    }

    /**
     * 添加表格数据
     * @param table PDF表格对象
     * @param sheet Excel工作表对象
     * @param columnCount 列数
     */
    private void addTableData(PdfPTable table, Sheet sheet, int columnCount) {
        Font cellFont = new Font(Font.HELVETICA, 9);
        Font boldFont = new Font(Font.HELVETICA, 9, Font.BOLD);

        final Color HEADER_BG_COLOR = new Color(240, 240, 240);
        final Color ALT_ROW_BG_COLOR = new Color(248, 248, 248);

        boolean isFirstRow = true;

        // 遍历Excel表格的每一行
        for (org.apache.poi.ss.usermodel.Row row : sheet) {
            // 遍历当前行的每个单元格
            for (int i = 0; i < columnCount; i++) {
                org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                String cellValue = getCellValue(cell);

                PdfPCell pdfCell;

                // 处理表头行，使用加粗字体和特殊背景色
                if (isFirstRow && cellValue != null && !cellValue.isEmpty()) {
                    pdfCell = new PdfPCell(new Paragraph(cellValue, boldFont));
                    pdfCell.setBackgroundColor(HEADER_BG_COLOR);
                } else {
                    pdfCell = new PdfPCell(new Paragraph(cellValue, cellFont));

                    // 为奇数行设置交替背景色
                    if (row.getRowNum() % 2 == 1) {
                        pdfCell.setBackgroundColor(ALT_ROW_BG_COLOR);
                    }
                }

                pdfCell.setPadding(5);
                pdfCell.setMinimumHeight(20);
                table.addCell(pdfCell);
            }
            isFirstRow = false;
        }
    }

    /**
     * 获取Excel工作表的最大列数
     *
     * @param sheet Excel工作表对象
     * @return 工作表中的最大列数，至少返回1
     */
    private int getMaxColumnCount(Sheet sheet) {
        // 参数校验，避免空指针异常
        if (sheet == null) {
            return 1; // 至少1列
        }

        int maxCols = 0;
        try {
            // 遍历所有行，查找最大列数
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum > maxCols) {
                    maxCols = lastCellNum;
                }
            }
        } catch (Exception e) {
            // POI操作可能出现异常，返回默认值保证至少1列
            return 1;
        }
        return Math.max(maxCols, 1); // 至少1列
    }

    /**
     * 获取单元格值
     * @param cell Excel单元格对象
     * @return 单元格的字符串表示值
     */
    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 使用线程安全的方式处理日期格式化
                    return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            .format(cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                } else {
                    double value = cell.getNumericCellValue();
                    // 改进浮点数比较方式
                    if (Math.abs(value - Math.floor(value)) < Double.MIN_VALUE) {
                        return String.valueOf((long) value);
                    } else {
                        return String.format("%.2f", value);
                    }
                }

            case BOOLEAN:
                return cell.getBooleanCellValue() ? "是" : "否";

            case FORMULA:
                // 处理公式类型单元格，优先尝试获取计算结果，异常时返回公式本身
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    // 处理公式计算异常
                    return cell.getCellFormula();
                } catch (NumberFormatException e) {
                    // 处理数字格式异常
                    return cell.getCellFormula();
                } catch (Exception e) {
                    // 处理其他异常
                    return cell.getCellFormula();
                }

            default:
                return "";
        }
    }

    /**
     * 添加工作表到PDF
     *
     * @param document PDF文档对象，用于添加内容
     * @param sheet Excel工作表对象，包含要转换的数据
     * @param sheetIndex 工作表索引，用于显示工作表序号
     * @throws DocumentException 当PDF文档操作失败时抛出此异常
     */
    private void addSheetToPdf(Document document, Sheet sheet, int sheetIndex)
            throws DocumentException {

        // 防御性检查
        if (document == null || sheet == null) {
            throw new IllegalArgumentException("Document and Sheet must not be null");
        }

        String sheetName = sheet.getSheetName();
        if (sheetName == null) {
            sheetName = ""; // 允许空名称但仍继续处理
        }

        // 工作表标题
        Font sheetFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0, 100, 0));
        Paragraph sheetTitle = new Paragraph(
                "工作表 " + (sheetIndex + 1) + ": " + sheetName,
                sheetFont
        );
        sheetTitle.setSpacingAfter(15);
        document.add(sheetTitle);

        // 创建表格
        int columnCount = getMaxColumnCount(sheet);
        if (columnCount <= 0) return; // 确保列数有效

        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(20f);

        // 添加表头
        addTableHeader(table, columnCount);

        // 添加数据行
        addTableData(table, sheet, columnCount);

        document.add(table);
    }


    /**
     * 向文档中添加页眉信息
     *
     * @param document 文档对象，用于添加内容
     * @param sheet 工作表对象，用于获取工作表名称等信息
     * @param sheetCount 工作表数量
     * @throws DocumentException 当文档操作出现异常时抛出
     */
    private void addDocumentHeader(Document document, Sheet sheet, int sheetCount) throws DocumentException {
        if (sheet == null) {
            throw new IllegalArgumentException("Sheet cannot be null");
        }

        // 添加主标题
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 0, 128));
        Paragraph title = new Paragraph(sheet.getSheetName(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // 添加文件信息段落
        Font infoFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
        Font boldInfoFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        Paragraph info = new Paragraph();
        info.add(new Chunk("文件: ", infoFont));
        info.add(new Chunk(String.format("%s ", sheet.getSheetName()), boldInfoFont));
        info.add(new Chunk("工作表: ", infoFont));
        info.add(new Chunk(String.format("%d个 ", sheetCount), boldInfoFont));
        info.add(new Chunk("生成时间: ", infoFont));

        // 添加生成时间戳
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        info.add(new Chunk(timestamp, boldInfoFont));

        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingAfter(30);
        document.add(info);
    }


    /**
     * 将Excel文件数据转换为PDF格式
     *
     * @param srcData 源Excel文件的字节数组，不能为null或空数组
     * @return 转换后的PDF文件字节数组
     * @throws Exception 当转换过程中发生错误时抛出异常
     */
    @Override
    public byte[] change(byte[] srcData) throws Exception {
        if (srcData == null || srcData.length == 0) {
            throw new IllegalArgumentException("源数据不能为空");
        }

        Workbook workbook = null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(srcData)) {
            // 直接由 POI 判断 Excel 版本（支持 .xls 和 .xlsx）
            workbook = WorkbookFactory.create(inputStream);

            if (workbook == null) {
                throw new RuntimeException("无法解析Excel文件，请确认文件格式正确");
            }

            // 创建横向A4页面的PDF文档
            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                PdfWriter.getInstance(document, outputStream);
                document.open();

                // 添加文档标题
                addDocumentHeader(document, workbook.getSheetAt(0), workbook.getNumberOfSheets());

                // 处理每个工作表
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    if (sheet.getPhysicalNumberOfRows() > 0) {
                        addSheetToPdf(document, sheet, i);

                        // 如果不是最后一个工作表，添加分页
                        if (i < workbook.getNumberOfSheets() - 1) {
                            document.newPage();
                        }
                    }
                }
            } finally {
                document.close();
                outputStream.close();
            }

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new Exception("转换过程中发生错误：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException ignored) {
                    // 忽略关闭异常
                }
            }
        }
    }
}

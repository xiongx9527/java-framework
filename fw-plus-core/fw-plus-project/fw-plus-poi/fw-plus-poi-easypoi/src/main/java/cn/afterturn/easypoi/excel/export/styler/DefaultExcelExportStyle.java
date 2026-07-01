package cn.afterturn.easypoi.excel.export.styler;

import org.apache.poi.ss.usermodel.*;

public class DefaultExcelExportStyle extends ExcelExportStylerDefaultImpl {
    /**
     * 构造函数，初始化默认Excel导出样式
     * @param workbook 工作簿对象
     */
    public DefaultExcelExportStyle(Workbook workbook) {
        super(workbook);
    }

    /**
     * 获取标题单元格样式
     * @param color 颜色参数
     * @return CellStyle 返回设置好的标题单元格样式
     */
    @Override
    public CellStyle getTitleStyle(short color) {
        CellStyle titleStyle = this.workbook.createCellStyle();
        Font font = this.workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short)12);
        titleStyle.setFont(font);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setWrapText(true);
        titleStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return titleStyle;
    }
}

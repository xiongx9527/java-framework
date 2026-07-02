package com.xiongx9527.fw.plus.template.excel;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import com.xiongx9527.fw.plus.template.easypoi.EasyPoiExcel2Html;
import com.xiongx9527.fw.plus.template.filechange.AbstractExcel2Pdf;
import com.xiongx9527.fw.plus.template.openpdf.OpenPdfExcel2Pdf;
import org.apache.poi.ss.usermodel.Workbook;
import com.xiongx9527.fw.plus.template.filechange.AbstractExcel2Html ;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ExcelParser extends ExcelAbstractParser {

    /**
     * Excel解析器类，用于将Excel文件转换为HTML或PDF格式
     */
    private final AbstractExcel2Html excel2Html;

    /** PDF转换器实例，用于将Excel文件转换为PDF格式 */
    private final AbstractExcel2Pdf excel2Pdf;

    /**
     * Excel解析器构造函数
     * 初始化Excel转PDF和Excel转HTML的转换器实例
     */
    public ExcelParser() {
        this.excel2Pdf = new OpenPdfExcel2Pdf();
        this.excel2Html = new EasyPoiExcel2Html();
    }

    /**
     * 构造函数，使用指定的HTML转换器和PDF转换器初始化Excel解析器
     * @param excel2Html HTML转换器实例，用于将Excel转换为HTML格式
     * @param excel2Pdf PDF转换器实例，用于将Excel转换为PDF格式
     */
    public ExcelParser(AbstractExcel2Html excel2Html, AbstractExcel2Pdf excel2Pdf) {
        this.excel2Html = excel2Html;
        this.excel2Pdf = excel2Pdf;
    }

    /**
     * 将Workbook对象转换为字节数组
     *
     * @param workbook 要转换的Workbook对象
     * @return 转换后的字节数组
     * @throws IOException 当写入操作发生错误时抛出
     */
    private byte[] convertWorkbookToByteArray(Workbook workbook) throws IOException {
        // 使用ByteArrayOutputStream将Workbook写入字节数组
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 根据模板数据和填充参数解析生成Excel文件
     *
     * @param data 模板文件的字节数组数据
     * @param map 用于填充Excel模板的数据映射
     * @return 填充数据后生成的Excel文件字节数组
     * @throws Exception 解析或导出过程中发生的异常
     */
    @Override
    public byte[] parseByData(byte[] data, Map<String, Object> map) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            TemplateExportParams params = new TemplateExportParams(inputStream);
            // 导出填充数据后的Excel文件
            Workbook workbook = ExcelExportUtil.exportExcel(params, map);
            if (workbook == null) {
                throw new RuntimeException("Failed to export Excel workbook");
            }
            return convertWorkbookToByteArray(workbook);
        } catch (Exception e) {
            // 可以根据需要转换为更具体的业务异常
            throw e;
        }
    }

    /**
     * 将Excel字节数组转换为HTML字节数组
     *
     * @param srcBytes 源Excel文件的字节数组
     * @return 转换后的HTML字节数组
     * @throws UnsupportedOperationException 当前实现不支持Excel到HTML的转换功能
     */
    @Override
    public byte[] toHmtl(byte[] srcBytes) {
        try {
            return this.excel2Html.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将源字节数组转换为PDF格式的字节数组
     *
     * @param srcBytes 源数据的字节数组
     * @return 转换后的PDF格式字节数组
     */
    @Override
    public byte[] toPdf(byte[] srcBytes) {
        try {
            return this.excel2Pdf.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

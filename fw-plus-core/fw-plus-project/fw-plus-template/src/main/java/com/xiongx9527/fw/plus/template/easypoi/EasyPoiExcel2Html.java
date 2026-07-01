package com.xiongx9527.fw.plus.template.easypoi;

import cn.afterturn.easypoi.excel.ExcelXorHtmlUtil;
import com.xiongx9527.fw.plus.template.filechange.AbstractExcel2Html;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;

public class EasyPoiExcel2Html extends AbstractExcel2Html {
    /**
     * 将Excel字节数组转换为HTML格式的字节数组
     *
     * @param srcData 输入的Excel文件字节数组，不能为空或长度为0
     * @return 转换后的HTML格式字节数组
     * @throws Exception 当文件解析失败或转换过程中发生错误时抛出异常
     */
    @Override
    public byte[] change(byte[] srcData) throws Exception {
        if (srcData == null || srcData.length == 0) {
            throw new IllegalArgumentException("srcData cannot be null or empty");
        }
        Workbook workbook = null;
        // 判断Excel文件类型并创建对应的工作簿对象
        if (srcData.length > 4) {
            // 检查是否为xlsx格式
            if (srcData[0] == (byte) 0x50 && srcData[1] == (byte) 0x4B) {
                // xlsx格式
                workbook = WorkbookFactory.create(new ByteArrayInputStream(srcData));
            } else {
                // xls格式，直接使用POI处理
                workbook = WorkbookFactory.create(new ByteArrayInputStream(srcData));
            }
        }
        if (workbook == null) {
            throw new RuntimeException("无法解析Excel文件");
        }
        // 生成HTML内容并转换为UTF-8字节数组
        String htmlContent = ExcelXorHtmlUtil.toAllHtml(workbook);
        return htmlContent.getBytes("UTF-8");
    }
}

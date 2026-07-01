/**
 * Copyright 2013-2015 JueYue (qrb.jueyue@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.afterturn.easypoi.view;

import cn.afterturn.easypoi.util.WebFilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基础抽象Excel View
 * @author JueYue
 *  2015年2月28日 下午1:41:05
 */
public abstract class MiniAbstractExcelView extends PoiBaseView {

    /**
     * 内容类型常量，用于指定HTTP响应的内容类型
     * 包含HTML文本和Excel文件类型
     */
    private static final String CONTENT_TYPE = "text/html;application/vnd.ms-excel";

    /**
     * Excel 97-2003格式文件扩展名常量
     * 用于标识.xls格式的Excel文件
     */
    protected static final String HSSF = ".xls";

    /**
     * Excel 2007及以上版本格式文件扩展名常量
     * 用于标识.xlsx格式的Excel文件
     */
    protected static final String XSSF = ".xlsx";


    /**
     * Excel视图抽象类的构造函数
     *
     * 初始化Excel视图实例，设置默认的内容类型为HTML和Excel混合格式
     */
    public MiniAbstractExcelView() {
        setContentType(CONTENT_TYPE);
    }


    /**
     * 将Workbook对象输出到HTTP响应流中
     *
     * @param workbook 要输出的工作簿对象，可以是HSSFWorkbook或XSSFWorkbook实例
     * @param codedFileName 文件名，会根据工作簿类型自动添加相应的扩展名
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 当输出过程中发生错误时抛出异常
     */
    public void out(Workbook workbook, String codedFileName, HttpServletRequest request,
                    HttpServletResponse response) throws Exception {
        // 根据工作簿类型确定文件扩展名
        if (workbook instanceof HSSFWorkbook) {
            codedFileName += HSSF;
        } else {
            codedFileName += XSSF;
        }
        // 用工具类生成符合RFC 5987标准的文件名header, 去掉UA判断
        response.setHeader("content-disposition", WebFilenameUtils.disposition(codedFileName));
        ServletOutputStream out = response.getOutputStream();
        workbook.write(out);
        out.flush();
    }


}

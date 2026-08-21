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

import cn.afterturn.easypoi.entity.vo.BigExcelConstants;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.handler.inter.IExcelExportServer;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author JueYue on 14-3-8. Excel 生成解析器,减少用户操作
 */
@Controller(BigExcelConstants.EASYPOI_BIG_EXCEL_VIEW)
public class EasypoiBigExcelExportView extends MiniAbstractExcelView {

    /**
     * EasypoiBigExcelExportView构造函数
     *
     * <p>初始化Easypoi大数据Excel导出视图实例</p>
     *
     * @since 1.0
     */
    public EasypoiBigExcelExportView() {
        super();
    }

    /**
     * 渲染合并的输出模型，生成Excel文件并输出到响应流中
     *
     * @param model 包含导出参数的模型Map，应包含导出参数、实体类、数据接口等信息
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 导出过程中可能抛出的异常
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        // 设置默认文件名
        String codedFileName = "临时文件";

        // 调用Excel导出工具生成工作簿
        Workbook workbook = ExcelExportUtil.exportBigExcel(
                (ExportParams) model.get(BigExcelConstants.PARAMS),
                (Class<?>) model.get(BigExcelConstants.CLASS),
                (IExcelExportServer) model.get(BigExcelConstants.DATA_INTER),
                model.get(BigExcelConstants.DATA_PARAMS));

        // 如果模型中包含自定义文件名，则使用自定义文件名
        if (model.containsKey(BigExcelConstants.FILE_NAME)) {
            codedFileName = (String) model.get(BigExcelConstants.FILE_NAME);
        }

        // 输出Excel文件到响应流
        out(workbook, codedFileName, request, response);
    }

}

/**
 * Copyright 2013-2015 JueYue (qrb.jueyue@gmail.com)
 *   
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.afterturn.easypoi.view;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;

import cn.afterturn.easypoi.entity.vo.NormalExcelConstants;
import cn.afterturn.easypoi.entity.vo.TemplateExcelConstants;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;

/**
 * Excel模板视图
 * 
 * @author JueYue
 *  2014年6月30日 下午9:15:49
 */
@SuppressWarnings("unchecked")
@Controller(TemplateExcelConstants.EASYPOI_TEMPLATE_EXCEL_VIEW)
public class EasypoiTemplateExcelView extends MiniAbstractExcelView {

    /**
     * EasypoiTemplateExcelView构造函数
     *
     * <p>创建一个新的EasypoiTemplateExcelView实例，调用父类构造函数进行初始化</p>
     *
     * @since 1.0
     */
    public EasypoiTemplateExcelView() {
        super();
    }

    /**
     * 渲染合并的输出模型，生成Excel文件并输出到响应流
     *
     * @param model 包含导出参数的模型数据，包括模板参数、数据类、列表数据、映射数据等
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 导出过程中可能抛出的异常
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        // 设置默认文件名
        String codedFileName = "临时文件";

        // 使用Excel导出工具生成工作簿
        @SuppressWarnings("deprecation")
        Workbook workbook = ExcelExportUtil.exportExcel(
            (TemplateExportParams) model.get(TemplateExcelConstants.PARAMS),
            (Class<?>) model.get(TemplateExcelConstants.CLASS),
            (List<?>) model.get(TemplateExcelConstants.LIST_DATA),
            (Map<String, Object>) model.get(TemplateExcelConstants.MAP_DATA));

        // 如果模型中包含自定义文件名，则使用自定义文件名
        if (model.containsKey(NormalExcelConstants.FILE_NAME)) {
            codedFileName = (String) model.get(NormalExcelConstants.FILE_NAME);
        }

        // 输出Excel文件到响应流
        out(workbook, codedFileName, request, response);
    }

}

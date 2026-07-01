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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;

import cn.afterturn.easypoi.entity.vo.MapExcelConstants;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;

/**
 * Map 对象接口
 * 
 * @author JueYue
 *  2014年11月25日 下午3:26:32
 */
@SuppressWarnings("unchecked")
@Controller(MapExcelConstants.EASYPOI_MAP_EXCEL_VIEW)
public class EasypoiMapExcelView extends MiniAbstractExcelView {

    /**
     * 构造函数，初始化EasypoiMapExcelView实例
     *
     * @since 1.0
     */
    public EasypoiMapExcelView() {
        super();
    }


    /**
     * 渲染合并输出模型，生成Excel文件并输出到响应流中
     *
     * @param model 包含导出数据和配置参数的模型Map
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 导出过程中可能抛出的异常
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        // 设置默认文件名
        String codedFileName = "临时文件";

        // 调用工具类生成Excel工作簿
        Workbook workbook = ExcelExportUtil.exportExcel(
            (ExportParams) model.get(MapExcelConstants.PARAMS),
            (List<ExcelExportEntity>) model.get(MapExcelConstants.ENTITY_LIST),
            (Collection<? extends Map<?, ?>>) model.get(MapExcelConstants.MAP_LIST));

        // 如果模型中包含自定义文件名，则使用自定义文件名
        if (model.containsKey(MapExcelConstants.FILE_NAME)) {
            codedFileName = (String) model.get(MapExcelConstants.FILE_NAME);
        }

        // 输出Excel文件到响应流
        out(workbook, codedFileName, request, response);
    }


}

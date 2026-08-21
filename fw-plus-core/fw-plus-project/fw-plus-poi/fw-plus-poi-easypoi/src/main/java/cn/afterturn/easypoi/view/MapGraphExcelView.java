package cn.afterturn.easypoi.view;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.graph.builder.ExcelChartBuildService;
import cn.afterturn.easypoi.excel.graph.entity.ExcelGraph;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;

@Controller("MapGraphExcelView")
public class MapGraphExcelView extends MiniAbstractExcelView {

    /**
     * 构造函数，用于创建MapGraphExcelView实例
     */
    public MapGraphExcelView() {
    }

    /**
     * 渲染合并的输出模型，生成Excel文件并输出到响应流
     *
     * @param model 包含导出数据和参数的模型对象
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String codedFileName = "临时文件";
        // 从模型中获取Excel导出参数
        ExportParams params = (ExportParams)model.get("params");
        // 从模型中获取实体列表
        List<ExcelExportEntity> entityList = (List)model.get("EntityList");
        // 从模型中获取数据列表
        List<Map<String, Object>> mapList = (List)model.get("mapList");
        // 从模型中获取图表定义列表
        List<ExcelGraph> graphDefinedList = (List)model.get("graphDefined");
        // 根据参数和数据生成Excel工作簿
        Workbook workbook = ExcelExportUtil.exportExcel(params, entityList, mapList);
        // 创建Excel图表
        ExcelChartBuildService.createExcelChart(workbook, graphDefinedList, params.isDynamicData(), params.isAppendGraph());
        // 如果模型中包含文件名，则使用指定的文件名
        if (model.containsKey("fileName")) {
            codedFileName = (String)model.get("fileName");
        }

        this.out(workbook, codedFileName, request, response);
    }

}

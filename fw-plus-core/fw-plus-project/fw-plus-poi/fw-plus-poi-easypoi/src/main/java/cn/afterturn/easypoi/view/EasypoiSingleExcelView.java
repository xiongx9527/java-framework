package cn.afterturn.easypoi.view;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.afterturn.easypoi.excel.export.FwPlusExcelExportService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Controller;

@Controller("easypoiExcelView")
public class EasypoiSingleExcelView extends MiniAbstractExcelView {

    /**
     * EasypoiSingleExcelView构造函数
     *
     * 该构造函数用于创建EasypoiSingleExcelView实例，初始化Excel视图处理对象
     * 此类基于Easypoi框架实现单个Excel文件的导出功能
     */
    public EasypoiSingleExcelView() {
    }

    /**
     * 渲染合并的输出模型，生成Excel文件并输出到响应流
     *
     * @param model 包含导出数据和参数的模型Map
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String codedFileName = "临时文件";
        Workbook workbook = null;

        // 根据模型中是否包含mapList来决定导出方式
        if (model.containsKey("mapList")) {
            List<Map<String, Object>> list = (List)model.get("mapList");
            if (list.size() == 0) {
                throw new RuntimeException("MAP_LIST IS NULL");
            }

            // 导出第一个工作表
            workbook = ExcelExportUtil.exportExcel((ExportParams)((Map)list.get(0)).get("params"), (Class)((Map)list.get(0)).get("entity"), (Collection)((Map)list.get(0)).get("data"));

            // 循环创建其他工作表
            for(int i = 1; i < list.size(); ++i) {
                (new FwPlusExcelExportService()).createSheet(workbook, (ExportParams)((Map)list.get(i)).get("params"), (Class)((Map)list.get(i)).get("entity"), (Collection)((Map)list.get(i)).get("data"));
            }
        } else {
            // 单个工作表导出
            workbook = ExcelExportUtil.exportExcel((ExportParams)model.get("params"), (Class)model.get("entity"), (Collection)model.get("data"));
        }

        // 获取自定义文件名
        if (model.containsKey("fileName")) {
            codedFileName = (String)model.get("fileName");
        }

        this.out(workbook, codedFileName, request, response);
    }

}

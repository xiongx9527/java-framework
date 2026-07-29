package cn.afterturn.easypoi.excel;

import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusTemplateExportParams;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.export.ExcelBatchExportService;
import cn.afterturn.easypoi.excel.export.FwPlusExcelBatchExportService;
import cn.afterturn.easypoi.excel.export.FwPlusExcelExportService;
import cn.afterturn.easypoi.excel.export.template.ExcelExportOfTemplateUtil;
import cn.afterturn.easypoi.handler.inter.IExcelExportServer;
import cn.afterturn.easypoi.handler.inter.IWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.afterturn.easypoi.util.TransUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelExportUtil {
    /**
     * Excel导出工具类使用的SXSSF模式触发阈值
     * 当数据量超过此限制时，将使用SXSSF模式进行Excel导出以节省内存
     */
    public static int USE_SXSSF_LIMIT = 1000000;
    //public static final String SHEET_NAME = "sheetName";

    /**
     * 私有构造函数，防止实例化工具类
     */
    private ExcelExportUtil() {
    }


    /**
     * 导出大量数据的Excel文件
     * 
     * @param entity    导出参数配置对象，包含导出Excel的基本配置信息
     * @param pojoClass 数据对象的Class类型，用于映射导出数据的结构
     * @return IWriter<Workbook> 返回一个Workbook写入器实例，用于处理大批量数据的分批写入
     */
    public static IWriter<Workbook> exportBigExcel(ExportParams entity, Class<?> pojoClass) {
        ExcelBatchExportService batchServer = new FwPlusExcelBatchExportService();
        batchServer.init(entity, pojoClass);
        return batchServer;
    }

    /**
     * 导出大量数据的Excel文件
     * 
     * @param entity       导出参数配置对象，包含导出Excel的基本配置信息
     * @param excelParams  Excel导出实体列表，用于定义导出列的属性和样式
     * @return IWriter<Workbook> 返回一个Workbook写入器实例，用于处理大批量数据的分批写入
     */
    public static IWriter<Workbook> exportBigExcel(ExportParams entity, List<ExcelExportEntity> excelParams) {
        ExcelBatchExportService batchServer = new FwPlusExcelBatchExportService();
        batchServer.init(entity, excelParams);
        return batchServer;
    }

    /**
     * 导出大量数据的Excel文件，通过服务端分页获取数据
     * 
     * @param entity       导出参数配置对象，包含导出Excel的基本配置信息
     * @param pojoClass    数据对象的Class类型，用于映射导出数据的结构
     * @param server       Excel导出服务接口，用于分批获取数据
     * @param queryParams  查询参数对象，传递给服务接口用于获取数据
     * @return Workbook    返回生成的Excel工作簿对象
     */
    public static Workbook exportBigExcel(ExportParams entity, Class<?> pojoClass, IExcelExportServer server, Object queryParams) {
        ExcelBatchExportService batchServer = new FwPlusExcelBatchExportService();
        batchServer.init(entity, pojoClass);
        return batchServer.exportBigExcel(server, queryParams);
    }

    /**
     * 导出大量数据的Excel文件，通过服务端分页获取数据
     * 
     * @param entity       导出参数配置对象，包含导出Excel的基本配置信息
     * @param excelParams  Excel导出实体列表，用于定义导出列的属性和样式
     * @param server       Excel导出服务接口，用于分批获取数据
     * @param queryParams  查询参数对象，传递给服务接口用于获取数据
     * @return Workbook    返回生成的Excel工作簿对象
     */
    public static Workbook exportBigExcel(ExportParams entity, List<ExcelExportEntity> excelParams, IExcelExportServer server, Object queryParams) {
        ExcelBatchExportService batchServer = new FwPlusExcelBatchExportService();
        batchServer.init(entity, excelParams);
        return batchServer.exportBigExcel(server, queryParams);
    }

    public static Workbook exportExcel(ExportParams entity, Class<?> pojoClass, Collection<?> dataSet) {
        Workbook workbook = getWorkbook(entity.getType(), dataSet.size());
        FwPlusExcelExportService excelExportService = new FwPlusExcelExportService();
        excelExportService.init(entity);
        excelExportService.createSheet(workbook, entity, pojoClass, dataSet);
        return workbook;
    }

    /**
     * 根据Excel类型创建相应的工作簿对象
     * 
     * @param type  Excel类型枚举，决定创建HSSF(.xls)还是XSSF(.xlsx)格式的工作簿
     * @param size  数据大小，用于初始化工作簿（在某些实现中可能用到）
     * @return Workbook 返回创建的Excel工作簿对象
     */
    private static Workbook getWorkbook(ExcelType type, int size) {
        return (Workbook)(ExcelType.HSSF.equals(type) ? new HSSFWorkbook() : new XSSFWorkbook());
    }

    /**
     * 导出Excel文件，使用自定义的导出实体列表
     * 
     * @param entity       导出参数配置对象，包含导出Excel的基本配置信息
     * @param entityList   Excel导出实体列表，用于定义导出列的属性和样式
     * @param dataSet      数据集合，包含需要导出的实际数据
     * @return Workbook    返回生成的Excel工作簿对象
     */
    public static Workbook exportExcel(ExportParams entity, List<ExcelExportEntity> entityList, Collection<?> dataSet) {
        Workbook workbook = getWorkbook(entity.getType(), dataSet.size());
        FwPlusExcelExportService excelExportService = new FwPlusExcelExportService();
        excelExportService.init(entity);
        excelExportService.createSheetForMap(workbook, entity, entityList, dataSet);
        return workbook;
    }

    /**
     * 导出多个Sheet的Excel文件
     * 
     * @param list  包含多个Sheet数据的列表，每个元素是一个Map，包含"title"(ExportParams)、"entity"(Class)和"data"(Collection)键值对
     * @param type  Excel类型枚举，决定创建HSSF(.xls)还是XSSF(.xlsx)格式的工作簿
     * @return Workbook 返回生成的包含多个Sheet的Excel工作簿对象
     */
    public static Workbook exportExcel(List<Map<String, Object>> list, ExcelType type) {
        Workbook workbook = getWorkbook(type, 0);
        FwPlusExcelExportService service = null;
        for(Map<String, Object> map : list) {
            service = new FwPlusExcelExportService();
            ExportParams params = (ExportParams)map.get("title");
            service.init(params);
            params.setType(type);
            service.createSheet(workbook, params, (Class)map.get("entity"), (Collection)map.get("data"));
        }
        return workbook;
    }


    /**
     * 导出基于模板的Excel文件（已废弃）
     * 
     * @param params     模板导出参数配置对象
     * @param pojoClass  数据对象的Class类型
     * @param dataSet    数据集合
     * @param map        模板参数Map
     * @return Workbook  返回生成的Excel工作簿对象
     * @deprecated 此方法已废弃，不推荐使用
     */
    /** @deprecated */
    @Deprecated
    public static Workbook exportExcel(TemplateExportParams params, Class<?> pojoClass, Collection<?> dataSet, Map<String, Object> map) {
        Collection<?> exportDataSet = dataSet;
        if (params instanceof FwPlusTemplateExportParams) {
            FwPlusTemplateExportParams fwPlusTemplateExportParams = (FwPlusTemplateExportParams)params;
            exportDataSet = TransUtils.transExportCollectionByTemplate(fwPlusTemplateExportParams, dataSet);
        }
        return (new ExcelExportOfTemplateUtil()).createExcelByTemplate(params, pojoClass, exportDataSet, map);
    }

    /**
     * 导出基于模板的Excel文件（仅使用Map数据）
     * 
     * @param params  模板导出参数配置对象
     * @param map     模板参数Map，包含用于填充模板的数据
     * @return Workbook 返回生成的Excel工作簿对象
     */
    public static Workbook exportExcel(TemplateExportParams params, Map<String, Object> map) {
        return (new ExcelExportOfTemplateUtil()).createExcelByTemplate(params, (Class)null, (Collection)null, map);
    }

    /**
     * 导出基于模板的Excel文件（使用带索引的Map数据）
     * 
     * @param map     带索引的Map数据，键为Sheet索引，值为包含Sheet数据的Map
     * @param params  模板导出参数配置对象
     * @return Workbook 返回生成的Excel工作簿对象
     */
    public static Workbook exportExcel(Map<Integer, Map<String, Object>> map, TemplateExportParams params) {
        return (new ExcelExportOfTemplateUtil()).createExcelByTemplate(params, map);
    }

    /**
     * 导出克隆模板的Excel文件
     * 
     * @param map     带索引的List<Map>数据，用于克隆模板生成多个相似结构的Sheet
     * @param params  模板导出参数配置对象
     * @return Workbook 返回生成的Excel工作簿对象
     */
    public static Workbook exportExcelClone(Map<Integer, List<Map<String, Object>>> map, TemplateExportParams params) {
        return (new ExcelExportOfTemplateUtil()).createExcelCloneByTemplate(params, map);
    }

}

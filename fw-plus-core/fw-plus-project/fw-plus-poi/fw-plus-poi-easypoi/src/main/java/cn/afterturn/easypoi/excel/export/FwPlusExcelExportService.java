package cn.afterturn.easypoi.excel.export;

import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Collection;

/**
 * Plus导出服务
 */
public class FwPlusExcelExportService extends ExcelExportService implements FwPlusExcelTransService{
    /** FwPlus导出参数对象 */
    private FwPlusExportParams fwPlusExportParams;

    /**
     * 默认构造函数
     */
    public FwPlusExcelExportService() {
    }

    /**
     * 初始化方法，根据传入的ExportParams实体进行特定处理
     * 此方法的目的在于根据传入参数的类型，将其转换并赋值给特定的类变量
     * 这样做可以使得类在处理不同类型的ExportParams时更加灵活和高效
     *
     * @param entity ExportParams类型的实体，可以是FwPlusExportParams的实例
     */
    public void init(ExportParams entity) {
        // 检查传入的entity是否为FwPlusExportParams类型，如果是，则进行类型转换并赋值给fwPlusExportParams
        // 这样处理是为了让类能够处理特定类型的ExportParams，从而实现更细致的控制和优化
        if (entity instanceof FwPlusExportParams) {
            fwPlusExportParams = (FwPlusExportParams) entity;
        } else {
            fwPlusExportParams = null;
        }
    }

    /**
     * 重写创建工作表的方法以支持自定义导出行为
     *
     * @param workbook Excel工作簿对象，用于创建和管理工作表
     * @param entity 导出参数对象，包含导出的样式和配置信息
     * @param pojoClass 数据模型类的Class对象，用于反射和映射数据到单元格
     * @param dataSet 要导出的数据集合，包含实际要写入工作表的数据
     */
    @Override
    public void createSheet(Workbook workbook, ExportParams entity, Class<?> pojoClass, Collection<?> dataSet) {
        // 调用父类的createSheet方法进行标准的创建工作表流程
        super.createSheet(workbook, entity, pojoClass, transExportCollection(fwPlusExportParams,dataSet));
    }
}

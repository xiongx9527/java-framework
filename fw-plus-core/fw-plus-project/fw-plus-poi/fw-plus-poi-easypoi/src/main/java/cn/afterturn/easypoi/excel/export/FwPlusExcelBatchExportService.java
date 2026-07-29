package cn.afterturn.easypoi.excel.export;

import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.handler.inter.IWriter;
import org.apache.poi.ss.usermodel.Workbook;
import java.util.*;

/**
 * 批量导出Excel
 */
public class FwPlusExcelBatchExportService extends ExcelBatchExportService implements FwPlusExcelTransService {
    /** FwPlus导出参数对象 */
    private FwPlusExportParams fwPlusExportParams;
    
    /**
     * 初始化导出服务
     * @param entity    导出参数配置对象
     * @param pojoClass 数据对象的Class类型
     */
    @Override
    public void init(ExportParams entity, Class<?> pojoClass) {
        if (entity instanceof FwPlusExportParams) {
            fwPlusExportParams = (FwPlusExportParams) entity;
        } else {
            fwPlusExportParams = null;
        }
        super.init(entity, pojoClass);
    }

    @Override
    public void init(ExportParams entity, List<ExcelExportEntity> excelParams) {
        if (entity instanceof FwPlusExportParams) {
            fwPlusExportParams = (FwPlusExportParams) entity;
        } else {
            fwPlusExportParams = null;
        }
        super.init(entity, excelParams);
    }

    /**
     * 重写write方法以处理数据导出
     * 在调用父类的write方法之前，首先对导出的数据进行转换处理
     *
     * @param data 要写入的Collection类型数据
     * @return 返回一个IWriter<Workbook>对象，用于进一步操作Workbook
     */
    @Override
    public IWriter<Workbook> write(Collection data) {
        // 调用父类的write方法进行实际的数据写入操作
        return super.write(transExportCollection(fwPlusExportParams,data));
    }

}

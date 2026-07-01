package cn.afterturn.easypoi.excel.imports;

import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.excel.export.FwPlusExcelTransService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.InputStream;
import java.util.List;

/**
 * @author EDY
 */
public class FwPlusExcelImportService extends ExcelImportService implements FwPlusExcelTransService {

    /**
     * 处理Excel导入结果的转换操作
     * 此方法旨在根据导入参数中的指示，对成功和失败的导入记录进行额外的转换处理
     *
     * @param excelImportResult Excel导入结果对象，包含成功和失败的导入记录列表
     * @param params 导入参数对象，指导如何进行导入和转换操作
     */
    private void trans(ExcelImportResult excelImportResult,ImportParams params) {
        // 确保导入结果和参数均有效，并且参数是FwPlusImportParams类型的实例
        if (ObjectUtils.isNotEmpty(excelImportResult) && ObjectUtils.isNotEmpty(params) && params instanceof FwPlusImportParams) {
            FwPlusImportParams fwPlusImportParams = (FwPlusImportParams) params;
            // 检查是否需要进行转换操作
            if (fwPlusImportParams.isNeedTrans()) {
                // 如果成功导入的记录列表不为空，则执行转换操作
                if (CollectionUtils.isNotEmpty(excelImportResult.getList())) {
                    excelImportResult.setList((List)transImportCollection(fwPlusImportParams, excelImportResult.getList()));
                }
                // 如果失败导入的记录列表不为空，则执行转换操作
                if (CollectionUtils.isNotEmpty(excelImportResult.getFailList())) {
                    excelImportResult.setFailList((List)transImportCollection(fwPlusImportParams, excelImportResult.getFailList()));
                }
            }
        }
    }

    /**
     * 重写父类的导入Excel方法，以实现特定的Excel数据导入功能
     * 此方法允许通过输入流导入Excel数据，并根据提供的参数和POJO类转换为对象列表
     *
     * @param inputstream 输入流，用于读取Excel文件数据
     * @param pojoClass POJO类的Class对象，表示Excel数据应映射到的目标对象类型
     * @param params 导入参数，包含导入过程中的配置和选项
     * @param needMore 标志位，指示是否需要额外的导入信息
     * @return 返回ExcelImportResult对象，包含导入结果和可能的错误信息
     * @throws Exception 如果导入过程中发生错误，则抛出异常
     */
    @Override
    public ExcelImportResult importExcelByIs(InputStream inputstream, Class<?> pojoClass, ImportParams params, boolean needMore) throws Exception {
        // 调用父类的importExcelByIs方法进行Excel数据导入，并获取导入结果
        ExcelImportResult excelImportResult = super.importExcelByIs(inputstream, pojoClass, params,needMore);
        trans(excelImportResult, params);
        // 返回导入结果
        return excelImportResult;
    }

}

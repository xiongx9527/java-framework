package cn.afterturn.easypoi.excel.export;

import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusTemplateExportParams;
import cn.afterturn.easypoi.util.TransUtils;

import java.util.Collection;

/**
 * @author EDY
 */
public interface FwPlusExcelTransService {

    /**
     * 根据导出参数转换集合
     * 此方法用于在数据导出过程中，根据是否需要转换来决定集合的处理方式
     * 主要用于当需要从给定集合中包含或排除特定字段时
     *
     * @param exportParams 导出参数对象，包含是否需要转换的标志以及需要包含或排除的字段
     * @param coll 待转换的原始集合
     * @return 根据导出参数可能进行了转换的集合如果不需要转换，则返回原始集合
     */
    default Collection<?> transExportCollection(FwPlusExportParams exportParams, Collection coll) {
        // 检查导出参数是否非空，并判断是否需要进行转换
        if (null != exportParams && exportParams.isNeedTrans()) {
            // 使用转换工具类TransUtils根据包含和排除的字段列表转换集合
            return TransUtils.transCollection(coll, exportParams.getIncludeFields(), exportParams.getExcludeFields());
        }
        // 如果不需要转换，直接返回原始集合
        return coll;
    }

    /**
     * 根据导入参数转换集合
     * 此方法用于在数据导入过程中，根据导入参数决定是否对给定集合进行转换
     * 主要目的是根据用户指定的字段列表包含或排除规则，对集合数据进行相应处理
     *
     * @param importParams 导入参数对象，包含是否需要转换的标志以及包含和排除的字段列表
     * @param coll 待转换的集合对象
     * @return 转换后的集合如果不需要转换，则直接返回原始集合
     */
    default Collection<?> transImportCollection(FwPlusImportParams importParams, Collection coll) {
        // 检查导出参数是否非空，并判断是否需要进行转换
        if (null != importParams && importParams.isNeedTrans()) {
            // 使用转换工具类TransUtils根据包含和排除的字段列表转换集合
            return TransUtils.transCollection(coll, importParams.getIncludeFields(), importParams.getExcludeFields());
        }
        // 如果不需要转换，直接返回原始集合
        return coll;
    }



}

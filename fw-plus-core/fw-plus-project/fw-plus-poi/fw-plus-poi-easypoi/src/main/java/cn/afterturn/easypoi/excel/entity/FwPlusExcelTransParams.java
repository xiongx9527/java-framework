package cn.afterturn.easypoi.excel.entity;

import java.util.Set;

public interface FwPlusExcelTransParams {

    /**
     * 是否需要翻译
     * @return boolean 返回是否需要翻译的布尔值，默认为false
     */
    default boolean isNeedTrans() {
        return false;
    }

    /**
     * 获取需要包含的字段集合
     * @return Set<String> 返回需要包含的字段名称集合
     */
    Set<String> getIncludeFields();

    /**
     * 获取需要排除的字段集合
     * @return Set<String> 返回需要排除的字段名称集合
     */
    Set<String> getExcludeFields();
}

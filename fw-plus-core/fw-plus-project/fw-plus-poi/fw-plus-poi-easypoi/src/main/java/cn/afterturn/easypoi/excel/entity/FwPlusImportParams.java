package cn.afterturn.easypoi.excel.entity;

import lombok.Data;

import java.util.Collections;
import java.util.Set;

@Data
public class FwPlusImportParams extends ImportParams  implements FwPlusExcelTransParams {
    /** 是否需要翻译 */
    private boolean needTrans = false;
    /** 用于存储需要翻译的字段列表 */
    private Set<String> includeFields = null;
    /** 用于存储不需要翻译的字段列表 */
    private Set<String> excludeFields = null;

}

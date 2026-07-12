package cn.afterturn.easypoi.excel.entity;

import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.export.styler.ExcelExportStylerDefaultImpl;
import lombok.Data;
import org.apache.poi.hssf.util.HSSFColor;

import java.util.Set;

@Data
public class FwPlusExportParams extends ExportParams implements FwPlusExcelTransParams {
    /** 是否需要翻译 */
    private boolean needTrans = false;
    /** 用于存储需要翻译的字段列表 */
    private Set<String> includeFields = null;
    /** 用于存储不需要翻译的字段列表 */
    private Set<String> excludeFields = null;

    /**
     * 默认构造函数
     */
    public FwPlusExportParams() {
        super();
    }

    /**
     * 构造函数，指定标题和工作表名称
     * @param title     Excel标题
     * @param sheetName 工作表名称
     */
    public FwPlusExportParams(String title, String sheetName) {
        super(title,sheetName);
    }

    /**
     * 构造函数，指定标题、工作表名称和Excel类型
     * @param title     Excel标题
     * @param sheetName 工作表名称
     * @param type      Excel类型
     */
    public FwPlusExportParams(String title, String sheetName, ExcelType type) {
        super(title,sheetName,type);
    }

    /**
     * 构造函数，指定标题、副标题和工作表名称
     * @param title       Excel标题
     * @param secondTitle Excel副标题
     * @param sheetName   工作表名称
     */
    public FwPlusExportParams(String title, String secondTitle, String sheetName) {
        super(title,secondTitle,sheetName);
    }

}

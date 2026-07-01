package cn.afterturn.easypoi.excel.entity;

import lombok.Data;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Data
public class FwPlusTemplateExportParams extends TemplateExportParams  implements FwPlusExcelTransParams {
    /** 用于存储需要翻译的字段列表 */
    private Set<String> includeFields = null;
    /** 用于存储不需要翻译的字段列表 */
    private Set<String> excludeFields = null;

    /**
     * 构造函数，通过模板URL创建导出参数
     * @param templateUrl   模板文件URL
     * @param scanAllsheet  是否扫描所有工作表
     * @param sheetName     工作表名称数组
     */
    public FwPlusTemplateExportParams(String templateUrl, boolean scanAllsheet, String... sheetName) {
        super(templateUrl, scanAllsheet, sheetName);
    }

    /**
     * 构造函数，通过模板URL创建导出参数
     * @param templateUrl   模板文件URL
     * @param sheetNum      工作表编号数组
     */
    public FwPlusTemplateExportParams(String templateUrl, Integer... sheetNum) {
        super(templateUrl, sheetNum);
    }

    /**
     * 构造函数，通过模板URL创建导出参数
     * @param templateUrl   模板文件URL
     * @param sheetName     工作表名称
     * @param sheetNum      工作表编号数组
     */
    public FwPlusTemplateExportParams(String templateUrl, String sheetName, Integer... sheetNum) {
        super(templateUrl,sheetName , sheetNum);
    }

    /**
     * 构造函数，通过输入流创建导出参数
     * @param inputStream   模板文件输入流
     * @param scanAllsheet  是否扫描所有工作表
     * @param sheetName     工作表名称数组
     * @throws IOException  IO异常
     */
    public FwPlusTemplateExportParams(InputStream inputStream, boolean scanAllsheet, String... sheetName) throws IOException {
        super(inputStream,scanAllsheet,sheetName);
    }

    /**
     * 构造函数，通过输入流创建导出参数
     * @param inputStream   模板文件输入流
     * @param sheetNum      工作表编号数组
     * @throws IOException  IO异常
     */
    public FwPlusTemplateExportParams(InputStream inputStream, Integer... sheetNum) throws IOException {
        super(inputStream,sheetNum);
    }

    /**
     * 构造函数，通过输入流创建导出参数
     * @param inputStream   模板文件输入流
     * @param sheetName     工作表名称
     * @param sheetNum      工作表编号数组
     * @throws IOException  IO异常
     */
    public FwPlusTemplateExportParams(InputStream inputStream, String sheetName, Integer... sheetNum) throws IOException {
        super(inputStream,sheetName,sheetNum);
    }
}

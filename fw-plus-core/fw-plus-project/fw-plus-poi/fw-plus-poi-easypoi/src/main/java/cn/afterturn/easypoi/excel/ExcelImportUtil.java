package cn.afterturn.easypoi.excel;

import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.excel.imports.FwPlusExcelImportService;
import cn.afterturn.easypoi.excel.imports.sax.SaxReadExcel;
import cn.afterturn.easypoi.exception.excel.ExcelImportException;
import cn.afterturn.easypoi.handler.inter.IReadHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;

@Slf4j
public class ExcelImportUtil {

    /**
     * 私有构造函数，防止实例化工具类
     */
    private ExcelImportUtil() {
    }

    /**
     * 导入Excel数据到指定的POJO类列表中
     *
     * @param file Excel文件对象，用于读取Excel数据
     * @param pojoClass POJO类的Class对象，用于指定返回列表中对象的类型
     * @param params 导入参数对象，用于控制导入行为
     * @return 返回一个包含Excel数据的POJO类列表
     *
     * 此方法负责打开Excel文件，并使用ExcelImportService类读取文件内容，
     * 将其转换为指定POJO类的列表对象如果在读取或转换过程中发生错误，
     * 将抛出ExcelImportException异常
     */
    public static <T> List<T> importExcel(File file, Class<?> pojoClass, ImportParams params) {
        FileInputStream in = null;
        List data;
        try {
            // 打开文件输入流
            in = new FileInputStream(file);
            // 使用ExcelImportService类导入Excel数据，并转换为指定POJO类的列表
            data = (new FwPlusExcelImportService()).importExcelByIs(in, pojoClass, params, false).getList();
        } catch (ExcelImportException e) {
            // 处理Excel导入异常
            throw new ExcelImportException(e.getType(), e);
        } catch (Exception e) {
            // 处理其他异常
            log.error(e.getMessage(), e);
            throw new ExcelImportException(e.getMessage(), e);
        } finally {
            // 关闭文件输入流
            IOUtils.closeQuietly(in);
        }

        return data;
    }

    /**
     * 通过输入流导入Excel数据到指定的POJO类列表中
     * 此方法使用了泛型，以适应不同类型的POJO类
     *
     * @param inputstream Excel文件的输入流，用于读取Excel文件内容
     * @param pojoClass POJO类的Class对象，用于指定导入数据的目标类型
     * @param params 导入参数对象，包含导入过程中的相关参数，如需要导入的 sheet 索引或名称
     * @return 返回一个包含导入数据的POJO类列表，列表中的具体类型由调用时指定的pojoClass决定
     * @throws Exception 如果导入过程中发生错误，则抛出异常
     */
    public static <T> List<T> importExcel(InputStream inputstream, Class<?> pojoClass, ImportParams params) throws Exception {
        // 使用ExcelImportService类的importExcelByIs方法处理Excel数据导入
        // 这里不直接操作具体业务逻辑，而是调用服务类方法，便于维护和扩展
        return (new FwPlusExcelImportService()).importExcelByIs(inputstream, pojoClass, params, false).getList();
    }

    /**
     * 用于导入Excel文件中的数据到指定的JavaBean列表中
     * 该方法支持更复杂的导入参数设置，适用于需要高级配置的Excel导入场景
     *
     * @param inputstream Excel文件的输入流，用于读取Excel文件数据
     * @param pojoClass JavaBean类的Class对象，用于指定Excel数据应映射到的JavaBean类型
     * @param params 导入参数对象，包含导入Excel时的各种配置和选项
     * @return 返回一个ExcelImportResult对象，包含导入结果和可能的错误信息
     *
     * 注意：该方法由ExcelImportService类的具体实现来执行Excel数据的读取和解析
     */
    public static <T> ExcelImportResult<T> importExcelMore(InputStream inputstream, Class<?> pojoClass, ImportParams params) throws Exception {
        return (new FwPlusExcelImportService()).importExcelByIs(inputstream, pojoClass, params, true);
    }

    /**
     * 导入Excel文件中的数据到指定的JavaBean列表中
     * 该方法用于处理Excel文件的导入，可以解析Excel文件中的数据，并将其映射到指定的JavaBean类中
     *
     * @param file Excel文件，必须是.xlsx格式
     * @param pojoClass JavaBean类的Class对象，用于映射Excel中的数据
     * @param params 导入参数，包含了一些导入的配置信息，如是否需要验证数据等
     * @return 返回一个ExcelImportResult对象，包含导入的数据和一些导入过程中的信息
     * @throws ExcelImportException 当导入过程中发生错误时抛出此异常
     */
    public static <T> ExcelImportResult<T> importExcelMore(File file, Class<?> pojoClass, ImportParams params) {
        FileInputStream in = null;

        ExcelImportResult result;
        try {
            // 创建文件输入流，用于读取Excel文件
            in = new FileInputStream(file);
            // 使用ExcelImportService类的importExcelByIs方法来导入Excel数据
            result = (new FwPlusExcelImportService()).importExcelByIs(in, pojoClass, params, true);
        } catch (ExcelImportException e) {
            // 当发生Excel导入异常时，重新抛出该异常
            throw new ExcelImportException(e.getType(), e);
        } catch (Exception e) {
            // 当发生其他异常时，记录错误日志，并抛出ExcelImportException异常
            log.error(e.getMessage(), e);
            throw new ExcelImportException(e.getMessage(), e);
        } finally {
            // 关闭文件输入流，避免资源泄露
            IOUtils.closeQuietly(in);
        }

        // 返回导入的结果
        return result;
    }

    /**
     * 使用SAX方式导入Excel文件
     * 该方法通过SAX解析器读取Excel文件，适用于处理大型Excel文件，以减少内存消耗
     *
     * @param inputstream Excel文件的输入流，提供Excel文件数据
     * @param pojoClass 数据模型类的Class对象，用于映射Excel数据到程序中的数据模型
     * @param params 导入参数对象，包含导入过程中的相关参数，如需导入的 sheet 等
     * @param handler 实现了IReadHandler接口的处理对象，用于自定义数据处理逻辑
     */
    public static void importExcelBySax(InputStream inputstream, Class<?> pojoClass, ImportParams params, IReadHandler handler) {
        // 创建SaxReadExcel实例并调用其readExcel方法进行Excel文件的解析和处理
        (new SaxReadExcel()).readExcel(inputstream, pojoClass, params, handler);
    }
}

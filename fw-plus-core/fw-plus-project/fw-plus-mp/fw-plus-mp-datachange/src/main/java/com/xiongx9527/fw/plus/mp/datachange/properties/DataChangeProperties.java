package com.xiongx9527.fw.plus.mp.datachange.properties;
import com.xiongx9527.fw.plus.mp.datachange.DataChangeHelper;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据变更监听的属性配置类
 */
@Data
public class DataChangeProperties {
    /**
     * 修改单个entity的方法的默认值
     */
    private static final String[] defaultSingleEntityUpdateMethods = new String[]{
            "updateById",
            "updateColumnsById",
            "updateAllColumnsById"};
    /**
     * 根据多个entity修改的方法的默认值
     */
    private static final String[] defaultMultipelEntityUpdateMethods = new String[]{
            "batchUpdateByIds",
            "batchUpdateColumnsByIds",
            "batchUpdateByIdsInternal",
            "batchUpdateAllColumnsByIdsInternal",
            "batchUpdateColumnsByIdsInternal"};

    /**
     * 新增单个entity的方法的默认值
     */
    private static final String[] defaultSingleEntityInsertMethods = new String[]{
            "insert"};

    /**
     * 根据多个entity新增的方法的默认值
     */
    private static final String[] defaultMultipelEntityInsertMethods = new String[]{
            "batchInsert",
            "batchInsertInternal"};

    /**
     * 不依靠entity的修改的方法的默认值
     */
    private static final String[] defaultNoneEntityUpdateMethods = new String[]{"update"};

    /**
     * 物理删除单个entity的方法的默认值
     */
    private static final String[] defaultSingleEntityPhysicalDeleteMethods = new String[]{
            "physicalDeleteById"};

    /**
     * 物理删除多个entity的方法的默认值
     */
    private static final String[] defaultMultipelEntityPhysicalDeleteMethods = new String[]{
            "physicalDeleteBatchIds"};

    /**
     * 不依靠entity的物理删除的方法的默认值
     */
    private static final String[] defaultNoneEntityPhysicalDeleteMethods = new String[]{
            "physicalDeleteByMap",
            "physicalDelete"};

    /**
     * 逻辑删除单个entity的方法的默认值
     */
    private static final String[] defaultSingleEntityLogicDeleteMethods = new String[]{
            "deleteById"};

    /**
     * 逻辑删除多个entity的方法的默认值
     */
    private static final String[] defaultMultipelEntityLogicDeleteMethods = new String[]{
            "deleteBatchIds",
            "batchDeleteById"};

    /**
     * 不依靠entity的逻辑删除的方法的默认值
     */
    private static final String[] defaultNoneEntityLogicDeleteMethods = new String[]{
            "deleteByMap",
            "delete"};


    /**
     * 要忽略对比的字段名称的默认值
     */
    private static final String[] defaultIgnoreFields = new String[]{
            "id",
            "deleteFlag",
            "createTime",
            "createUser",
            "updateTime",
            "updateUser",
            "deleteTime",
            "deleteUser"};

    /**
     * 修改单个entity的方法
     */
    List<String> singleEntityUpdateMethods = new ArrayList<>(Arrays.asList(defaultSingleEntityUpdateMethods));

    /**
     * 根据多个entity修改的方法
     */
    List<String> multipelEntityUpdateMethods = new ArrayList<>(Arrays.asList(defaultMultipelEntityUpdateMethods));

    /**
     * 修改单个entity的方法
     */
    List<String> singleEntityInsertMethods = new ArrayList<>(Arrays.asList(defaultSingleEntityInsertMethods));

    /**
     * 根据多个entity修改的方法
     */
    List<String> multipelEntityInsertMethods = new ArrayList<>(Arrays.asList(defaultMultipelEntityInsertMethods));


    /**
     * 不依靠entity的修改的方法
     */
    List<String> noneEntityUpdateMethods = new ArrayList<>(Arrays.asList(defaultNoneEntityUpdateMethods));

    /**
     * 物理删除单个entity的方法
     */
    List<String> singleEntityPhysicalDeleteMethods = new ArrayList<>(Arrays.asList(defaultSingleEntityPhysicalDeleteMethods));

    /**
     * 根据多个entity物理删除的方法
     */
    List<String> sultipelEntityPhysicalDeleteMethods = new ArrayList<>(Arrays.asList(defaultMultipelEntityPhysicalDeleteMethods));

    /**
     * 不依靠entity的逻辑删除的方法
     */
    List<String> noneEntityPhysicalDeleteMethods = new ArrayList<>(Arrays.asList(defaultMultipelEntityPhysicalDeleteMethods));


    /**
     * 逻辑删除单个entity的方法
     */
    List<String> singleEntityLogicDeleteMethods = new ArrayList<>(Arrays.asList(defaultSingleEntityLogicDeleteMethods));

    /**
     * 根据多个entity逻辑删除的方法
     */
    List<String> sultipelEntityLogicDeleteMethods = new ArrayList<>(Arrays.asList(defaultMultipelEntityLogicDeleteMethods));

    /**
     * 不依靠entity的逻辑删除的方法
     */
    List<String> noneEntityLogicDeleteMethods = new ArrayList<>(Arrays.asList(defaultNoneEntityLogicDeleteMethods));


    /**
     * 要忽略对比的字段名称
     */
    List<String> ignoreFields = new ArrayList<>(Arrays.asList(defaultIgnoreFields));

    /**
     * 需要监听的字段 ,key 为表名，value 为表字段配置
     */
    private Map<String, DataChangeTable> tableFields;


    /**
     * 表字段配置
     */
    @Data
    public static class DataChangeTable {
        // 表名
        private String tableName;
        // 名称字段
        private String nameField;
        // 备注
        private String insertRemarks;
        // 变更类型
        private List<String> dataChangeTypes = new ArrayList<>();
        // 变更字段
        private Map<String,DataChangeField> fields;
        // 变更处理器
        private String dataChangeHandlerBeanName;
    }

    /**
     * 变更字段配置
     */
    @Data
    public static class DataChangeField {
        // 字段名
        private String fieldName;
        // 备注
        private String remarks;
        // 格式化
        private Class<? extends DataChangeHelper.DataChangeFormat> formatClass;
        // 日期格式
        private String dateFormat;
        // 枚举
        private Class<?> enumClass;
        // 排序
        private Integer sort;
        // 变更类型
        private FieldFormat bef;
        // 变更类型
        private FieldFormat after;
    }

    /**
     * 字段格式化
     */
    @Data
    public static class FieldFormat {
        // 值
        private String value;
        // 值类型
        private String dateFormat;
        // 枚举
        private Class<?> enumClass;
    }

}

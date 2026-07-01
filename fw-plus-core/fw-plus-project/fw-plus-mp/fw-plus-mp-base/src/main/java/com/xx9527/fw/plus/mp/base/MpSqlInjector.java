package com.xx9527.fw.plus.mp.base;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.SelectList;
import org.apache.commons.collections4.ListUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MpSqlInjector extends DefaultSqlInjector {

    /**
     * SQL注入器实例，用于处理SQL注入相关的操作
     *
     * @param sqlInjector SQL注入器对象，负责SQL语句的注入和处理
     */
    protected AbstractSqlInjector sqlInjector;


    /**
     * 默认构造函数，创建一个空的MpSqlInjector实例
     */
    public MpSqlInjector() {
    }

    /**
     * 带参数的构造函数，用于初始化MpSqlInjector实例
     *
     * @param sqlInjector SQL注入器实例，用于处理SQL相关的操作
     */
    public MpSqlInjector(ISqlInjector sqlInjector) {
        // 检查传入的sqlInjector是否非空且是AbstractSqlInjector的实例
        if (Objects.nonNull(sqlInjector) && sqlInjector instanceof AbstractSqlInjector) {
            this.sqlInjector = (AbstractSqlInjector)sqlInjector;
        }
    }


    /**
     * 获取Mapper类的方法列表
     *
     * @param mapperClass Mapper类的Class对象，用于获取该类中的方法信息
     * @return 返回一个包含多个AbstractMethod对象的列表，表示Mapper类中的方法
     */
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        // 获取父类方法列表，并处理为空的情况
        List<AbstractMethod> list = ListUtils.emptyIfNull(super.getMethodList(mapperClass));

        // 如果SqlInjector不为空，则获取SqlInjector中的方法列表，并进行过滤后添加到当前列表中
        if (null != sqlInjector) {
            list.addAll(methodFilter(ListUtils.emptyIfNull(sqlInjector.getMethodList(mapperClass))));
        }

        // 添加自定义方法
        return list;
    }

    /**
     * 方法过滤器
     * 该方法用于过滤给定列表中的方法对象，移除那些名称在指定列表中且属于特定包的方法对象
     * 此外，还会向列表中添加一些特定的内部类对象，以供后续处理使用
     *
     * @param list 待过滤的AbstractMethod对象列表
     * @return 过滤后的AbstractMethod对象列表
     */
    private List<AbstractMethod> methodFilter(List<AbstractMethod> list) {
        // 获取SelectList类所在包的名称，用于后续的包名比较
        String packageStr = SelectList.class.getPackage().getName();

        // 定义一个需要过滤的方法名称列表，包含了一系列特定的方法名
        List<String> methodList = Arrays.asList("Update", "Delete", "SelectOne", "SelectCount", "SelectMaps",
                "SelectMapsPage", "SelectObjs", "SelectList", "SelectPage",
                "DeleteInternal","DeleteByMapInternal","DeleteByIdInternal",
                "PhysicalDelete","PhysicalDeleteById","PhysicalDeleteByMap","PhysicalDeleteBatchIds",
                "SelectOneByMap","SelectByIdIgnoreDelFlag","BatchUpdateByIdsInternal","BatchInsertInternal",
                "BatchDeleteByIdsInternal","UpdateAllColumnsById","UpdateColumnsById",
                "BatchUpdateColumnsByIdsInternal","BatchUpdateAllColumnsByIdsInternal");

        /**
        // 向列表中添加一系列特定的内部类对象，这些对象代表了不同的方法
        list.addAll((Collection) Stream.of(
                new BatchUpdateByIdsInternal(),
                new BatchInsertInternal(),
                new BatchDeleteByIdsInternal(),
                new UpdateAllColumnsById(),
                new UpdateColumnsById(),
                new BatchUpdateColumnsByIdsInternal(),
                new BatchUpdateAllColumnsByIdsInternal()).collect(Collectors.toList()));
         **/

        // 移除列表中那些名称在methodList中且包名与packageStr相匹配的方法对象
        list.removeIf((i) -> methodList.contains(i.getClass().getSimpleName()) && Objects.equals(packageStr, i.getClass().getPackage().getName()));

        // 返回过滤后的列表
        return list;
    }
}

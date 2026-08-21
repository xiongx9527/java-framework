package com.github.yulichang.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.AbstractSqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.methods.DeleteBatchByIds;
import com.baomidou.mybatisplus.core.injector.methods.DeleteById;
import com.baomidou.mybatisplus.core.injector.methods.DeleteByMap;
import com.baomidou.mybatisplus.core.injector.methods.Insert;
import com.baomidou.mybatisplus.core.injector.methods.SelectBatchByIds;
import com.baomidou.mybatisplus.core.injector.methods.SelectById;
import com.baomidou.mybatisplus.core.injector.methods.SelectByMap;
import com.baomidou.mybatisplus.core.injector.methods.SelectList;
import com.baomidou.mybatisplus.core.injector.methods.UpdateById;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.github.yulichang.adapter.base.tookit.VersionUtils;
import com.github.yulichang.adapter.v3431.AbstractMethodV3431;
import com.github.yulichang.method.DeleteJoin;
import com.github.yulichang.method.SelectJoinCount;
import com.github.yulichang.method.SelectJoinList;
import com.github.yulichang.method.SelectJoinOne;
import com.github.yulichang.method.SelectJoinPage;
import com.github.yulichang.method.SqlMethod;
import com.github.yulichang.method.UpdateJoin;
import com.github.yulichang.method.UpdateJoinAndNull;
import com.github.yulichang.method.mp.Delete;
import com.github.yulichang.method.mp.SelectCount;
import com.github.yulichang.method.mp.SelectMaps;
import com.github.yulichang.method.mp.SelectMapsPage;
import com.github.yulichang.method.mp.SelectObjs;
import com.github.yulichang.method.mp.SelectOne;
import com.github.yulichang.method.mp.SelectPage;
import com.github.yulichang.method.mp.Update;
import com.github.yulichang.toolkit.MPJTableMapperHelper;
import com.github.yulichang.toolkit.ReflectionKit;
import com.github.yulichang.toolkit.TableHelper;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.xiongx9527.fw.plus.mp.base.MpSqlInjector;
import lombok.Generated;
import org.apache.commons.collections4.ListUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

public class MPJSqlInjector extends MpSqlInjector {

    /**
     * 默认构造函数，初始化MPJSqlInjector实例
     */
    public MPJSqlInjector() {
        super();
    }

    /**
     * 带参数的构造函数，使用指定的SQL注入器初始化MPJSqlInjector实例
     * @param sqlInjector SQL注入器接口实现，用于处理SQL注入相关的操作
     */
    public MPJSqlInjector(ISqlInjector sqlInjector) {
        super(sqlInjector);
    }


    /**
     * 获取方法列表。
     *
     * @param configuration 配置信息
     * @param mapperClass   Mapper接口类
     * @param tableInfo     表信息
     * @return 方法列表
     */
    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        return Objects.nonNull(this.sqlInjector) ? this.methodFilter(this.sqlInjector.getMethodList(mapperClass, tableInfo)) :
                this.methodFilter(super.getMethodList(configuration, mapperClass, tableInfo));
    }

    /**
     * 对方法列表进行过滤处理，并添加自定义的方法（如Wrapper相关方法、Join相关方法）。
     *
     * @param list 原始方法列表
     * @return 过滤并增强后的方法列表
     */
    private List<AbstractMethod> methodFilter(List<AbstractMethod> list) {
        // 定义需要移除的方法名集合
        String packageStr = SelectList.class.getPackage().getName();
        List<String> methodList = Arrays.asList("Update", "Delete", "SelectOne", "SelectCount", "SelectMaps",
                "SelectMapsPage", "SelectObjs", "SelectList", "SelectPage",
                "DeleteInternal","DeleteByMapInternal","DeleteByIdInternal",
                "PhysicalDelete","PhysicalDeleteById","PhysicalDeleteByMap","PhysicalDeleteBatchIds",
                "SelectOneByMap","SelectByIdIgnoreDelFlag","BatchUpdateByIdsInternal","BatchInsertInternal",
                "BatchDeleteByIdsInternal","UpdateAllColumnsById","UpdateColumnsById",
                "BatchUpdateColumnsByIdsInternal","BatchUpdateAllColumnsByIdsInternal");
        // 移除指定包下特定名称的方法
        list.removeIf((i) -> methodList.contains(i.getClass().getSimpleName()) && Objects.equals(packageStr, i.getClass().getPackage().getName()));
        // 添加自定义方法
        this.addAll(list, this.getWrapperMethod());
        this.addAll(list, this.getJoinMethod());
        return list;
    }

    /**
     * 根据MyBatis Plus版本获取Join相关的扩展方法。
     *
     * @return Join相关的方法列表
     */
    private List<AbstractMethod> getJoinMethod() {
        List<AbstractMethod> list = new ArrayList();
        if (VersionUtils.compare(VersionUtils.getVersion(), "3.5.0") >= 0) {
            list.add(new DeleteJoin(SqlMethod.DELETE_JOIN.getMethod()));
            list.add(new UpdateJoin(SqlMethod.UPDATE_JOIN.getMethod()));
            list.add(new UpdateJoinAndNull(SqlMethod.UPDATE_JOIN_AND_NULL.getMethod()));
            list.add(new SelectJoinCount(SqlMethod.SELECT_JOIN_COUNT.getMethod()));
            list.add(new SelectJoinOne(SqlMethod.SELECT_JOIN_ONE.getMethod()));
            list.add(new SelectJoinList(SqlMethod.SELECT_JOIN_LIST.getMethod()));
            list.add(new SelectJoinPage(SqlMethod.SELECT_JOIN_PAGE.getMethod()));
        } else {
            list.add(new DeleteJoin());
            list.add(new UpdateJoin());
            list.add(new UpdateJoinAndNull());
            list.add(new SelectJoinCount());
            list.add(new SelectJoinOne());
            list.add(new SelectJoinList());
            list.add(new SelectJoinPage());
        }

        return list;
    }

    /**
     * 获取Wrapper相关的扩展方法。
     *
     * @return Wrapper相关的方法列表
     */
    private List<AbstractMethod> getWrapperMethod() {
        List<AbstractMethod> list = new ArrayList();
        list.add(new Delete());
        list.add(new SelectOne());
        list.add(new SelectCount());
        list.add(new SelectMaps());
        list.add(new SelectMapsPage());
        list.add(new SelectObjs());
        list.add(new com.github.yulichang.method.mp.SelectList());
        list.add(new SelectPage());
        list.add(new Update());
        return list;
    }

    /**
     * 将addList中的方法加入到source中，避免重复添加相同简单类名的方法。
     *
     * @param source   源方法列表
     * @param addList  待添加的方法列表
     */
    private void addAll(List<AbstractMethod> source, List<AbstractMethod> addList) {
        for(AbstractMethod method : addList) {
            if (source.stream().noneMatch((m) -> m.getClass().getSimpleName().equals(method.getClass().getSimpleName()))) {
                source.add(method);
            }
        }
    }

    /**
     * 注入检查逻辑，在构建Mapper时初始化模型类与表映射关系等辅助工具。
     *
     * @param builderAssistant Mapper构建助手
     * @param mapperClass      Mapper接口类
     */
    @Override
    public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        Class<?> modelClass = ReflectionKit.getSuperClassGenericType(mapperClass, Mapper.class, 0);
        super.inspectInject(builderAssistant, mapperClass);
        MPJTableMapperHelper.init(modelClass, mapperClass);
        Supplier<Class<?>> supplier = () -> {
            try {
                return this.extractModelClassOld(mapperClass);
            } catch (Throwable var3) {
                return null;
            }
        };
        TableHelper.init(modelClass, (Class)supplier.get());
    }

    /**
     * 提取旧版Mapper接口泛型参数中的实体类类型。
     *
     * @param mapperClass Mapper接口类
     * @return 实体类类型
     */
    protected Class<?> extractModelClassOld(Class<?> mapperClass) {
        Type[] types = mapperClass.getGenericInterfaces();
        ParameterizedType target = null;

        for(Type type : types) {
            if (type instanceof ParameterizedType) {
                Type[] typeArray = ((ParameterizedType)type).getActualTypeArguments();
                if (ArrayUtils.isNotEmpty(typeArray)) {
                    int var10 = typeArray.length;
                    byte var11 = 0;
                    if (var11 < var10) {
                        Type t = typeArray[var11];
                        if (!(t instanceof TypeVariable) && !(t instanceof WildcardType)) {
                            target = (ParameterizedType)type;
                        }
                    }
                }
                break;
            }
        }

        return target == null ? null : (Class)target.getActualTypeArguments()[0];
    }

    /**
     * 获取SQL注入器实例。
     *
     * @return SQL注入器对象
     */
    @Generated
    public AbstractSqlInjector getSqlInjector() {
        return this.sqlInjector;
    }



}

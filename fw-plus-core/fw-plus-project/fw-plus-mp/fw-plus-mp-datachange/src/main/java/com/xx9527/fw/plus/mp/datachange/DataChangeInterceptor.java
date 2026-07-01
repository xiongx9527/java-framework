package com.xx9527.fw.plus.mp.datachange;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.xx9527.fw.plus.mp.base.MpBaseEntity;
import com.xx9527.fw.plus.mp.base.MpBaseMapper;
import com.xx9527.fw.plus.mp.datachange.properties.ChangeDataRecord;
import com.xx9527.fw.plus.mp.datachange.properties.DataChangeProperties;
import com.xx9527.fw.plus.mp.datachange.properties.DataChangeProperties.DataChangeTable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.core.Ordered;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据变更监听拦截器
 * @author zengrx
 * @date 2024/12/25
 */
@Slf4j
@Intercepts(
        value = {
                    @Signature(
                           type = Executor.class, method = "update",args = {MappedStatement.class, Object.class}
                    )
        }
)
public class DataChangeInterceptor implements Interceptor, Ordered {
    /** 数据变更配置属性 */
    private final DataChangeProperties dataChangeProperties;

    /** 数据变更处理器映射表，用于存储不同数据类型对应的处理器 */
    private final Map<String,DataChangeHandler> dataChangeHandlerMap;

    /**
     * 数据变更拦截器构造函数
     *
     * @param dataChangeProperties 数据变更配置属性，包含拦截器的相关配置信息
     * @param dataChangeHandlerMap 数据变更处理器映射表，用于根据数据类型获取对应的处理器
     */
    public DataChangeInterceptor(DataChangeProperties dataChangeProperties,Map<String,DataChangeHandler> dataChangeHandlerMap) {
        this.dataChangeHandlerMap = dataChangeHandlerMap;
        this.dataChangeProperties = dataChangeProperties;
    }

    /**
     * 获取默认的数据变更处理器
     * 当未指定数据变更处理器时，此方法用于获取一个默认的处理器
     * 首先尝试从数据变更处理器映射中获取默认处理器，如果不存在，则根据数据变更属性中的表名配置来寻找合适的处理器
     * 如果仍然找不到合适的处理器，那么将返回null
     *
     * @return DataChangeHandler 默认的数据变更处理器，如果没有找到则返回null
     */
    private DataChangeHandler getDefaultDataChangeHandler() {
        // 尝试从映射中获取默认的数据变更处理器
        DataChangeHandler defaultDataChangeHandler = dataChangeHandlerMap.get(DataChangeHandler.DEFAULT_DATACHANGEHANDLER_BEAN_NAME);
        if (null == defaultDataChangeHandler) {
            // 如果默认处理器不存在，获取所有需要监控的表名集合
            Set<String> tableNames = MapUtils.emptyIfNull(dataChangeProperties.getTableFields()).values().stream().filter(e->StringUtils.isEmpty(e.getDataChangeHandlerBeanName()))
                    .map(e->e.getDataChangeHandlerBeanName()).collect(Collectors.toSet());
            // 遍历所有的数据变更处理器，寻找合适的默认处理器
            for (Map.Entry<String,DataChangeHandler> e: dataChangeHandlerMap.entrySet()) {
                // 如果当前处理器对应的表名在监控列表中，则跳过，寻找下一个
                if (tableNames.contains(e.getKey())) {
                    continue;
                }
                // 找到一个不在监控列表中的处理器，作为默认处理器
                defaultDataChangeHandler = e.getValue();
                break;
            }
        }
        // 返回默认的数据变更处理器
        return defaultDataChangeHandler;
    }

    /**
     * 根据名称获取数据变更处理器
     *
     * @param name 数据变更处理器的名称
     * @return 对应名称的数据变更处理器实例，如果不存在则返回null
     */
    private DataChangeHandler getDataChangeHandlerByName(String name) {
        // 从数据变更处理器映射中获取指定名称的处理器
        DataChangeHandler defaultDataChangeHandler = dataChangeHandlerMap.get(name);
        // 返回获取到的数据变更处理器
        return defaultDataChangeHandler;
    }

    /**
     * 根据表名获取数据变更处理器
     *
     * @param tableName 表名，用于查找对应的数据变更处理器
     * @return DataChangeHandler 数据变更处理器实例
     */
    private DataChangeHandler getDataChangeHandlerByTableName(String tableName) {
        // 获取配置中的表字段映射，如果为null则返回一个空映射
        Map<String,DataChangeTable> tableFieldsMap = MapUtils.emptyIfNull(dataChangeProperties.getTableFields());

        // 根据表名从映射中获取数据变更表配置
        DataChangeTable dataChangeTable = tableFieldsMap.get(tableName);
        if(null == dataChangeTable) {
            return null;
        }

        // 初始化数据变更处理器为null
        DataChangeHandler dataChangeHandler = null;

        // 检查数据变更表配置中的处理器Bean名称是否为空
        if (StringUtils.isEmpty(dataChangeTable.getDataChangeHandlerBeanName())) {
            // 如果为空，则获取默认的数据变更处理器
            dataChangeHandler = getDefaultDataChangeHandler();
        } else {
            // 如果不为空，则根据配置的Bean名称获取特定的数据变更处理器
            dataChangeHandler = getDataChangeHandlerByName(dataChangeTable.getDataChangeHandlerBeanName());
        }
        // 如果数据变更处理器为空，则获取默认的数据变更处理器
        if (ObjectUtils.isEmpty(dataChangeHandler)) {
            dataChangeHandler = getDefaultDataChangeHandler();
        }
        // 返回获取到的数据变更处理器实例
        return dataChangeHandler;
    }


    /**
     * 拦截器方法，用于在方法调用前后执行自定义操作
     * 主要功能包括：
     * 1. 获取当前跟踪ID作为批处理编号
     * 2. 尝试处理数据变更并记录
     * 3. 在出现异常时进行错误日志记录，并执行批处理后的操作
     * 4. 确保最终执行原始方法调用
     * 5. 如果有数据变更记录，则处理这些记录
     *
     * @param invocation 被拦截的方法调用对象，包含方法调用的相关信息
     * @return 执行被拦截方法后的返回值
     * @throws Throwable 如果被拦截的方法调用或数据处理过程中抛出异常，则重新抛出
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 初始化批处理编号
        String batchNo = null;
        DataChangeHandler defaultDataChangeHandler = getDefaultDataChangeHandler();
        if (ObjectUtils.isNotEmpty(defaultDataChangeHandler)) {
            batchNo = defaultDataChangeHandler.getTraceId();
        }

        Object proceedObj = null;
        List<ChangeDataRecord> changeDataRecords = null;

        // 尝试处理数据变更并记录，同时处理可能的异常
        try {
            changeDataRecords = handler(invocation, batchNo);
        }
        catch (Throwable e) {
            log.error(e.getMessage(), e);
            try {
                getDefaultDataChangeHandler().afterHandler(batchNo, e);
            }
            catch (Throwable e1) {
                log.error(e1.getMessage(), e1);
            }
        }
        finally {
            // 确保最终执行原始方法调用
            proceedObj = invocation.proceed();
        }

        // 如果有数据变更记录，则处理这些记录
        if(!CollectionUtils.isEmpty(changeDataRecords)) {
            Map<String, List<ChangeDataRecord>> changeDataRecordsMap = changeDataRecords.stream().collect(Collectors.groupingBy(ChangeDataRecord::getObjectName));
            Map<DataChangeHandler , List<ChangeDataRecord>> map = changeDataRecordsMap.entrySet().stream()
                    .filter(e->{
                        return null != getDataChangeHandlerByTableName(e.getKey());
                    }).collect(Collectors.toMap(e->{
                        return getDataChangeHandlerByTableName(e.getKey());
                    }, e->e.getValue()));

            if (MapUtils.isNotEmpty(map)) {
                for (Map.Entry<DataChangeHandler , List<ChangeDataRecord>> e: map.entrySet()) {
                    try {
                        e.getKey().handler(e.getValue());
                    } catch (Throwable ee) {
                        log.error(String.format("%s handler error",e.getKey()), ee);
                    }
                }
            }
        }

        return proceedObj;
    }


    /**
     * 处理数据变更事件
     *
     * @param invocation 当前调用信息
     * @param batchNo 批处理编号
     * @return 返回处理后的数据变更记录列表，如果没有变更则返回null
     */
    private List<ChangeDataRecord> handler(Invocation invocation, String batchNo) {
        // 日志输出当前处理的batchNo
        log.debug("invocation batchNo:{} ", batchNo);
        DataChangeHandler defaultDataChangeHandler = null;

        // 获取目标对象
        Object target = invocation.getTarget();
        // 如果目标对象不是Executor实例，直接返回null
        if(!(target instanceof Executor)) {
            return null;
        }

        // 获取方法参数
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 获取SQL命令类型
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        String commandName = sqlCommandType.name();
        // 日志输出当前SQL命令类型
        log.debug("invocation args commandName:{} ", commandName);

        // 获取Mapper方法名
        String mapperMethod = ms.getId().substring(ms.getId().lastIndexOf(".") + 1);
        // 日志输出当前Mapper方法名
        log.debug("invocation args mapperMethod:{} ", mapperMethod);

        // 根据SQL命令类型进行不同处理
        // 修改操作
        if(SqlCommandType.UPDATE == sqlCommandType) {
            // 获取Mapper实例
            MpBaseMapper<?> mapper = DataChangeHelper.getMapper(ms.getId());
            if(null == mapper) {
                return null;
            }

            // 获取表字段配置
            Map<String, DataChangeTable> tableFields = dataChangeProperties.getTableFields();
            if(MapUtils.isEmpty(tableFields)) {
                return null;
            }

            // 根据entity单个修改
            if(dataChangeProperties.getSingleEntityUpdateMethods().stream().anyMatch(m -> m.equalsIgnoreCase(mapperMethod))) {
                // 获取Entity实例
                MpBaseEntity entity = DataChangeHelper.getEntity(parameter);
                if(null == entity) {
                    return null;
                }

                // 获取表名
                String tableName = entity.getClass().getAnnotation(TableName.class).value();
                if(StringUtils.isBlank(tableName)) {
                    return null;
                }

                defaultDataChangeHandler = getDataChangeHandlerByTableName(tableName);
                if(null == defaultDataChangeHandler) {
                    return null;
                }

                // 获取表配置
                DataChangeTable dataChangeTable = tableFields.get(tableName);
                if(null == dataChangeTable) {
                    return null;
                }

                // 检查是否需要处理当前命令类型
                if(!dataChangeTable.getDataChangeTypes().contains(commandName)) {
                    return null;
                }

                // 创建更新记录
                UpdateRecord record = new UpdateRecord();
                record.setUpdateColums(DataChangeHelper.getUpdateColums(parameter, entity, dataChangeProperties.getIgnoreFields()));
                record.setNewEntity(entity);

                // 返回更新记录
                return defaultDataChangeHandler.createUpdateRecord(dataChangeTable, batchNo, mapper, tableName, Collections.singletonList(record));
            }
            // 根据多个entity修改
            else if(dataChangeProperties.getMultipelEntityUpdateMethods().stream().anyMatch(m -> m.equalsIgnoreCase(mapperMethod))) {
                // 获取Entity列表
                List<MpBaseEntity> entitys = DataChangeHelper.getEntitys(parameter);
                if(CollectionUtils.isEmpty(entitys)) {
                    return null;
                }

                // 获取表名
                String tableName = entitys.get(0).getClass().getAnnotation(TableName.class).value();
                if(StringUtils.isBlank(tableName)) {
                    return null;
                }

                defaultDataChangeHandler = getDataChangeHandlerByTableName(tableName);
                if(null == defaultDataChangeHandler) {
                    return null;
                }

                // 获取表配置
                DataChangeTable dataChangeTable = tableFields.get(tableName);
                if(null == dataChangeTable) {
                    return null;
                }

                // 检查是否需要处理当前命令类型
                if(!dataChangeTable.getDataChangeTypes().contains(commandName)) {
                    return null;
                }

                // 创建更新记录列表
                List<UpdateRecord> records = new ArrayList<>(entitys.size());
                for (MpBaseEntity entity : entitys) {
                    UpdateRecord record = new UpdateRecord();
                    record.setUpdateColums(DataChangeHelper.getUpdateColums(parameter, entity, dataChangeProperties.getIgnoreFields()));
                    record.setNewEntity(entity);
                    records.add(record);
                }

                // 返回更新记录列表
                return defaultDataChangeHandler.createUpdateRecord(dataChangeTable, batchNo, mapper, tableName, records);
            }
            // 不依靠entity的修改
            else if(dataChangeProperties.getNoneEntityUpdateMethods().stream().anyMatch(m -> m.equalsIgnoreCase(mapperMethod))) {
                // 日志输出进入不依靠entity的修改处理
                log.debug("DataChangeInterceptor: into the update handle method of mapper");

                // 解析参数
                ParamMap parameterMap = (ParamMap) parameter;
                LambdaUpdateWrapper ew = (LambdaUpdateWrapper) parameterMap.get(Constants.WRAPPER);
                if(null == ew) {
                    return null;
                }

                // 检查参数有效性
                Map paramNameValuePairs = ew.getParamNameValuePairs();
                if(MapUtils.isEmpty(paramNameValuePairs)) {
                    return null;
                }
                String sqlSet = ew.getSqlSet();
                if(StringUtils.isBlank(sqlSet)) {
                    return null;
                }
                String[] sqlSetArray = sqlSet.split(StrUtil.COMMA);
                if(sqlSetArray.length == 0) {
                    return null;
                }

                // 检查本次修改是否涉及监听字段
                MpBaseEntity entity = DataChangeHelper.getEntity(parameter);
                if(Objects.isNull(entity)) {
                    entity = mapper.newEntity();
                    if(Objects.isNull(entity)) {
                        log.error("DataChangeInterceptor: the update method is not supported, please use other update!!!!!!");
                        return null;
                    }
                }
                String tableName = entity.getClass().getAnnotation(TableName.class).value();
                if(StringUtils.isBlank(tableName)) {
                    return null;
                }
                DataChangeTable dataChangeTable = tableFields.get(tableName);
                if(null == dataChangeTable) {
                    return null;
                }

                defaultDataChangeHandler = getDataChangeHandlerByTableName(tableName);
                if(null == defaultDataChangeHandler) {
                    return null;
                }

                if(!dataChangeTable.getDataChangeTypes().contains(commandName)) {
                    return null;
                }

                // 处理修改字段
                Set<String> fieldSet = new HashSet<>();
                Map<String, DataChangeProperties.DataChangeField> fields = dataChangeTable.getFields();
                Map<String, Object> fieldValueMap = new HashMap<>();
                Map<String, Object> tmpFieldValueMap = BeanMap.create(entity);
                for (Map.Entry<String, Object> e : tmpFieldValueMap.entrySet()) {
                    if(fields.containsKey(e.getKey())) {
                        fieldSet.add(e.getKey());
                        fieldValueMap.put(e.getKey(), e.getValue());
                    }
                }
                for (String s : sqlSetArray) {
                    int i = s.indexOf("=");
                    String field = DataChangeHelper.lineToHump(s.substring(0, i));
                    if(fields.containsKey(field)) {
                        fieldSet.add(field);
                    }
                    else {
                        continue;
                    }
                    int i1 = s.lastIndexOf(".");
                    String key = s.substring(i1 + 1, s.length() - 1);
                    Object o = paramNameValuePairs.get(key);
                    fieldValueMap.put(field, o);
                }

                // 如果没有修改监听字段，直接返回null
                if(CollectionUtils.isEmpty(fieldSet)) {
                    return Collections.emptyList();
                }

                // 查询受影响的记录
                List list = mapper.selectList(new MyLambdaQueryWrapper(ew));
                if(CollectionUtils.isEmpty(list)) {
                    return null;
                }

                // 返回更新记录
                return defaultDataChangeHandler.createUpdateRecord(batchNo, tableName, dataChangeTable, fields, fieldSet, fieldValueMap, list);
            }
        }
        // 插入操作
        else if(SqlCommandType.INSERT == sqlCommandType) {
            // 获取Mapper实例
            MpBaseMapper<?> mapper = DataChangeHelper.getMapper(ms.getId());
            if(null == mapper) {
                return null;
            }

            // 获取表字段配置
            Map<String, DataChangeTable> tableFields = dataChangeProperties.getTableFields();
            if(MapUtils.isEmpty(tableFields)) {
                return null;
            }

            try {
                // 单个插入
                if(dataChangeProperties.getSingleEntityInsertMethods().stream().anyMatch(m -> m.equalsIgnoreCase(mapperMethod))) {
                    // 获取Entity实例
                    MpBaseEntity entity = DataChangeHelper.getEntity(parameter);
                    if(null == entity) {
                        return null;
                    }

                    // 获取表名
                    String tableName = entity.getClass().getAnnotation(TableName.class).value();
                    if(StringUtils.isBlank(tableName)) {
                        return null;
                    }

                    defaultDataChangeHandler = getDataChangeHandlerByTableName(tableName);
                    if(null == defaultDataChangeHandler) {
                        return null;
                    }

                    // 设置Entity ID
                    if(null == entity.getId()) {
                        entity.setId(defaultDataChangeHandler.nextId());
                    }

                    // 获取表配置
                    DataChangeTable dataChangeTable = tableFields.get(tableName);
                    if(null == dataChangeTable) {
                        return null;
                    }

                    // 检查是否需要处理当前命令类型
                    if(!dataChangeTable.getDataChangeTypes().contains(commandName)) {
                        return null;
                    }

                    // 处理修改字段
                    Set<String> fieldSet = new HashSet<>();
                    Map<String, DataChangeProperties.DataChangeField> fields = dataChangeTable.getFields();
                    Map<String, Object> tmpFieldValueMap = BeanMap.create(entity);
                    for (Map.Entry<String, Object> e : tmpFieldValueMap.entrySet()) {
                        if(fields.containsKey(e.getKey())) {
                            fieldSet.add(e.getKey());
                        }
                    }
                    // 如果没有修改监听字段，直接返回null
                    if(CollectionUtils.isEmpty(fieldSet)) {
                        return Collections.emptyList();
                    }

                    // 返回插入记录
                    return defaultDataChangeHandler.createInsertRecord(dataChangeTable, batchNo, tableName, fieldSet ,new ArrayList<>(Collections.singletonList(entity)));
                }
                // 批量插入
                else if(dataChangeProperties.getMultipelEntityInsertMethods().stream().anyMatch(m -> m.equalsIgnoreCase(mapperMethod))) {
                    // 获取Entity列表
                    List<MpBaseEntity> entitys = DataChangeHelper.getEntitys(parameter);
                    if(CollectionUtils.isEmpty(entitys)) {
                        return null;
                    }

                    // 获取表名
                    String tableName = entitys.get(0).getClass().getAnnotation(TableName.class).value();
                    if(StringUtils.isBlank(tableName)) {
                        return null;
                    }

                    defaultDataChangeHandler = getDataChangeHandlerByTableName(tableName);
                    if(null == defaultDataChangeHandler) {
                        return null;
                    }

                    // 获取表配置
                    DataChangeTable dataChangeTable = tableFields.get(tableName);
                    if(null == dataChangeTable) {
                        return null;
                    }

                    // 检查是否需要处理当前命令类型
                    if(!dataChangeTable.getDataChangeTypes().contains(commandName)) {
                        return null;
                    }

                    // 设置Entity ID
                    for (MpBaseEntity entity: entitys) {
                        if(null == entity.getId()) {
                            entity.setId(defaultDataChangeHandler.nextId());
                        }
                    }

                    // 处理修改字段
                    Set<String> fieldSet = dataChangeTable.getFields().keySet();
                    // 如果没有修改监听字段，直接返回null
                    if(CollectionUtils.isEmpty(fieldSet)) {
                        return Collections.emptyList();
                    }

                    // 返回插入记录
                    return defaultDataChangeHandler.createInsertRecord(dataChangeTable, batchNo, tableName, fieldSet ,new ArrayList<>(entitys));
                }
            }
            catch (Throwable e) {
                // 日志输出异常信息
                log.error(e.getMessage(), e);
            }
        }
        // 删除操作
        else if(SqlCommandType.DELETE == sqlCommandType) {
            // TODO: 处理删除操作
        }

        // 默认返回null
        return null;
    }

    /**
     * 更新记录信息封装类
     * 用于封装实体更新操作所需的数据信息
     */
    @Data
    public static class UpdateRecord {
        /**
         * 新的实体对象
         * 包含更新后的实体数据
         */
        private MpBaseEntity newEntity;

        /**
         * 需要更新的列名集合
         * 指定哪些字段需要进行更新操作
         */
        private Collection<String> updateColums;
    }


    /**
     * 用于将LambdaUpdateWrapper持有的expression和paramNameValuePairs传递给LambdaQueryWrapper
     * 由于是protected方法,应该只能弄个子类来接收了
     */
    private final static class MyLambdaQueryWrapper<T> extends LambdaQueryWrapper<T> {

        public MyLambdaQueryWrapper(LambdaUpdateWrapper ew) {
            super.initNeed();
            super.expression = ew.getExpression();
            super.paramNameValuePairs = ew.getParamNameValuePairs();
        }
    }

    /**
     * 获取订单号
     *
     * @return 返回订单号，固定返回值为0
     */
    @Override
    public int getOrder() {
        return 0;
    }


}

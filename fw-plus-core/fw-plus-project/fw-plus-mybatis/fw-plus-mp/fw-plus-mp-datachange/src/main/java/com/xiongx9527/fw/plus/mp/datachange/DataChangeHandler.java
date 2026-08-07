package com.xiongx9527.fw.plus.mp.datachange;

import cn.hutool.core.util.NumberUtil;
import com.xiongx9527.fw.plus.common.util.OrikaUtils;
import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import com.xiongx9527.fw.plus.mp.base.MpBaseMapper;
import com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties;
import com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties.DataChangeField;
import com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties.DataChangeTable;
import com.xiongx9527.fw.plus.mp.datachange.properties.ChangeDataRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.util.CollectionUtils;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class DataChangeHandler {

    /**
     * 默认数据变更处理器Bean名称常量
     */
    public static final String DEFAULT_DATACHANGEHANDLER_BEAN_NAME = "DEFAULT_DATACHANGEHANDLER";

    /**
     * 更新操作类型常量
     */
    public static final String UPDATE = "UPDATE";

    /**
     * 插入操作类型常量
     */
    public static final String INSERT = "INSERT";

    /**
     * 逻辑删除操作类型常量
     */
    public static final String LOGIC_DELETE = "LOGIC_DELETE";

    /**
     * 物理删除操作类型常量
     */
    public static final String PHYSICAL_DELETE = "PHYSICAL_DELETE";


    /**
     * 获取当前请求的追踪ID
     *
     * 追踪ID用于在分布式系统中唯一标识一个请求，以便于跟踪和调试
     * 该方法通过调用TraceContext的静态方法traceId来获取当前上下文的追踪ID
     *
     * @return 当前请求的追踪ID如果无法获取到追踪ID，则返回空字符串
     */
    public String getTraceId() {
        return null;
    }

    /**
     * 获取用户的信息, 用于给变更记录赋值
     */
    public abstract UserInfo getUserInfo();

    /**
     * 获取枚举类的映射信息
     * @param clazz
     * @return
     */
    public abstract Map<Object, Object> enumToMap(Class<?> clazz);

    /**
     * 设置操作用户的信息
     * 该方法用于将当前操作用户的的信息设置到指定的实体对象中
     * 主要用于审计、日志记录等场景，以便跟踪用户操作
     *
     * @param entity 实体对象，通常是一个业务实体，该方法将用户信息设置到此实体中
     */
    public void setOperUserInfo(Object entity) {
        // 方法体为空，具体实现应根据业务需求来填充
    }

    /**
     * 处理操作
     * @param operationRecords
     */
    public abstract void handler(List<ChangeDataRecord> operationRecords);

    /**
     * 处理操作完成后的回调
     * 该方法主要用于在一系列处理操作完成后进行回调通知，无论操作是否成功
     * 如果操作中产生了异常，可以通过这个方法进行捕获和处理
     *
     * @param batchNo 批次号，表示处理操作的批次
     * @param e 异常对象，如果处理操作中产生了异常，则通过此参数传递
     */
    public void afterHandler(String batchNo, Throwable e) {

    }

    /**
     * 替换备注中的占位符
     * 该方法的目的是在给定的模板字符串中替换占位符
     * 占位符及其对应的值由参数params Map指定
     * 这种替换机制常用于生成动态消息或报告
     *
     * @param template 模板字符串，包含待替换的占位符
     * @param params 包含占位符及其对应值的Map
     *               Map的键是占位符的名称，值是占位符的实际值
     * @return 替换占位符后生成的最终字符串
     */
    public String replaceParams(final String template, Map<String, Object> params) {
        return template;
    }

    /**
     * 获取下一个ID
     *
     * 该方法用于生成或获取一个唯一的ID此方法当前返回null，表示尚未实现
     * 它应该在子类中被重写以提供实际的ID生成逻辑
     *
     * @return 下一个唯一的ID如果未实现或无法生成，则返回null
     */
    public String nextId() {
        return null;
    }


    /**
     * 创建更新记录的起始方法
     * 该方法用于初始化更新记录的过程，主要作用是准备数据变更所需的字段映射和环境设置
     *
     * @param fieldMap 字段映射对象，定义了数据变更涉及的表结构
     * @param batchNo 批次号，用于标识一组相关的数据变更操作
     * @param mapper MyBatis Plus基础Mapper接口的实现类，用于执行数据库操作
     * @param tableName 需要进行数据变更的表名
     * @param records 一组待更新的记录列表，包含具体的更新操作信息
     * @return 目前该方法不返回任何值，返回null
     */
    public Object createUpdateRecordStart(DataChangeProperties.DataChangeTable fieldMap,
                            String batchNo,
                            MpBaseMapper<?> mapper,
                            String tableName,
                            List<DataChangeInterceptor.UpdateRecord> records) {
        return null;
    }

    /**
     * 创建或更新记录处理过程
     * 此方法用于处理记录的创建或更新逻辑，具体操作依赖于传入的result对象
     * 它可以是一个数据对象，代表需要创建或更新的记录，也可以是一个包含更新信息的对象
     * 此方法不返回任何值，但可能会修改传入的result对象或与其相关的数据结构
     *
     * @param startResult 一个对象，代表需要创建或更新的记录，或者包含更新信息的对象
     *               具体类型和使用方式取决于应用的具体逻辑和数据模型
     * @return 目前该方法不返回任何值，返回null
     */
    public Object createUpdateRecordProcess(Object startResult) {
        return null;
    }


    /**
     * 创建或更新记录的结束方法
     * 该方法用于在某个业务流程结束后，根据起始结果和处理结果来创建或更新记录
     * 它不返回任何值，但可能对数据库或其他持久化存储中的数据进行了更新
     *
     * @param startResult   业务流程起始阶段的结果，可能包含需要创建或更新记录的初始信息
     * @param processResult 业务流程处理阶段的结果，可能包含需要创建或更新记录的处理后信息
     */
    public void createUpdateRecordEnd(Object startResult, Object processResult) {

    }


    /**
     * 构建插入操作的变更记录
     *
     * @param fieldMap 数据变更表的映射信息，包含了表的结构和备注等信息
     * @param batchNo 批处理编号，用于标识一批变更操作
     * @param tableName 数据库表名，用于记录操作对象
     * @param fieldSet 需要监控插入的字段集合
     * @param records 插入的记录列表，包含多个待插入的实体对象
     * @return 返回一个包含所有插入操作变更记录的列表
     */
    List<ChangeDataRecord> createInsertRecord(DataChangeProperties.DataChangeTable fieldMap,
                                              String batchNo,
                                              String tableName,
                                              Set<String> fieldSet,
                                              List<MpBaseEntity> records) {
        // 初始化操作记录列表
        List<ChangeDataRecord> operationRecords = new ArrayList<>();
        try {
            // 遍历每个记录，生成插入操作的变更记录
            for (MpBaseEntity record : records) {
                // 将实体对象转换为Map，便于操作属性
                Map<String, Object> newMap = BeanMap.create(record);
                for (String fieldName : fieldSet) {
                    // 创建变更记录对象
                    ChangeDataRecord entity = new ChangeDataRecord();
                    // 设置操作类型为插入
                    entity.setOperType(INSERT);
                    // 设置操作对象名称，即数据库表名
                    entity.setObjectName(tableName);
                    // 设置操作对象的显示名称，用于界面展示
                    entity.setObjectNameShow(fieldMap.getTableName());
                    // 如果配置了名称字段，则尝试获取记录的名称
                    if(!StringUtils.isBlank(fieldMap.getNameField())) {
                        Object nameValue = newMap.get(fieldMap.getNameField());
                        if(null != nameValue) {
                            // 设置记录名称
                            entity.setRecordName(nameValue.toString());
                        }
                    }
                    //监控字段值
                    Object monitorValue = newMap.get(fieldName);
                    // 如果是null，先过滤
                    if (Objects.isNull(monitorValue)){
                        continue;
                    }
                    entity.setAfterValue(monitorValue.toString());
                    // 设置字段类型
                    try {
                        entity.setFieldType(record.getClass().getDeclaredField(fieldName).getType().getName());
                    }
                    catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                    entity.setFieldCode(fieldName);
                    entity.setFieldName(fieldName);
                    // 设置批处理编号
                    entity.setBatchNo(batchNo);
                    // 设置记录ID
                    entity.setRecordId(record.getId());
                    // 设置操作时间
                    entity.setOperTime(new Date());
                    // 设置操作用户信息
                    setOperUserInfo(entity);

                    // 初始化参数Map，用于替换备注中的占位符
                    Map<String, Object> params = new HashMap<>();
                    // 将变更记录对象的属性放入参数Map
                    params.putAll(BeanMap.create(entity));
                    // 遍历记录的每个属性，并添加到参数Map
                    for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                        params.put(entry.getKey(), entry.getValue());
                    }
                    // 替换备注中的占位符，并设置备注信息
                    entity.setRemarks(replaceParams(fieldMap.getInsertRemarks(), params));
                    // 将变更记录添加到操作记录列表
                    operationRecords.add(entity);
                }
            }
        }
        catch (Throwable e) {
            // 记录错误信息，并重新抛出异常
            log.error(e.getMessage(), e);
            throw e;
        }
        // 返回操作记录列表
        return operationRecords;
    }

    /**
     * 构建依靠entity进行修改操作的变更记录
     *
     * @param fieldMap 数据变更字段映射
     * @param batchNo 批处理编号
     * @param mapper 数据库操作映射器
     * @param tableName 表名
     * @param records 更新记录列表
     * @return 变更数据记录列表
     */
    List<ChangeDataRecord> createUpdateRecord(DataChangeProperties.DataChangeTable fieldMap,
                                              String batchNo,
                                              MpBaseMapper<?> mapper,
                                              String tableName,
                                              List<DataChangeInterceptor.UpdateRecord> records) {
        // 获取行权限状态
        Object startResult = createUpdateRecordStart(fieldMap,batchNo,mapper,tableName,records);
        Object processResult = null;
        // 初始化变更记录列表
        List<ChangeDataRecord> operationRecords = new ArrayList<>();
        try {
            // 初始化ID集合，用于去重
            Set<String> ids = new HashSet<>();
            // 获取字段映射，如果为空则使用空映射
            Map<String, DataChangeProperties.DataChangeField> fields = MapUtils.emptyIfNull(fieldMap.getFields());
            // 遍历更新记录，过滤出需要的字段并收集ID
            for (DataChangeInterceptor.UpdateRecord record : records) {
                List<String> columns = new ArrayList<>();
                for (String column : record.getUpdateColums()) {
                    // 检查字段是否需要被记录变更
                    if(fields.containsKey(column)
                            || fields.containsKey(DataChangeHelper.humpToLine(column))
                            || fields.containsKey(DataChangeHelper.humpToLine(column).toLowerCase())
                            || fields.containsKey(DataChangeHelper.lineToHump(column))) {
                        ids.add(record.getNewEntity().getId().toString());
                        columns.add(column);
                    }
                }
                record.setUpdateColums(columns);
            }
            // 初始化实体映射，用于存储从数据库查询的结果
            Map<Object, MpBaseEntity> map = new HashMap<>();

            // 如果ID集合不为空，查询这些ID对应的实体
            if(!CollectionUtils.isEmpty(ids)) {
                processResult = createUpdateRecordProcess(startResult);
                map = ListUtils.emptyIfNull(mapper.selectBatchIds(ids)).stream().collect(Collectors.toMap(MpBaseEntity::getId, val -> val, (val1, val2) -> val1));
            }
            // 获取用户信息
            UserInfo userInfo = getUserInfo();
            // 再次遍历更新记录，构建变更记录实体
            for (DataChangeInterceptor.UpdateRecord record : records) {
                if(ids.contains(record.getNewEntity().getId()) && map.containsKey(record.getNewEntity().getId())) {
                    MpBaseEntity oldObject = map.get(record.getNewEntity().getId());
                    Map<String, Object> oldMap = BeanMap.create(oldObject);
                    MpBaseEntity newObject = record.getNewEntity();
                    Map<String, Object> newMap = BeanMap.create(newObject);
                    Collection<String> columns = record.getUpdateColums();
                    // 遍历列，构建变更记录
                    for (String column : columns) {
                        DataChangeProperties.DataChangeField details = fields.get(column);
                        if(null == details) {
                            continue;
                        }
                        String oldVal = DataChangeHelper.formatVal(oldMap.get(column));
                        String oldValShow = "";
                        if(null != details.getBef()) {
                            oldValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(oldMap.get(column), details.getBef()));
                        }
                        else {
                            oldValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(oldMap.get(column), details));
                        }
                        String newVal = DataChangeHelper.formatVal(newMap.get(column));
                        String newValShow = null;
                        if(null != details.getAfter()) {
                            newValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(oldMap.get(column), details.getAfter()));
                        }
                        else {
                            newValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(newMap.get(column), details));
                        }
                        if(oldVal.equalsIgnoreCase(newVal)) {
                            continue;
                        }
                        // 数值比较，忽略大小写
                        if(null != oldVal && null != newVal && NumberUtil.isNumber(oldVal) && NumberUtil.isNumber(newVal)) {
                            try {
                                if(new BigDecimal(oldVal).compareTo(new BigDecimal(newVal)) == 0) {
                                    continue;
                                }
                            }
                            catch (Throwable e) {
                                log.error(e.getMessage(), e);
                            }
                        }

                        // 创建并填充变更记录实体
                        ChangeDataRecord entity = new ChangeDataRecord();
                        entity.setNewObject(newObject);
                        entity.setOldObject(oldObject);
                        entity.setObjectName(tableName);
                        entity.setObjectNameShow(fieldMap.getTableName());
                        if(!StringUtils.isEmpty(fieldMap.getNameField())) {
                            Object nameValue = newMap.get(fieldMap.getNameField());
                            if(null != nameValue) {
                                entity.setRecordName(nameValue.toString());
                            }
                        }
                        entity.setOperType(UPDATE);
                        entity.setSort(details.getSort());
                        entity.setBatchNo(batchNo);
                        entity.setRecordId(oldObject.getId());
                        entity.setOperTime(new Date());
                        entity.setBefValueShow(oldValShow);
                        entity.setAfterValueShow(newValShow);
                        try {
                            entity.setFieldType(oldObject.getClass().getDeclaredField(column).getType().getName());
                        }
                        catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        }
                        entity.setFieldCode(column);
                        entity.setFieldName(details.getFieldName());
                        entity.setBefValue(oldVal);
                        entity.setAfterValue(newVal);
                        setOperUserInfo(entity);

                        // 合并新旧实体的属性，用于替换备注模板中的变量
                        Map<String, Object> params = new HashMap<>();
                        params.putAll(BeanMap.create(entity));
                        for (Map.Entry<String, Object> entry : oldMap.entrySet()) {
                            if(null != entry.getValue() && !StringUtils.isEmpty(entry.getValue().toString())) {
                                params.put(entry.getKey(), entry.getValue());
                            }
                        }
                        for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                            if(null != entry.getValue() && !StringUtils.isEmpty(entry.getValue().toString())) {
                                params.put(entry.getKey(), entry.getValue());
                            }
                        }
                        entity.setRemarks(replaceParams(details.getRemarks(), params));
                        entity.setOperTime(new Date());

                        // 设置操作用户信息
                        if(Objects.nonNull(userInfo) && StringUtils.isNotEmpty(userInfo.getId())) {
                            entity.setOperUserId(userInfo.getId());
                            entity.setOperUserName(userInfo.getName());
                        }
                        // 添加到变更记录列表
                        operationRecords.add(entity);
                    }
                }
            }
        }
        catch (Throwable e) {
            log.debug(e.getMessage(), e);
            throw e;
        }
        finally {
            createUpdateRecordEnd(processResult,processResult);
        }
        return operationRecords;
    }

    /**
     * 构建不依靠entity进行修改操作的变更记录
     *
     * @param batchNo 批次号，用于标识一组变更记录
     * @param tableName 表名，标识发生变更的数据库表
     * @param fieldMap 包含表信息的映射，用于获取表的显示名称等信息
     * @param fields 字段映射，包含每个字段的详细信息，如字段的显示名称、排序号等
     * @param updateFieldSet 需要更新的字段集合
     * @param fieldValueMap 字段值映射，包含每个字段的新值
     * @param baseEntityList 原始实体列表，用于比较和生成变更记录
     * @return 返回一个包含所有变更记录的列表
     */
    List<ChangeDataRecord> createUpdateRecord(String batchNo,
                                              String tableName,
                                              DataChangeTable fieldMap,
                                              Map<String, DataChangeProperties.DataChangeField> fields,
                                              Set<String> updateFieldSet,
                                              Map<String, Object> fieldValueMap,
                                              List<? extends MpBaseEntity> baseEntityList) {
        // 获取当前时间，用于记录操作时间
        Date now = new Date();
        // 获取当前用户信息，用于记录操作用户
        UserInfo userInfo = this.getUserInfo();
        String operUserId = null;
        String operUserName = null;
        // 如果用户信息不为空，则提取用户ID和用户名
        if(Objects.nonNull(userInfo) && StringUtils.isNotEmpty(userInfo.getId())) {
            operUserId = userInfo.getId();
            operUserName = userInfo.getName();
        }
        // 初始化结果列表，预估大小为更新字段数乘以实体列表大小
        List<ChangeDataRecord> result = new ArrayList<>(updateFieldSet.size() * baseEntityList.size());
        // 遍历每个原始实体，生成对应的新实体并比较差异
        for (MpBaseEntity oldEntity : baseEntityList) {
            // 使用Orika工具将原始实体转换为新实体
            MpBaseEntity newEntity = OrikaUtils.convert(oldEntity, oldEntity.getClass());
            try {
                // 将新值设置到新实体中
                for (Map.Entry<String, Object> entry : fieldValueMap.entrySet()) {
                    Field declaredField = oldEntity.getClass().getDeclaredField(entry.getKey());
                    declaredField.setAccessible(true);
                    declaredField.set(newEntity, entry.getValue());
                }
            }
            catch (IllegalAccessException | NoSuchFieldException e) {
                log.error(e.getMessage(), e);
            }

            // 创建原始实体和新实体的属性映射，便于后续比较
            Map<String, Object> oldMap = BeanMap.create(oldEntity);
            Map<String, Object> newMap = BeanMap.create(newEntity);

            // 遍历每个需要更新的字段，生成变更记录
            for (String fieldName : updateFieldSet) {
                DataChangeField details = fields.get(fieldName);
                String oldVal = DataChangeHelper.formatVal(oldMap.get(fieldName));
                String newVal = fieldValueMap.get(fieldName).toString();
                // 如果新旧值相同，则跳过
                if(oldVal.equalsIgnoreCase(newVal)) {
                    continue;
                }

                // 格式化新旧值，以便显示
                String oldValShow;
                if(null != details.getBef()) {
                    oldValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(oldMap.get(fieldName), details.getBef()));
                }
                else {
                    oldValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(oldMap.get(fieldName), details));
                }

                String newValShow;
                if(null != details.getAfter()) {
                    newValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(oldMap.get(fieldName), details.getAfter()));
                }
                else {
                    newValShow = DataChangeHelper.formatVal(DataChangeHelper.formatValue(newMap.get(fieldName), details));
                }

                // 如果新旧值都是数字且相等，则跳过
                if(NumberUtil.isNumber(oldVal) && NumberUtil.isNumber(newVal)) {
                    try {
                        if(new BigDecimal(oldVal).compareTo(new BigDecimal(newVal)) == 0) {
                            continue;
                        }
                    }
                    catch (Throwable e) {
                        log.error(e.getMessage(), e);
                    }
                }

                // 创建变更记录实体
                ChangeDataRecord entity = new ChangeDataRecord();
                entity.setNewObject(newEntity);
                entity.setOldObject(oldEntity);
                entity.setObjectName(tableName);
                entity.setObjectNameShow(fieldMap.getTableName());
                // 设置记录名称
                if(!StringUtils.isEmpty(fieldMap.getNameField())) {
                    Object nameValue = newMap.get(fieldMap.getNameField());
                    if(null != nameValue) {
                        entity.setRecordName(nameValue.toString());
                    }
                }
                entity.setOperType(UPDATE);
                entity.setSort(details.getSort());
                entity.setBatchNo(batchNo);
                entity.setRecordId(oldEntity.getId());
                entity.setOperTime(now);
                entity.setBefValueShow(oldValShow);
                entity.setAfterValueShow(newValShow);
                // 设置字段类型
                try {
                    entity.setFieldType(oldEntity.getClass().getDeclaredField(fieldName).getType().getName());
                }
                catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
                entity.setFieldCode(fieldName);
                entity.setFieldName(fieldName);
                entity.setBefValue(oldVal);
                entity.setAfterValue(newVal);
                setOperUserInfo(entity);

                // 合并新旧实体的属性，用于替换备注中的参数
                Map<String, Object> params = new HashMap<>();
                params.putAll(BeanMap.create(entity));
                for (Map.Entry<String, Object> entry : oldMap.entrySet()) {
                    if(null != entry.getValue() && !StringUtils.isEmpty(entry.getValue().toString())) {
                        params.put(entry.getKey(), entry.getValue());
                    }
                }
                for (Map.Entry<String, Object> entry : newMap.entrySet()) {
                    if(null != entry.getValue() && !StringUtils.isEmpty(entry.getValue().toString())) {
                        params.put(entry.getKey(), entry.getValue());
                    }
                }
                entity.setRemarks(replaceParams(details.getRemarks(), params));

                entity.setOperUserId(operUserId);
                entity.setOperUserName(operUserName);

                // 将变更记录添加到结果列表中
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 用于给变更数据赋值用户相关的信息
     */
    public interface UserInfo {
        String getId();
        String getName();
    }
}

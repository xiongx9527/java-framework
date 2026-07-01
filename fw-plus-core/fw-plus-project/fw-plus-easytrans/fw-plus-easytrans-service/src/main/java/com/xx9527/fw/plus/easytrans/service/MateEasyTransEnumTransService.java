package com.xx9527.fw.plus.easytrans.service;

import cn.hutool.core.util.StrUtil;
import com.fhs.common.utils.CheckUtils;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import com.fhs.trans.service.impl.EnumTransService;
import com.fhs.trans.service.impl.TransService;
import com.xx9527.fw.plus.common.validator.IEnumValiatorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class MateEasyTransEnumTransService extends EnumTransService implements IEnumValiatorService, InitializingBean {

    private static final String MATEEASYTRANSENUMTRANSSERVICE_ENUM_VALUE = "MATEEASYTRANSENUMTRANSSERVICE_ENUM_VALUE";

    /**
     * 枚举值缓存时间（秒）
     * 默认值为604800秒（7天）
     */
    @Value("${fw.plus.mate.easy-trans.enum-trans.enum-value.cache-seconds:604800}")
    private long enumValueCacheSeconds;

    /**
     * 枚举值缓存最大大小
     * 默认值为1000
     */
    @Value("${fw.plus.mate.easy-trans.enum-trans.enum-value.cache-max-size:1000}")
    private int enumValueCacheSecondsMaxSize;

    /**
     * MateEasyTransEnumTransService构造函数
     *
     * 无参构造函数，用于创建MateEasyTransEnumTransService实例
     */
    public MateEasyTransEnumTransService() {
    }

    /**
     * 获取枚举值的映射
     *
     * @param key 枚举的唯一标识符
     * @param valueFieldKey 枚举值的字段键
     * @param labelFieldKey 枚举标签的字段键
     * @return 返回一个映射，其中键是枚举值，值是枚举标签
     */
    private Map<String, Object> getEnumValueMap(Class<?> key,String valueFieldKey,String labelFieldKey){
        // 从全局缓存中获取枚举值映射
        Map<String, Object> enumValueMap = getFromGlobalCache(key.getName(),MATEEASYTRANSENUMTRANSSERVICE_ENUM_VALUE,TransType.ENUM);
        if (null == enumValueMap) {
            enumValueMap = getFromGlobalCache(key,MATEEASYTRANSENUMTRANSSERVICE_ENUM_VALUE,TransType.ENUM);
            if (null == enumValueMap) {
                enumValueMap = new HashMap<>();
                // 获取枚举类
                Class clazz = key;
                if (null != clazz) {
                    Field valueField = null;
                    Field labelField = null;
                    try {
                        // 尝试获取枚举值对应的方法
                        valueField = ReflectUtils.getDeclaredField(clazz,valueFieldKey);
                        if (null != valueField) {
                            valueField.setAccessible(Boolean.TRUE);
                        }
                    } catch (Throwable e) {
                        log.error(String.format("class %s not exist Field:%s", key , valueFieldKey),e);
                    }
                    // 尝试获取枚举标签对应的方法
                    if (null != valueField) {
                        try {
                            // 尝试获取枚举值对应的方法
                            labelField = ReflectUtils.getDeclaredField(clazz,labelFieldKey);
                            if (null != labelField) {
                                labelField.setAccessible(Boolean.TRUE);
                            }
                        } catch (Throwable e) {
                            log.error(String.format("class %s not exist Field:%s", key , labelFieldKey),e);
                        }
                    }
                    // 如果成功获取到方法，则遍历枚举类的所有枚举值，并填充映射
                    if (null != valueField && null != labelField) {
                        Object[] objects = clazz.getEnumConstants();
                        for (Object obj : objects) {
                            try {
                                enumValueMap.put(ConverterUtils.toString(valueField.get(obj)), ConverterUtils.toString(labelField.get(obj)));
                            } catch (Throwable e) {
                                log.error(e.getMessage(),e);
                            }
                        }
                    }
                    // 将枚举值映射放入全局缓存
                    put2GlobalCache(enumValueMap, false, enumValueCacheSeconds, enumValueCacheSecondsMaxSize, key,
                            MATEEASYTRANSENUMTRANSSERVICE_ENUM_VALUE, TransType.ENUM);
                }
            }
        }
        return enumValueMap;
    }

    /**
     * 根据枚举的标签获取对应的枚举值
     *
     * @param key 枚举类型的键，用于获取枚举值映射
     * @param label 枚举的标签，用于匹配枚举项
     * @param valueFieldKey 枚举值的字段键，用于获取枚举值
     * @param labelFieldKey 枚举标签的字段键，用于获取枚举标签
     * @return 返回匹配到的枚举值，如果没有匹配则返回null
     */
    private String getEnumValuelByLabel(Class<?> key,String label,String valueFieldKey,String labelFieldKey) {
        // 获取枚举值映射，如果为null则返回空映射
        Map<String, Object> enumValueMap = MapUtils.emptyIfNull(getEnumValueMap(key,valueFieldKey,labelFieldKey));
        // 遍历枚举值映射，寻找匹配的枚举标签
        for (Map.Entry<String,Object> e: enumValueMap.entrySet()) {
            // 如果当前枚举项的标签与传入的标签匹配，则返回对应的枚举值
            if (Objects.equals(label,ConverterUtils.toString(e.getValue()))) {
                return e.getKey();
            }
        }
        // 如果没有找到匹配的枚举标签，则返回null
        return null;
    }



    /**
     * 根据枚举的值获取其对应的标签
     *
     * @param key 枚举类型的键，用于识别枚举类型
     * @param value 枚举值，用于查找对应的标签
     * @param valueFieldKey 枚举值的字段键，用于在枚举映射中匹配值字段
     * @param labelFieldKey 枚举标签的字段键，用于在枚举映射中匹配标签字段
     * @return 返回找到的枚举标签，如果未找到或映射为空，则返回空字符串
     */
    private Object getEnumLalelByValue(Class<?> key,String value,String valueFieldKey,String labelFieldKey) {
        // 获取枚举值映射，如果为null则返回空映射
        Map<String, Object> enumValueMap = MapUtils.emptyIfNull(getEnumValueMap(key,valueFieldKey,labelFieldKey));
        // 根据枚举值返回对应的枚举标签值
        Object val = enumValueMap.get(value);
        return ObjectUtils.isEmpty(val)?getEnumValuelByLabel(key,value,valueFieldKey,labelFieldKey):val;
    }

    /**
     * 设置枚举引用
     * 该方法用于将给定的值设置到一个价值对象（VO）的指定属性中，该属性由trans对象的ref()方法指定
     * 主要用于在枚举类型和其引用的实体之间建立关联
     *
     * @param trans 枚举实例，包含要设置的属性的引用信息
     * @param vo 值对象，用于承载枚举引用的实际对象
     * @param val 要设置到vo指定属性中的值
     * @return 如果成功设置属性值且值不为null，则返回true；否则返回false
     */
    private boolean setEnumRef(Trans trans, Object vo, Object val) {
        // 初始化设置引用标志为false
        boolean isSetRef = false;

        // 检查trans的ref()是否非空，以决定是否需要设置引用
        if (CheckUtils.isNotEmpty(trans.ref())) {
            try {
                // 尝试将val设置到vo的指定属性中
                setValue(vo, trans.ref(), val);
                // 如果设置成功且值不为null，则将标志设置为true
                isSetRef = Objects.nonNull(ReflectUtils.getValue(vo,trans.ref()));
            } catch (Throwable e) {
                // 如果设置过程中发生错误，记录错误日志
                log.error(String.format("trans.ref() :%s , setValue error "),e);
            }
        }
        // 返回设置引用的结果
        return isSetRef;
    }

    /**
     * 将枚举值转换为相应的标签或值
     *
     * @param obj 要进行转换的值对象
     * @param toTransList 需要转换的字段列表
     */
    public void trans(VO obj, List<Field> toTransList) {
        //枚举转换的临时变量
        Trans tempTrans = null;
        //枚举的唯一标识字段
        Class<?> enumClass = null;
        //枚举的字段数组，包含值字段和标签字段
        String[] fields = null;
        //fields[0]，枚举的实际值字段
        String valueFieldKey = null;
        //fields[1]，枚举的显示标签字段
        String labelFieldKey = null;
        //遍历需要转换的字段列表
        for (Field tempField : toTransList) {
            try {
                //获取字段的枚举转换注解
                tempTrans = tempField.getAnnotation(Trans.class);
                if (null == tempTrans) {
                    continue;
                }
                //设置字段可访问，以读取和写入值
                tempField.setAccessible(true);
                //获取字段值
                Object filedValue = tempField.get(obj);
                if (filedValue == null) {
                    continue;
                }
                //检查字段值类型是否为支持的枚举类型
                if (!(filedValue instanceof String || filedValue instanceof Integer)) {
                    log.error("TransType.ENUM 暂时只支持枚举(String , Integer)，当前字段:" + tempField.getName());
                    continue;
                }
                //获取枚举转换的唯一标识和字段数组
                enumClass = tempTrans.target();
                fields = tempTrans.fields();
                if (ObjectUtils.isEmpty(enumClass) || !enumClass.isEnum() || null == fields || fields.length != 2) {
                    continue;
                }
                //分配字段数组到值字段和标签字段
                valueFieldKey = fields[0];
                labelFieldKey = fields[1];
                //如果字段为空，则跳过转换
                if (StringUtils.isEmpty(valueFieldKey) || StringUtils.isEmpty(labelFieldKey)) {
                    continue;
                }
                //将字段值转换为字符串
                String fieldValueStr = ConverterUtils.toString(filedValue);
                //获取枚举标签值
                Object transResult = getEnumLalelByValue(enumClass, fieldValueStr, valueFieldKey, labelFieldKey);
                Object transRealResult = transResult;
                //如果取不到值，尝试取多个值
                if (ObjectUtils.isEmpty(transRealResult) && StringUtils.split(fieldValueStr, StrUtil.COMMA).length > 1) {
                    String[] fieldValueStrs = StringUtils.split(fieldValueStr, StrUtil.COMMA);
                    List<Object> transResultList = new ArrayList<>();
                    for (String tmpStr : fieldValueStrs) {
                        Object tmpTransResult = getEnumLalelByValue(enumClass, tmpStr, valueFieldKey, labelFieldKey);
                        if (ObjectUtils.isNotEmpty(tmpTransResult)) {
                            transResultList.add(tmpTransResult);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(transResultList)) {
                        transRealResult = StringUtils.join(transResultList.stream().map(e->ConverterUtils.toString(e))
                                .collect(Collectors.toSet()), StrUtil.COMMA);
                    }
                }
                //如果转换结果不为空，尝试设置到对象或其转换映射中
                if (ObjectUtils.isNotEmpty(transRealResult)) {
                    boolean isSetRef = setEnumRef(tempTrans, obj, transRealResult);
                    if (!isSetRef && StringUtils.isEmpty(tempTrans.ref()) && obj.getTransMap() != null) {
                        obj.getTransMap().put(tempField.getName() + "Name", transRealResult);
                    } else if (!isSetRef && !StringUtils.isEmpty(tempTrans.ref())) {
                        obj.getTransMap().put(tempTrans.ref(), transRealResult);
                    }
                }
            } catch (Throwable e) {
                log.error("trans enum error", e);
            }
        }
    }
    /**
     * 根据传入的实体对象和需要转换的字段列表，对每个字段进行枚举值到枚举标签的转换
     * 此方法主要用于将数据库中存储的枚举值（如代码）转换为人类可读的标签
     *
     * @param obj 实体对象，包含需要转换的枚举字段
     * @param toTransList 需要进行枚举转换的字段列表
     */
    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        trans(obj, toTransList);
    }

    /**
     * 批量转换对象列表中的每个对象
     * 此方法用于处理一个包含各种视图对象（VO）的列表，对列表中的每个对象执行转换操作
     * 它通过调用transOne方法来转换每个对象，转换依据由toTransList参数指定的字段列表
     *
     * @param objList 待转换的对象列表，类型为List<? extends VO>，表示可以是任何继承自VO类的列表
     * @param toTransList 指定需要转换的字段列表，类型为List<Field>
     */
    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        // 遍历objList中的每个对象，并对其执行转换操作
        objList.forEach((obj) -> this.transOne(obj, toTransList));
    }


    /**
     * 执行不转换操作
     * 该方法用于处理对象的字段，而不进行转换操作
     * 主要用于在不改变对象字段值的情况下，对字段进行某些处理或检查
     *
     * @param obj 待处理的对象
     * @param toTransList 包含待处理字段的列表
     */
    @Override
    public void unTransOne(Object obj, List<Field> toTransList) {
    }

    /**
     * 反向翻译多个 字段
     *
     * @param objList     需要翻译的对象集合
     * @param toTransList 需要翻译的字段集合
     */
    @Override
    public void unTransMore(List objList, List<Field> toTransList) {
        objList.forEach((obj) -> this.unTransOne(obj, toTransList));
    }

    /**
     * 在所有属性设置完成后执行初始化操作
     * 该方法主要用于在所有依赖属性注入完成后进行一些特定的初始化操作
     *
     * @throws Exception 如果初始化过程中发生错误，则抛出异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册TransType.ENUM类型的事务服务处理方式
        TransService.registerTransType(TransType.ENUM, this);
    }


    /**
     * 判断给定的枚举类、值字段名称和字段值是否满足验证条件
     * @param enumClass
     * @param valueFieldName
     * @param fieldValue
     * @return
     */
    @Override
    public boolean isValid(Class<?> enumClass, String valueFieldName, Object fieldValue) {
        if (null != enumClass && enumClass.isEnum()) {
            return null != getEnumLalelByValue(enumClass, fieldValue.toString(), valueFieldName, valueFieldName);
        }
        return false;
    }
}
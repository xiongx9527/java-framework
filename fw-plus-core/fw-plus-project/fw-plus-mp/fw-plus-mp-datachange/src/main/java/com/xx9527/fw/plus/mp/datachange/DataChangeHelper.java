package com.xx9527.fw.plus.mp.datachange;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.xx9527.fw.plus.common.util.SpringUtils;
import com.xx9527.fw.plus.mp.base.MpBaseEntity;
import com.xx9527.fw.plus.mp.base.MpBaseMapper;
import com.xx9527.fw.plus.mp.datachange.properties.DataChangeProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 主要为BaseAbstarctDataChangeMonitorHandler服务, 拆分部分基础方法, 使程序更加整洁
 */
@Slf4j
public class DataChangeHelper {
    /**
     * 匹配下划线后跟一个单词字符的模式，用于识别下划线命名法中的转换点
     * 例如：user_name 中的 "_n"
     */
    private final static Pattern LINEPATTERN = Pattern.compile("_(\\w)");

    /**
     * 匹配大写字母的模式，用于识别驼峰命名法中的转换点
     * 例如：userName 中的 "N"
     */
    private final static Pattern HUMPPATTERN = Pattern.compile("[A-Z]");

    /**
     * 线程安全的映射表，用于缓存字符串到Class对象的映射关系
     * key: 字符串标识符
     * value: 对应的Class对象
     */
    private final static ConcurrentHashMap<String, Class<?>> MAPPER_CLASS_MAP = new ConcurrentHashMap<>();


    /**
     * 私有构造函数，用于防止外部实例化DataChangeHelper类。
     * 由于这是一个工具类，通常只包含静态方法，因此将其构造函数设为私有以避免被实例化。
     */
    private DataChangeHelper(){}


    /**
     * 根据参数获取实体对象
     * 该方法旨在处理多种类型的输入参数，从中提取出BaseEntity类型的对象
     * 主要处理两种情况：参数直接是BaseEntity类型或参数是包含BaseEntity的ParamMap
     *
     * @param parameter 可以是BaseEntity的子类实例或包含BaseEntity实例的ParamMap实例，或其他类型
     * @return 如果能从参数中提取出BaseEntity，则返回该实体；否则返回null
     */
    public static MpBaseEntity getEntity(Object parameter) {
        // 检查参数是否为空，为空则直接返回null
        if(null == parameter) {
            return null;
        }
        MpBaseEntity entity = null;

        // 如果参数是BaseEntity的实例，则直接转换并返回
        if(parameter instanceof MpBaseEntity) {
            entity = (MpBaseEntity) parameter;
        }
        // 如果参数是ParamMap的实例，则遍历其值，寻找BaseEntity的实例
        else if(parameter instanceof ParamMap) {
            ParamMap map = (ParamMap) parameter;
            for (Object o : map.values()) {
                // 一旦找到BaseEntity的实例，将其赋值给entity，并终止循环
                if(o instanceof MpBaseEntity) {
                    entity = (MpBaseEntity) o;
                    break;
                }
            }
        }
        // 返回找到的BaseEntity实例，或null（如果未找到）
        return entity;
    }

    /**
     * 获取修改的字段
     * 根据传入的参数和实体对象，确定需要更新的字段集合
     * 如果参数中包含特定的字段集合，则使用该集合，否则根据实体对象的属性确定
     *
     * @param parameter 更新操作的参数，可能包含需要更新的字段集合
     * @param entity 实体对象，用于确定更新的字段
     * @param ignoreFields 忽略的字段列表，这些字段不会被包含在返回的字段集合中
     * @return 返回需要更新的字段集合，不包含在ignoreFields中的字段
     */
    public static Collection<String> getUpdateColums(Object parameter, Object entity, List<String> ignoreFields) {
        // 如果参数或实体对象为空，则直接返回空集合
        if(null == parameter || null == entity) {
            return Lists.newArrayList();
        }
        Collection<String> coll = null;
        // 如果参数是ParamMap类型，尝试从参数中获取字段集合
        if(parameter instanceof ParamMap) {
            ParamMap map = (ParamMap) parameter;
            if(map.containsKey("coll")) {
                coll = (Collection) map.get("coll");
            }
        }
        // 如果从参数中获取到的字段集合为空，或者集合中的元素不是String类型，则根据实体对象的属性确定字段集合
        if(CollectionUtils.isEmpty(coll)) {
            coll = getUpdateColumsByEntity(entity);
        }
        else {
            List<?> objList = new ArrayList<>(coll);
            if(!(objList.get(0) instanceof String)) {
                coll = getUpdateColumsByEntity(entity);
            }
        }
        // 如果字段集合为空，则直接返回空集合
        if(CollectionUtils.isEmpty(coll)) {
            return Lists.newArrayList();
        }
        List<String> newColl = new ArrayList<>();
        // 遍历字段集合，排除掉需要忽略的字段
        for (String s : coll) {
            if(!ignoreFields.contains(s)) {
                newColl.add(lineToHump(s));
            }
        }
        // 返回处理后的字段集合
        return newColl;
    }

    /**
     * 根据实体对象获取用于更新操作的列集合
     * 此方法用于检查实体对象中哪些字段有值，从而确定在数据库更新操作中涉及哪些列
     * 主要用于ORM（对象关系映射）场景，帮助构建SQL更新语句
     *
     * @param entity 实体对象，继承自BaseEntity，代表数据库中的一个表的行
     * @return Collection<String> 返回包含所有需要更新的列名的集合
     */
    public static Collection<String> getUpdateColumsByEntity(Object entity) {
        // 初始化一个集合，用于存储需要更新的列名
        Collection<String> coll = new ArrayList<>();

        // 将实体对象转换为Map，以便于遍历其属性
        // 此处使用@SuppressWarnings("unchecked")是因为BeanMap.create(entity)返回的Map类型未指定，但在此处我们明确知道其应为Map<String, Object>
        Map<String, Object> map = BeanMap.create(entity);

        // 遍历实体对象的每个属性，检查其值是否不为空
        for (Map.Entry<String, Object> e : map.entrySet()) {
            // 如果属性值不为空，则将其对应的列名添加到集合中
            if(null != e.getValue()) {
                coll.add(e.getKey());
            }
        }

        // 返回包含所有需要更新的列名的集合
        return coll;
    }

    /**
     * 将下划线风格的字符串转换为驼峰风格
     * 该方法主要用于将数据库列名或文件名等转换为Java代码中常用的驼峰命名方式
     *
     * @param str 待转换的字符串，通常为下划线连接的字符串
     * @return 转换后的驼峰风格字符串
     */
    public static String lineToHump(String str) {
        // 检查输入字符串是否为空或不包含下划线，如果是，则直接返回原字符串
        if(StringUtils.isEmpty(str) || !str.contains(StrUtil.UNDERLINE)) {
            return str;
        }
        // 使用正则表达式匹配所有下划线后的字符，并将其转换为大写
        Matcher matcher = LINEPATTERN.matcher(str.toLowerCase());
        StringBuffer sb = new StringBuffer();
        // 遍历匹配结果，将匹配到的部分替换为大写形式，构建驼峰风格的字符串
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        // 将剩余部分添加到结果字符串中
        matcher.appendTail(sb);
        // 返回转换后的驼峰风格字符串
        return sb.toString();
    }

    /**
     * 驼峰转下划线
     * 这个方法的目的是将驼峰命名法转换为下划线命名法
     * 例如，将"camelCase"转换为"camel_case"
     * 这在某些编程场景中很有用，比如数据库列名通常是下划线命名法
     *
     * @param str 需要转换的字符串，应该是驼峰命名法
     * @return 转换后的字符串，为下划线命名法并且全部大写
     */
    public static String humpToLine(String str) {
        // 检查输入字符串是否为空，如果为空则直接返回
        if(StringUtils.isEmpty(str)) {
            return str;
        }
        // 使用正则表达式匹配驼峰命名的字符串
        Matcher matcher = HUMPPATTERN.matcher(str);
        // 创建一个StringBuffer来存储转换后的字符串
        StringBuffer sb = new StringBuffer();
        // 遍历匹配到的所有驼峰部分
        while (matcher.find()) {
            // 将匹配到的驼峰部分转换为下划线加小写的形式
            matcher.appendReplacement(sb, StrUtil.UNDERLINE + matcher.group(0).toLowerCase());
        }
        // 将剩余的部分添加到StringBuffer中
        matcher.appendTail(sb);
        // 将转换后的字符串全部转换为大写并返回
        // 这是因为在某些情况下，全部大写可能更符合命名约定，例如数据库列名
        return sb.toString().toUpperCase();
    }
    
    /**
     * 根据mapper方法ID获取对应的Mapper实例
     * 此方法用于从Spring容器中根据类名获取Mapper实例，以便进行数据库操作
     * 使用泛型<?>以适应不同类型的Mapper
     *
     * @param mapperMethodId Mapper方法的唯一标识符，用于确定Mapper类型
     * @return 对应的BaseMapper实例，如果找不到则返回null
     */
    public static MpBaseMapper<?> getMapper(String mapperMethodId) {
        // 获取Mapper类的Class对象
        Class<?> clazz = getMappperClass(mapperMethodId);
        // 如果Class对象为空，则返回null
        if(null == clazz) {
            return null;
        }
        // 使用SpringUtils获取对应Class的Bean实例，并强制转换为BaseMapper类型
        return (MpBaseMapper<?>) SpringUtils.getBean(clazz);
    }

    /**
     * 根据Mapper方法ID获取对应的Mapper类
     *
     * @param mapperMethodId Mapper方法的唯一标识符，通常包括类名和方法名
     * @return 对应的Mapper类的Class对象，如果找不到则返回null
     */
    public static Class<?> getMappperClass(String mapperMethodId) {
        // 检查mapperMethodId是否为空，为空则直接返回null
        if(StringUtils.isEmpty(mapperMethodId)) {
            return null;
        }

        // 提取classname，即Mapper类的全限定名
        String classname = mapperMethodId.substring(0, mapperMethodId.lastIndexOf("."));

        // 检查缓存中是否已经存在该classname对应的Class对象
        if(!MAPPER_CLASS_MAP.containsKey(classname)) {
            // 使用ReentrantLock来避免多线程环境下的类重复加载问题
            Lock lock = new ReentrantLock();
            lock.lock();
            try {
                // 再次检查缓存，以确保在锁定期间没有其他线程已经加载了该类
                if(!MAPPER_CLASS_MAP.containsKey(classname)) {
                    try {
                        // 通过类名加载Class对象，并存入缓存中
                        MAPPER_CLASS_MAP.put(classname, Class.forName(classname));
                    }
                    catch (ClassNotFoundException e) {
                        // 如果类加载失败，记录错误日志
                        log.error(e.getMessage(), e);
                    }
                }
            }
            finally {
                // 释放锁
                lock.unlock();
            }
        }

        // 从缓存中获取并返回Class对象
        return MAPPER_CLASS_MAP.get(classname);
    }

    /**
     * 获取实体列表
     * 根据传入的参数类型，尝试转换并返回一个包含BaseEntity实例的列表
     * 如果参数为空，或者不包含符合要求的列表，则返回null
     *
     * @param parameter 可以是包含BaseEntity实例的List或者ParamMap
     * @return 实体列表，如果无法转换则返回null
     */
    public static List<MpBaseEntity> getEntitys(Object parameter) {
        // 参数为空时，直接返回null
        if(null == parameter) {
            return null;
        }
        List<MpBaseEntity> entitys = null;

        // 如果参数是List类型，尝试将其转换为BaseEntity列表
        if(parameter instanceof List<?>) {
            List<?> list = (List) parameter;
            // 检查List中的元素是否为BaseEntity类型
            if(list.get(0) instanceof MpBaseEntity) {
                entitys = (List<MpBaseEntity>) list;
            }
            // entity = (BaseEntity)parameter;
        }
        // 如果参数是ParamMap类型，遍历其值，尝试找到BaseEntity列表
        else if(parameter instanceof ParamMap) {
            ParamMap map = (ParamMap) parameter;
            for (Object o : map.values()) {
                // 对ParamMap中的每个值，如果它是List类型，尝试转换为BaseEntity列表
                if(o instanceof List<?>) {
                    List<?> list = (List) o;
                    // 检查List中的元素是否为BaseEntity类型
                    if(list.get(0) instanceof MpBaseEntity) {
                        entitys = (List<MpBaseEntity>) list;
                        // 找到符合要求的列表后，立即停止遍历
                        break;
                    }
                }
            }
        }
        // 返回转换后的实体列表，如果没有找到符合要求的列表，则返回null
        return entitys;
    }

    /**
     * 格式化值
     * 该方法接受一个对象参数val，并将其格式化为字符串
     * 如果val为null，则返回空字符串；如果val是String类型，则直接返回其内容；
     * 对于其他类型，也返回其toString方法的结果
     *
     * @param val 要格式化的值，可以是任何类型
     * @return 格式化后的字符串
     */
    public static String formatVal(Object val) {
        // 检查val是否为null
        if(null == val) {
            return "";
        }
        // 判断val是否为String类型
        if(val instanceof String) {
            return val.toString();
        }
        // 对于其他类型，也返回其toString方法的結果
        return val.toString();
    }


    /**
     * 格式化值
     * 此方法用于根据提供的格式信息格式化输入值它处理各种情况，如空值、字符串格式、日期格式和枚举转换
     *
     * @param val 要格式化的值，可以是任何类型，但通常为String、Date或Enum
     * @param formmat 字段格式信息，包含格式化的规则和参数
     * @return 格式化后的值如果无法进行格式化或输入为空，则返回空字符串或原始值
     */
    public static Object formatValue(Object val, DataChangeProperties.FieldFormat formmat) {
        // 检查输入值和格式是否为空，如果任一为空，则直接返回空字符串
        if(null == val || null == formmat) {
            return "";
        }
        // 如果格式值不为空，则直接返回格式值
        if(!StringUtils.isBlank(formmat.getValue())) {
            return formmat.getValue();
        }
        try {
            // 如果设置了日期格式且值为Date类型，则按照指定的日期格式进行格式化
            if(null != formmat.getDateFormat() && val instanceof Date) {
                return formatDate((Date) val, formmat.getDateFormat());
            }
            // 如果设置了枚举类，则将值转换为该枚举类对应的值
            else if(null != formmat.getEnumClass() && formmat.getEnumClass().isEnum()) {
                DataChangeHandler dataChangeHandler = SpringUtils.getBean(DataChangeHandler.class);
                Map<Object, Object> map = dataChangeHandler.enumToMap(formmat.getEnumClass());
                // 如果枚举映射为空，则直接返回原始值
                if(null == map) {
                    return val;
                }
                // 返回枚举映射中的对应值
                return map.get(val);
            }
            // 如果以上条件都不满足，则直接返回原始值
            return val;
        }
        // 如果在格式化过程中发生任何错误，记录错误信息并返回原始值
        catch (Throwable e) {
            log.error(e.getMessage(), e);
            return val;
        }
    }


    /**
     * 格式化给定的值，根据指定的字段格式属性
     *
     * @param val 待格式化的值
     * @param field 字段格式属性，包含格式化规则信息
     * @return 格式化后的值如果无法格式化，则返回原始值或空字符串
     */
    public static Object formatValue(Object val, DataChangeProperties.DataChangeField field) {
        // 检查输入参数是否为空，如果为空则直接返回空字符串
        if(null == val || null == field) {
            return "";
        }
        try {
            // 如果字段有指定格式化类，则尝试使用该格式化类进行格式化
            if(null != field.getFormatClass()) {
                DataChangeFormat format = SpringUtils.getBean((Class<DataChangeFormat>) field.getFormatClass());
                if(null != format) {
                    return format.formatValue(val, field);
                }
            }
            // 如果字段有指定日期格式且值为Date类型，则使用指定的日期格式进行格式化
            if(null != field.getDateFormat() && val instanceof Date) {
                return formatDate((Date) val, field.getDateFormat());
            }
            // 如果字段有指定枚举类，则将值映射到枚举类的对应值
            else if(null != field.getEnumClass()) {
                DataChangeHandler dataChangeHandler = SpringUtils.getBean(DataChangeHandler.class);
                Map<Object, Object> map = dataChangeHandler.enumToMap(field.getEnumClass());
                if(null == map) {
                    return val;
                }
                return map.get(val);
            }
            // 如果以上条件都不满足，则直接返回原始值
            return val;
        }
        // 如果在格式化过程中发生任何错误，记录错误信息并返回原始值
        catch (Throwable e) {
            log.error(e.getMessage(), e);
            return val;
        }
    }

    /**
     * 将日期对象转换为格式化的日期字符串
     * 如果没有提供日期对象，则返回null
     * 如果提供了日期对象但没有提供格式模式，则使用默认的"yyyy-MM-dd"格式
     *
     * @param date 日期对象，用于转换为日期字符串如果为null，则方法返回null
     * @param pattern 可变参数，用于指定日期的格式模式如果未提供或为空，则使用默认格式"yyyy-MM-dd"
     * @return 格式化后的日期字符串如果日期对象为null，则返回null
     */
    public static String formatDate(Date date, Object... pattern) {
        // 检查日期对象是否为null，如果为null则直接返回null
        if(date == null) {
            return null;
        }
        String formatDate = null;
        // 检查是否提供了格式模式，如果提供了则使用提供的模式进行格式化
        if(pattern != null && pattern.length > 0) {
            formatDate = DateFormatUtils.format(date, pattern[0].toString());
        }
        // 如果没有提供格式模式，则使用默认的"yyyy-MM-dd"格式进行格式化
        else {
            formatDate = DateFormatUtils.format(date, "yyyy-MM-dd");
        }
        // 返回格式化后的日期字符串
        return formatDate;
    }

    /**
     * 数据格式化接口
     */
    public static interface DataChangeFormat {
        /**
         * 格式化值
         * @param val
         * @param field
         * @return
         */
        Object formatValue(Object val, DataChangeProperties.DataChangeField field);
    }

}

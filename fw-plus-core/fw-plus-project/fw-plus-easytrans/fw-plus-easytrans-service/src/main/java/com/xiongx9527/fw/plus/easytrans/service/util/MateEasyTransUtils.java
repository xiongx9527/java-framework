package com.xiongx9527.fw.plus.easytrans.service.util;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.IgnoreTrans;
import com.fhs.core.trans.util.ReflectUtils;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.utils.TransUtil;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MateEasyTransUtils extends TransUtil {

    /**
     * 是否启用结果映射转换功能的配置值
     * 从配置文件中读取 fw.plus.mate.easy-trans.is-enable-map-result 属性，默认值为 false
     */
    @Value("${fw.plus.mate.easy-trans.is-enable-map-result:false}")
    private Boolean isEnableMapResult;

    /**
     * 是否启用参数映射转换功能的配置值
     * 从配置文件中读取 fw.plus.mate.easy-trans.is-enable-map-arg 属性，默认值为 false
     */
    @Value("${fw.plus.mate.easy-trans.is-enable-map-arg:false}")
    private Boolean isEnableMapArg;

    // 结果映射转换功能的静态开关状态
    private static boolean transResultMap = false;

    // 参数映射转换功能的静态开关状态
    private static boolean transArgMap = false;

    // 代理类映射缓存，存储原始类与代理类的对应关系
    private static final Map<Class, Class> proxyClassMap = new ConcurrentHashMap();

    // 代理类字段映射缓存，存储类与其需要转换的字段集合的对应关系
    private static final Map<Class, Set<String>> proxyClassFieldMap = new ConcurrentHashMap();

    // 不需要进行转换的类名集合
    private static final Map<String,String> notTransClassNamesMap = new ConcurrentHashMap();


    /**
     * 私有构造函数，用于创建MateEasyTransUtils实例
     *
     * 该构造函数被声明为私有，目的是防止外部直接实例化此类，
     * 通常用于工具类或单例模式中，确保类的静态方法可以被调用
     * 而无需创建类的实例
     */
    private MateEasyTransUtils() {
    }

    /**
     * 初始化方法，用于设置转换结果和参数的映射启用状态
     * 该方法在依赖注入完成后执行，确保相关属性被正确处理
     */
    @PostConstruct
    public void init() {
        // 根据isEnableMapResult的值决定是否启用转换结果的映射
        if (Boolean.TRUE.equals(isEnableMapResult)) {
            transResultMap = isEnableMapResult;
        }
        // 根据isEnableMapArg的值决定是否启用转换参数的映射
        if (Boolean.TRUE.equals(isEnableMapArg)) {
            transArgMap = isEnableMapArg;
        }
    }

    /**
     * 批量转换对象集合中的每个对象
     * 此方法用于处理一个对象集合，根据是否是VO（Value Object）类型来决定使用不同的转换策略
     * 如果对象是VO类型，则执行批量转换和字段转换；如果不是，则逐个转换每个对象
     *
     * @param object 要转换的对象集合
     * @param transService 转换服务接口，用于执行具体的转换逻辑
     * @param isProxy 是否使用代理对象，用于控制是否返回代理后的VO对象
     * @param hasTransObjs 已转换的对象列表，用于去重处理
     * @param includeFields 需要转换的字段集合，null表示转换所有字段
     * @param excludeFields 不需要转换的字段集合，null表示不排除任何字段
     * @return 转换后的对象集合，如果输入为null则返回null
     * @throws IllegalAccessException 当访问字段时权限不足
     * @throws InstantiationException 当实例化对象时发生错误
     */
    public static Collection transBatch(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Set<Object> transContext = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        if (hasTransObjs != null) {
            transContext.addAll(hasTransObjs);
        }
        return transBatch(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
    }

    private static Collection transBatch(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<Object> transContext, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Collection param = (Collection)object;
        if (param == null) {
            return null;
        }
        if (param.isEmpty()) {
            return param;
        }

        Object firstNonNull = firstNonNull(param);
        if (firstNonNull == null) {
            return param;
        }

        boolean isVo = firstNonNull instanceof VO;
        if (isVo) {
            ArrayList<VO> transMoreParam = new ArrayList<>();
            for (Object tempObject : param) {
                if (tempObject != null) {
                    transMoreParam.add((VO) tempObject);
                }
            }
            transService.transMore(transMoreParam, includeFields, excludeFields);
            for (Object tempObject : param) {
                if (tempObject != null) {
                    transFields(tempObject, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
                }
            }
        } else {
            for (Object tempObject : param) {
                if (tempObject != null) {
                    transOne(tempObject, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
                }
            }
        }

        if (isProxy && isVo) {
            Collection result;
            if (param instanceof List) {
                result = new ArrayList();
            } else if (param instanceof Set) {
                result = new HashSet();
            } else {
                return param;
            }

            for (Object vo : param) {
                if (vo == null) {
                    result.add(null);
                } else {
                    result.add(createProxyVo((VO)vo));
                }
            }
            return result;
        }
        return param;
    }

    private static Object firstNonNull(Collection collection) {
        for (Object item : collection) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    /**
     * 合并交易明细子VO
     * 此方法用于将一个VO实例与一个VO集合合并，并将结果存储在以VO类为键的映射中
     * 如果VO为空，方法会自动选择集合中的第一个VO实例作为基准
     * 这种设计允许不同类型的VO实例根据其类型分类并合并到映射中对应的列表中
     *
     * @param vo 基准VO实例，如果为空，将自动从集合中选取第一个实例
     * @param vos 一个包含VO实例的集合，这些实例将被合并到基准VO的列表中
     * @param voMap 一个映射，用于存储不同类型的VO列表，键为VO类，值为该类的VO实例列表
     */
    public static void mergeTransSubVo(VO vo, Collection<? extends VO> vos, Map<Class, List<? extends VO>> voMap) {
        List hasVoList = null;
        // 如果基准VO为空，从集合中选取第一个实例作为基准VO
        if (vo == null) {
            vo = (VO)vos.iterator().next();
        }

        // 检查映射中是否已存在基准VO类型的列表，如果存在，则直接使用该列表；否则，创建一个新的列表
        hasVoList = (List)(voMap.containsKey(vo.getClass()) ? (List)voMap.get(vo.getClass()) : new ArrayList());
        // 如果集合不为空，将集合中的所有VO实例添加到列表中；否则，仅将基准VO添加到列表中
        if (vos != null) {
            hasVoList.addAll(vos);
        } else {
            hasVoList.add(vo);
        }

        // 更新映射，将合并后的VO列表与基准VO类型关联
        voMap.put(vo.getClass(), hasVoList);
    }

    /**
     * 检查列表中是否包含特定对象
     *
     * @param list 待检查的列表
     * @param obj 需要查找的对象
     * @return 如果列表中包含该对象，则返回true；否则返回false
     */
    /**
     * Converts a single object.
     *
     * This method is designed to convert a given object based on various conditions and parameters.
     * It supports the conversion of Map objects, VO (Value Object) instances, and collections.
     * The conversion process may involve recursive conversion of internal objects.
     *
     * @param object The object to be converted.
     * @param transService The service that provides the conversion logic.
     * @param isProxy Indicates whether a proxy object should be created for the VO.
     * @param hasTransObjs A list to record objects that have already been converted to avoid repeated conversion.
     * @param includeFields A set of field names to be included in the conversion.
     * @param excludeFields A set of field names to be excluded from the conversion.
     * @return The converted object, which may be the original object, a converted object, or a proxy object.
     * @throws IllegalAccessException If access to a field is not allowed during the conversion process.
     * @throws InstantiationException If an instance of the object cannot be created during the conversion process.
     */
    public static Object transOne(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Set<Object> transContext = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        if (hasTransObjs != null) {
            transContext.addAll(hasTransObjs);
        }
        return transOne(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
    }

    private static Object transOne(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<Object> transContext, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        if (object == null) {
            return null;
        }
        if (transContext.contains(object)) {
            return object;
        }
        transContext.add(object);
        if (hasTransObjs != null) {
            hasTransObjs.add(object);
        }

        boolean isVo = false;
        if (transResultMap && object instanceof Map) {
            Map tempMap = (Map)object;
            for(Object key : tempMap.keySet()) {
                Object mapValue = tempMap.get(key);
                try {
                    tempMap.put(key, transOne(mapValue, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields));
                } catch (Exception var12) {
                    // Exception handling can be logged or processed as needed
                }
            }
            return object;
        }

        if (object instanceof VO) {
            transService.transOne((VO)object, includeFields, excludeFields);
            transFields(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
            isVo = true;
        } else {
            if (object instanceof Collection) {
                return transBatch(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
            }
            if (object.getClass().getName().startsWith("java.")) {
                return object;
            }
            transFields(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
        }
        return isProxy && isVo ? createProxyVo((VO)object) : object;
    }

    /**
     * 根据指定的转换服务对对象的字段进行转换
     * 此方法通过反射遍历对象的所有字段，并根据一定条件对字段值进行转换
     * 主要用于在对象传输或存储前进行数据转换，如加密、序列化等
     *
     * @param object 待转换的对象
     * @param transService 提供转换服务的实例
     * @param isProxy 是否是代理对象，用于某些特殊处理
     * @param hasTransObjs 已转换对象的列表，避免重复转换
     * @param includeFields 需要转换的字段集合，null表示转换所有字段
     * @param excludeFields 不需要转换的字段集合，null表示不排除任何字段
     * @throws IllegalAccessException 当没有权限访问字段时抛出
     * @throws InstantiationException 当通过反射创建对象实例失败时抛出
     */
    public static void transFields(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        Set<Object> transContext = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        if (hasTransObjs != null) {
            transContext.addAll(hasTransObjs);
        }
        transFields(object, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
    }

    private static void transFields(Object object, TransService transService, boolean isProxy, ArrayList<Object> hasTransObjs, Set<Object> transContext, Set<String> includeFields, Set<String> excludeFields) throws IllegalAccessException, InstantiationException {
        // 检查对象是否为空
        if (object != null) {
            Object tempObj = null;
            // 判断对象是否为Map类型，非Map类型进行转换
            if (!(object instanceof Map)) {
                // 检查对象是否属于不需要转换的类
                if (!notTransClassNamesMap.containsKey(object.getClass().getName())) {
                    // 遍历对象的所有字段
                    for(Field field : ReflectUtils.getAllField(object)) {
                        // 对特定的final字段进行转换
                        if (Modifier.isFinal(field.getModifiers()) && (object.getClass().getName().contains("bs") || object.getClass().getName().contains("ejlchina"))) {
                            field.setAccessible(true);
                            tempObj = field.get(object);
                            // 如果字段值为List类型且不为空，则批量转换
                            if (Objects.nonNull(tempObj) && tempObj instanceof List) {
                                List tempList = (List)tempObj;
                                if (!CollectionUtils.isEmpty(tempList) && firstNonNull(tempList) instanceof VO) {
                                    Collection transResult = transBatch(tempObj, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields);
                                    tempList.clear();
                                    tempList.addAll(transResult);
                                }
                            }
                        }

                        // 对非final、非static且没有标注为忽略转换的字段进行转换
                        if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(IgnoreTrans.class) && !object.getClass().getName().startsWith("java.")) {
                            field.setAccessible(true);
                            tempObj = field.get(object);

                            try {
                                field.set(object, transOne(tempObj, transService, isProxy, hasTransObjs, transContext, includeFields, excludeFields));
                            } catch (Exception e) {
                                // 处理反射错误
                                if (e.getClass().getName().contains("java.lang.reflect")) {
                                    log.error("检测到反射错误{}，此类后面程序不在走翻译逻辑:" + object.getClass().getName(), e.getClass());
                                    notTransClassNamesMap.put(object.getClass().getName(),object.getClass().getName());
                                    return;
                                }

                                log.error("如果字段set错误，请反馈给easytrans开发者", e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 验证代理类是否包含指定属性集合
     * 此方法用于检查给定的属性集合是否完全包含在代理类的字段集合中
     * 它通过比较属性集合与代理类字段集合来确保所有属性都被代理类支持
     *
     * @param properties 待验证的属性集合，用于检查是否所有这些属性都存在于代理类中
     * @param clazz 代理类的Class对象，用于从proxyClassFieldMap中获取对应的字段集合
     * @return 如果代理类包含所有指定的属性，则返回true；否则返回false
     */
    private static boolean validProxyClass(Set<String> properties, Class clazz) {
        // 从代理类字段映射中获取指定类的字段集合
        Set<String> fieldSet = (Set)proxyClassFieldMap.get(clazz);
        // 检查字段集合是否包含所有指定的属性
        return fieldSet.containsAll(properties);
    }

    /**
     * 根据提供的VO对象生成一个新的类
     * 此方法旨在动态地创建一个新类，该类继承自VO对象的类，并根据VO对象中的转换映射（transMap）添加新字段
     * 如果已经存在一个有效的代理类并且其字段与VO对象的转换映射匹配，则不会生成新类
     *
     * @param vo 提供的VO对象，用于获取类信息和转换映射
     * @return 生成的或已存在的代理类的Class对象如果生成失败，返回null
     */
    public static Class genNewClass(VO vo) {
        try {
            // 检查是否已经为当前VO类生成过代理类
            Class targetClass = (Class)proxyClassMap.get(vo.getClass());
            // 默认需要生成新类
            boolean isGenNewClass = true;
            // 如果代理类已存在且其字段与VO的转换映射匹配，则不需要生成新类
            if (targetClass != null && validProxyClass(vo.getTransMap().keySet(), targetClass)) {
                isGenNewClass = false;
            }

            // 如果需要生成新类
            if (isGenNewClass) {
                // 定义Jackson和Fastjson注解以忽略序列化
                AnnotationDescription jacksonIgnore = AnnotationDescription.Builder.ofType(JsonIgnore.class).build();
                AnnotationDescription fastJsonIgnore = AnnotationDescription.Builder.ofType(JSONField.class).define("serialize", false).build();
                // 使用ByteBuddy创建新类的构建器，设置父类为VO类，并添加忽略序列化的注解
                DynamicType.Builder<? extends Object> builder = (new ByteBuddy()).subclass(vo.getClass()).name(vo.getClass().getSimpleName() + "DynamicTypeBuilder" + StringUtil.getUUID()).defineMethod("getTransMap", Map.class, 1).intercept(FixedValue.nullValue()).annotateMethod(new AnnotationDescription[]{jacksonIgnore, fastJsonIgnore});

                // 根据VO的转换映射添加新字段到构建器
                for(String property : vo.getTransMap().keySet()) {
                    builder = builder.defineField(property, String.class, 1);
                }

                // 创建并加载新类，然后保存到缓存中
                targetClass = builder.make().load(ClassUtils.getDefaultClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
                proxyClassMap.put(vo.getClass(), targetClass);
                proxyClassFieldMap.put(targetClass, vo.getTransMap().keySet());
            }

            // 返回代理类
            return targetClass;
        } catch (Exception e) {
            // 处理异常情况，特别是类加载异常
            if (e instanceof ClassNotFoundException) {
                log.error("生成新class错误，目前不支持JDK17，请关闭平铺模式: easy-trans.is-enable-tile 设置为false");
            } else {
                log.error("生成新class错误", e);
            }

            // 异常情况下返回null
            return null;
        }
    }

    /**
     * 创建代理VO对象
     * 此方法用于根据传入的VO对象创建一个新的代理对象，并将VO对象的属性复制到新对象中
     * 如果VO对象或其转换映射为空，则直接返回原始VO对象
     *
     * @param vo 待创建代理对象的VO实例如果为null或其转换映射为null，则直接返回该VO对象
     * @return 返回创建的代理VO对象如果创建过程中出现异常或无法创建新对象，则返回原始VO对象
     */
    public static Object createProxyVo(VO vo) {
        // 检查VO对象及其转换映射是否为空，如果任一条件不满足，则直接返回原VO对象
        if (vo != null && vo.getTransMap() != null) {
            try {
                // 生成新的类
                Class clazz = genNewClass(vo);
                if (clazz == null) {
                    return vo;
                }
                // 创建新类的实例
                Object newObject = clazz.newInstance();
                // 如果新创建的对象为空，则直接返回原VO对象
                if (newObject == null) {
                    return vo;
                } else {
                    // 将原VO对象的属性复制到新对象中
                    BeanUtils.copyProperties(vo, newObject);

                    // 遍历转换映射，将映射中的值转换为字符串并设置到新对象中
                    for (String property : vo.getTransMap().keySet()) {
                        ReflectUtils.setValue(newObject, property, ConverterUtils.toString(vo.getTransMap().get(property)));
                    }

                    // 返回新创建的对象
                    return newObject;
                }
            } catch (Exception e) {
                // 如果在创建新对象或复制属性过程中出现异常，记录错误日志并返回原VO对象
                log.error("easy trans 赋值错误", e);
                return vo;
            }
        } else {
            // 如果VO对象或其转换映射为空，直接返回原VO对象
            return vo;
        }
    }
}

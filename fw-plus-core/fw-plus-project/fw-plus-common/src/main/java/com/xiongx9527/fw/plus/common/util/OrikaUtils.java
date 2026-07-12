package com.xiongx9527.fw.plus.common.util;

import cn.hutool.core.util.StrUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ma.glasnost.orika.Converter;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import ma.glasnost.orika.metadata.Type;
import ma.glasnost.orika.metadata.TypeFactory;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public abstract class OrikaUtils {

    /**
     * 获取默认字段工厂
     */
    private static final MapperFactory MAPPER_FACTORY;

    /**
     * 默认字段实例集合
     */
    private static final Cache<String, MapperFacade> CACHE_MAPPER_FACADE;

    static {
        // mapNulls 表示 原对象中的 null 不会拷贝到目标对象
        MAPPER_FACTORY = new DefaultMapperFactory.Builder().mapNulls(false).build();
        CACHE_MAPPER_FACADE = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    private OrikaUtils(){}

    /**
     * 将一个对象转换为另一个类的实例
     * <p>
     * 此方法使用MAPPER_FACADE进行对象到对象的映射，将源对象的数据按照目标类的结构进行转换
     * 它简化了对象转换过程，提高了代码的可读性和可维护性
     *
     * @param <S> 源对象的类型
     * @param <D> 目标对象的类型
     * @param source 需要转换的源对象实例
     * @param toClass 目标对象的类类型
     * @return 转换后的目标对象实例
     */
    public static <S, D> D convert(S source, Class<D> toClass) {
        return convert(source, toClass,null);
    }


    /**
     * 将源对象转换为目标类的实例
     * 此方法使用配置映射来定制转换过程，允许在转换过程中考虑特定的配置
     *
     * @param <S> 源对象的类型
     * @param <D> 目标对象的类型
     * @param source 要转换的源对象
     * @param targetClass 目标对象的类
     * @param fieldMapping 包含转换配置的映射表，用于定制转换过程
     * @return 转换后的目标对象实例
     */
    public static <S, D> D convert(S source, Class<D> targetClass, Map<String, String> fieldMapping) {
        // 获取或创建一个MapperFacade实例，该实例包含了源类和目标类以及转换配置
        MapperFacade mapperFacade = getMapperFacade(source.getClass(), targetClass, null,fieldMapping);
        // 使用MapperFacade实例执行对象转换，并返回转换后的目标对象
        return mapperFacade.map(source, targetClass);
    }

    /**
     * 将源对象的属性复制到目标对象中
     *
     * @param source 源对象，其属性将被复制
     * @param target 目标对象，将源对象的属性复制到此对象中
     *
     * 此方法使用MAPPER_FACADE来执行源对象到目标对象的属性复制操作
     * 它是一个泛型方法，可以处理不同类型的源和目标对象
     */
    public static <S, D> void copy(S source, D target) {
        MapperFacade mapperFacade = getMapperFacade(source.getClass(),target.getClass(), null, null);
        mapperFacade.map(source, target);
    }


    /**
     * 将一个集合中的元素转换为另一个类型的集合
     * 此方法使用映射工具（如MapStruct）来完成对象之间的转换，确保源集合中的每个元素都被转换为目标类型
     *
     * @param <S> 源集合中元素的类型
     * @param <D> 目标集合中元素的类型
     * @param source 源集合，其中包含要转换的元素
     * @param targetClass 目标类型，源集合中的元素将被转换为这种类型
     * @param fieldMapping 配置映射，用于指导映射过程的配置信息
     * @return 目标类型的集合，包含转换后的元素如果源集合的第一个元素为null，则返回null
     */
    public static <S, D> List<D> converts(Collection<S> source, Class<D> targetClass, Map<String, String> fieldMapping) {
        // 检查源集合中的第一个元素是否为null，如果为null，则返回null
        if (source.iterator().next() == null) {
            return null;
        }
        MapperFacade mapperFacade = getMapperFacade(targetClass, source.iterator().next().getClass(), null,fieldMapping);
        // 使用映射工具将源集合中的所有元素转换为目标类型的集合
        return mapperFacade.mapAsList(source, targetClass);
    }


    /**
     * 将一个集合中的元素转换为另一个类类型的集合
     * 此方法使用映射工具（如MapStruct）来执行集合中每个元素的转换，避免了手动转换的繁琐
     *
     * @param <S> 源集合中元素的类型
     * @param <D> 目标集合中元素的类型
     * @param source 源集合，包含待转换的元素
     * @param sourceClass 源集合中元素的类类型，用于初始化映射工具
     * @param targetClass 目标集合中元素的类类型，用于初始化映射工具和指定转换目标类型
     * @param fieldMapping 包含转换配置的映射，用于定制化转换过程
     * @return 转换后的目标集合
     */
    public static <S, D> List<D> converts(Collection<S> source, Class<S> sourceClass, Class<D> targetClass, Map<String, String> fieldMapping) {
        // 根据目标类、源类和配置映射获取映射工具实例
        MapperFacade mapperFacade = getMapperFacade(targetClass, sourceClass, null,fieldMapping);
        // 使用映射工具将源集合中的所有元素转换为目标类类型的集合
        return mapperFacade.mapAsList(source, targetClass);
    }

    /**
     * 获取映射器实例，用于将源类转换为目标类
     * 此方法首先检查缓存中是否已存在相应的映射器实例如果不存在，则创建一个新的映射器实例，
     * 配置源类和目标类之间的字段映射关系，并将其缓存以供下次使用
     *
     * @param sourceClass 源类的Class对象
     * @param targetClass 目标类的Class对象
     * @param converters 一组自定义转换器，用于处理复杂类型的转换
     * @param fieldMapping 字段映射的Map，键为源类字段名，值为目标类字段名
     * @return 返回一个映射器实例，用于将源类转换为目标类
     */
    private static <S, D> MapperFacade getMapperFacade(Class<S> sourceClass, Class<D> targetClass, List<Converter> converters, Map<String, String> fieldMapping) {
        String mapKey = targetClass.getCanonicalName() + StrUtil.UNDERLINE + sourceClass.getCanonicalName();
        try {
            MapperFacade mapperFacade = CACHE_MAPPER_FACADE.get(mapKey, () -> {
                ListUtils.emptyIfNull(converters).forEach(converter -> MAPPER_FACTORY.registerConcreteType(converter.getAType(), converter.getBType()));
                ClassMapBuilder<D, S> classMapBuilder = classMap(sourceClass, targetClass);
                MapUtils.emptyIfNull(fieldMapping).forEach(classMapBuilder::field);
                classMapBuilder.byDefault().register();
                return MAPPER_FACTORY.getMapperFacade();
            });
            return mapperFacade;
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to get MapperFacade", e);
        }
    }

    /**
     * 将源数组转换为目标数组
     * 此方法使用泛型来处理不同类型的数组转换，提高了代码的复用性和灵活性
     * 它依赖于MAPPER_FACADE对象来执行实际的映射操作，将源数组中的元素映射到目标数组中
     *
     * @param <S> 源数组元素的类型
     * @param <D> 目标数组元素的类型
     * @param source 源数组，包含待转换的元素
     * @param target 目标数组，转换后的元素将填充到此数组中
     * @param targetClass 目标数组元素的类，用于确定目标数组的类型
     * @return 转换后的目标数组
     */
    public static <S, D> D[] convertArray(final S[] source, final D[] target, final Class<D> targetClass) {
        // 使用MAPPER_FACADE将源数组映射为目标数组
        return getMapperFacade(source.getClass(),targetClass,null,null).mapAsArray(target, source, targetClass);
    }

    /**
     * 创建一个类映射构建器，用于定义源类和目标类之间的映射关系
     *
     * @param <S> 源类的类型
     * @param <D> 目标类的类型
     * @param source 源类的Class对象，表示映射的起始类
     * @param target 目标类的Class对象，表示映射的目标类
     * @return 返回一个ClassMapBuilder对象，用于进一步配置源类和目标类之间的映射关系
     */
    public static <S, D> ClassMapBuilder<D, S> classMap(Class<S> source, Class<D> target) {
        return MAPPER_FACTORY.classMap(target, source);
    }


    /**
     * 根据原始类型获取Type对象
     * <p>
     * 此方法用于将给定的类转换为Type对象，Type对象提供了更多关于类型的信息，包括泛型信息
     * 它常用于需要进行类型检查或类型转换的场景，特别是在处理泛型时
     *
     * @param rawType 原始类型，即Class对象，代表某种具体类型
     * @return 返回Type对象，该对象表示更为详细的类型信息
     */
    public static <S> Type<S> getType(final Class<S> rawType) {
        return TypeFactory.valueOf(rawType);
    }
}

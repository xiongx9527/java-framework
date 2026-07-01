package com.xx9527.fw.plus.mp.base;


/**
 * @author: jugui.huang
 * @param <T>
 */
public interface MpBaseMapper<T extends MpBaseEntity>  extends MpBaseMapperInternal<T> {

    /**
     * 创建并返回一个新的实体对象
     *
     * @return 返回泛型类型T的新实例
     */
    T newEntity() ;

}

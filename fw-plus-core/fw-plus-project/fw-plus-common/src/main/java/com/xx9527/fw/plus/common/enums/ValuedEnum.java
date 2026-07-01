package com.xx9527.fw.plus.common.enums;

import com.xx9527.fw.plus.common.util.EnumUtils;

/**
 * {@code ValuedEnum} defines an enumeration which is bounded to a value, you
 * may implements this interface when you defines such kind of enumeration, that
 * you can use {@link EnumUtils} to simplify parse and valueOf operation.
 *  
 * @author yutianbao
 */
public interface ValuedEnum<T> {
    /**
     * 获取当前对象的值
     *
     * @return 返回泛型类型T的值
     */
    T value();

}
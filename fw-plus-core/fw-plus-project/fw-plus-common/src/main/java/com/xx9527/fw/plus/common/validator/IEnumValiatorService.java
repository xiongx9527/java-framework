package com.xx9527.fw.plus.common.validator;

public interface IEnumValiatorService {

    /**
     * 判断是否是枚举值
     * @param enumClass
     * @param valueFieldName
     * @param fieldValue
     * @return
     */
    boolean isValid(Class<?> enumClass , String valueFieldName ,Object fieldValue);
}

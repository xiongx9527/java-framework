package com.xiongx9527.fw.plus.common.validator;

public interface IDictValiatorService {

    /**
     * 校验字典
     * @param dictKey
     * @param valueFieldName
     * @param fieldValue
     * @return
     */
    boolean isValid(String dictKey , String valueFieldName ,Object fieldValue);
}

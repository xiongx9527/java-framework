package com.xiongx9527.fw.plus.easytrans.service;

import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.service.impl.ITransTypeService;
import com.fhs.trans.service.impl.TransService;
import com.xiongx9527.fw.plus.common.validator.IEnumValiatorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.util.List;
@Slf4j
public class MateEasyTransBooleanTransService implements IEnumValiatorService ,ITransTypeService, InitializingBean {


    /**
     * 表示布尔值为true时显示的文本值
     * 从配置文件中读取，默认值为"是"
     */
    @Value("${fw.plus.mate.easy-trans.boolean-trans.yes-value:是}")
    private String yesValue;

    /**
     * 表示布尔值为false时显示的文本值
     * 从配置文件中读取，默认值为"否"
     */
    @Value("${fw.plus.mate.easy-trans.boolean-trans.no-value:否}")
    private String noValue;

    /**
     * 验证指定的类是否为Boolean类型
     *
     * @param enumClass 要验证的类类型
     * @param valueFieldName 字段名称（此参数在当前实现中未使用）
     * @param fieldValue 字段值（此参数在当前实现中未使用）
     * @return 如果enumClass不为null且等于Boolean.class则返回true，否则返回false
     */
    @Override
    public boolean isValid(Class<?> enumClass, String valueFieldName, Object fieldValue) {
        return null != enumClass && enumClass.equals(Boolean.class);
    }

    /**
     * 在bean属性设置完成后执行的初始化方法
     * 将当前转换器注册到转换服务中，处理BOOLEAN类型的转换
     *
     * @throws Exception 如果注册过程中发生异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.BOOLEAN, this);
    }


    /**
     * 转换单个对象中的指定字段值
     * 此方法主要用于将对象中的某些字段值，根据预定义的规则进行转换
     * 通常用于在数据传输之前，将数据字段的值转换为符合特定业务逻辑的形式
     *
     * @param obj        要转换的源对象
     * @param toTransList 要转换的字段列表
     */
    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        Trans tempTrans = null;
        //枚举的字段数组，包含值字段和标签字段
        String[] fields = null;
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
                if (!(filedValue instanceof Boolean)) {
                    log.error("TransType.Boolean 暂时只支持枚举(Boolean)，当前字段:" + tempField.getName());
                    continue;
                }

                Boolean booleanValue = (Boolean) filedValue;
                String transResult = Boolean.TRUE.equals(booleanValue)?yesValue:noValue;
                fields = tempTrans.refs();
                if (ObjectUtils.isNotEmpty(fields) && fields.length == 2) {
                    transResult = Boolean.TRUE.equals(booleanValue)?fields[0]:fields[1];
                }
                //如果转换结果不为空，尝试设置到对象或其转换映射中
                if (ObjectUtils.isNotEmpty(transResult)) {
                    setRef(tempTrans, obj, transResult);
                }
            } catch (Throwable e) {
                log.error("trans boolean error", e);
            }
        }
    }

    /**
     * 转换一个对象列表中的每个对象
     * 此方法用于批量处理对象列表，将每个对象的指定字段进行转换
     *
     * @param objList      待转换的对象列表，可以是任何继承自VO的类的列表
     * @param toTransList  需要转换的字段列表，这些字段是对象中需要进行转换的属性
     */
    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        // 遍历objList中的每个对象，并对其执行转换操作
        objList.forEach((obj) -> this.transOne(obj, toTransList));
    }
}

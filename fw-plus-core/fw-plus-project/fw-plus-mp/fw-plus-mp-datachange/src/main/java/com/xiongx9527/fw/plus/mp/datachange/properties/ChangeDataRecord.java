package com.xiongx9527.fw.plus.mp.datachange.properties;

import com.xiongx9527.fw.plus.mp.base.MpBaseEntity;
import lombok.Data;

import java.util.Date;


/**
 * ChangeDataRecord类用于记录数据变更信息
 * 该类包含了数据变更前后的详细信息，包括变更的对象、字段、操作者等信息
 */
@Data
public class ChangeDataRecord {

    /** 变更后的对象实例 */
    private MpBaseEntity newObject;

    /** 变更前的对象实例 */
    private MpBaseEntity oldObject;

    /** 批次号，用于标识同一批次的数据变更 */
    private String batchNo;

    /** 对象名称，标识被变更对象的类型 */
    private String objectName;

    /** 对象显示名称，用于界面展示 */
    private String objectNameShow;

    /** 记录ID，标识具体被变更的记录 */
    private Object recordId;

    /** 记录名称，被变更记录的名称描述 */
    private String recordName;

    /** 字段类型，标识被变更字段的数据类型 */
    private String fieldType;

    /** 字段编码，唯一标识被变更的字段 */
    private String fieldCode;

    /** 字段名称，被变更字段的名称 */
    private String fieldName;

    /** 变更前的值 */
    private String befValue;

    /** 变更前值的显示文本 */
    private String befValueShow;

    /** 变更后的值 */
    private String afterValue;

    /** 变更后值的显示文本 */
    private String afterValueShow;

    /** 备注信息 */
    private String remarks;

    /** 排序字段 */
    private Integer sort;

    /** 操作类型，标识具体的变更操作 */
    private String operType;

    /** 操作时间 */
    private Date operTime;

    /** 操作用户ID */
    private String operUserId;

    /** 操作用户名称 */
    private String operUserName;
}


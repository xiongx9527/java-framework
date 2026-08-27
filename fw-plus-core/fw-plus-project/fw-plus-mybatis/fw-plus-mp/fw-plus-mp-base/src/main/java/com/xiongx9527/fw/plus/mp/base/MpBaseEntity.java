package com.xiongx9527.fw.plus.mp.base;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
/**
 * MyBatis-Plus基础实体类
 *
 * <p>所有实体类的基类，提供基础字段和功能</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>使用Lombok的@Data注解自动生成getter/setter等方法</li>
 *   <li>定义主键字段id，类型为Serializable以支持多种主键类型</li>
 *   <li>抽象类设计，强制子类继承使用</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * public classUser extends MpBaseEntity{
 *     private String name;
 *     private Integer age;
 * }
 * </pre>
 */
@Data
public abstract class MpBaseEntity<T> implements Serializable {

    /**
     * 实体主键ID
     * <p>使用Serializable类型支持多种主键类型：</p>
     * <ul>
     *   <li>Long/Integer等数值类型</li>
     *   <li>String类型UUID</li>
     *   <li>复合主键对象</li>
     * </ul>
     */
    @TableId(value = "id")
    private T id;

}

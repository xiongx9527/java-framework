package com.xiongx9527.fw.plus.mybatis.encrypt.metadata;

import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 字段设置属性信息：属性名（MetaObject 定位用）、映射列名（可选）与
 * {@code @FieldEncrypt} 注解。
 */
@AllArgsConstructor
@Data
public class FieldSetProperty {

	/**
	 * 属性名，用于 MetaObject 读写；与 ORM 列名可能不同。
	 */
	private String fieldName;

	/**
	 * 数据库列名（由 ORM 适配器通过 {@link FieldNameResolver} 提供），可为 {@code null}。
	 */
	private String columnName;

	/**
	 * 字段加密配置。
	 */
	private FieldEncrypt fieldEncrypt;
}

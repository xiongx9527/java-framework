package com.xiongx9527.fw.plus.mybatis.encrypt.metadata;

/**
 * ORM 字段名解析 SPI。
 *
 * <p>通用核心通过该接口把实体属性名解析为数据库列名；MyBatis-Plus 与
 * MyBatis-Flex 适配器分别基于各自实体元数据实现。未配置映射或无法解析时返回
 * {@code null}，调用方回退使用属性名。</p>
 */
public interface FieldNameResolver {

	/**
	 * 解析实体属性对应的数据库列名。
	 *
	 * @param entityType   实体类型
	 * @param propertyName 属性名
	 * @return 列名；无映射或无法解析时返回 {@code null}
	 */
	String resolveColumnName(Class<?> entityType, String propertyName);
}

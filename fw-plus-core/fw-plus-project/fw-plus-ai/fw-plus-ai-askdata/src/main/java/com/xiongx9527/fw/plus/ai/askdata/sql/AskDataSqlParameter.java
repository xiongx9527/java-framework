package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * AskData SQL 的命名参数值对象。
 *
 * <p>参数以名称而非 JDBC 下标绑定，Core 只传递此描述，不向插件暴露裸连接或执行器。</p>
 */
public final class AskDataSqlParameter {

	private final String name;
	private final Object value;

	private AskDataSqlParameter(String name, Object value) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("name 不能为空白");
		}
		this.name = name.trim();
		this.value = value;
	}

	/**
	 * 创建命名参数。
	 *
	 * @param name SQL 中 {@code :name} 的名称
	 * @param value 待由实际执行适配器绑定的值，可以为 {@code null}
	 * @return 不可变命名参数
	 */
	public static AskDataSqlParameter of(String name, Object value) {
		return new AskDataSqlParameter(name, value);
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}
}

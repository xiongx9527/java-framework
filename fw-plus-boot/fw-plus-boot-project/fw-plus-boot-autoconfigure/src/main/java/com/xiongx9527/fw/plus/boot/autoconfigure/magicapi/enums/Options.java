package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums;

public enum Options {

	/**
	 * 包装请求参数到一个变量中
	 */
	WRAP_REQUEST_PARAMETERS("包装请求参数到一个变量中", "wrap_request_parameter"),

	/**
	 * 配置默认数据源的key
	 */
	DEFAULT_DATA_SOURCE("配置默认数据源的key", "default_data_source"),

	/**
	 * 允许拥有该权限的访问
	 */
	PERMISSION("允许拥有该权限的访问", "permission"),

	/**
	 * 允许拥有该权限的查看
	 */
	PERMISSION_VISIBLE("允许拥有该权限的查看", "permission_visible"),

	/**
	 * 允许拥有该角色的访问
	 */
	ROLE("允许拥有该角色的访问", "role"),

	/**
	 * 允许拥有该角色的可查看
	 */
	ROLE_VISIBLE("允许拥有该角色的查看", "role_visible"),

	/**
	 * 该接口需要登录才允许访问
	 */
	REQUIRE_LOGIN("该接口需要登录才允许访问", "require_login", "true"),

	/**
	 * 该接口不需要登录也可访问
	 */
	ANONYMOUS("该接口不需要登录也可访问", "anonymous", "true"),

	/**
	 * 不接收未经定义的参数
	 */
	DISABLED_UNKNOWN_PARAMETER("不接收未经定义的参数", "disabled_unknown_parameter", "true"),

	/**
	 * 禁止验证requestBody
	 */
	DISABLED_VALIDATE_REQUEST_BODY("禁止验证RequestBody", "disabled_validate_request_body", "false");


	/**
	WRITE_SHORTS_AS_STRINGS("短整型写入为字符串", "write_shorts_as_strings", "true"),
	WRITE_DOUBLES_AS_STRINGS("双精度浮点数写入为字符串", "write_doubles_as_strings", "true"),
	WRITE_FLOATS_AS_STRINGS("浮点数写入为字符串", "write_floats_as_strings", "true"),
	WRITE_INTEGERS_AS_STRINGS("整数写入为字符串", "write_integers_as_strings", "true"),
	WRITE_BIGDECIMALS_AS_STRINGS("大整数写入为字符串", "write_bigdecimals_as_strings", "true"),
	WRITE_LONGS_AS_STRINGS("长整数写入为字符串", "write_longs_as_strings", "true"),
	WRITE_NUMBERS_AS_STRINGS("数字写入为字符串", "write_numbers_as_strings", "true"),
	WRITE_DATES_AS_TIMESTAMPS("日期写入为时间戳", "write_dates_as_timestamps", "true");
    **/


	/**
	 * Options类用于存储配置选项的信息
	 * 包含选项的名称、值和默认值
	 */
	private final String name;
	private final String value;
	private final String defaultValue;

	/**
	 * 构造函数，使用名称和值创建Options对象，默认值设为null
	 * @param name 选项名称
	 * @param value 选项值
	 */
	Options(String name, String value) {
		this(name, value, null);
	}

	/**
	 * 构造函数，使用名称、值和默认值创建Options对象
	 * @param name 选项名称
	 * @param value 选项值
	 * @param defaultValue 选项默认值
	 */
	Options(String name, String value, String defaultValue) {
		this.name = name;
		this.value = value;
		this.defaultValue = defaultValue;
	}

	/**
	 * 获取选项名称
	 * @return 返回选项名称
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取选项值
	 * @return 返回选项值
	 */
	public String getValue() {
		return value;
	}

	/**
	 * 获取选项默认值
	 * @return 返回选项默认值
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

}

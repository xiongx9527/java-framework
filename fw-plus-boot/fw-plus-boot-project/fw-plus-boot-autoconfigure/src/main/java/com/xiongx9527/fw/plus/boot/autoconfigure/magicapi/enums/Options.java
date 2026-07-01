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
	DISABLED_VALIDATE_REQUEST_BODY("禁止验证RequestBody", "disabled_validate_request_body", "false"),



	/**************AVOID_REPEAT_SUBMIT_ENABLE*******************/

	/**
	 * 启动避免重复提交
	 */
	AVOID_REPEAT_SUBMIT_ENABLE("启动避免重复提交", "avoid_repeat_submit_enable", "true") ,

	/**
	 * 避免重复提交超时时间
	 */
	AVOID_REPEAT_SUBMIT_TIMEOUT("避免重复提交超时时间(秒)", "avoid_repeat_submit_timeout", "1000") ,

	/**
	 * 避免重复提交的 Key（支持 SpEL 表达式）
	 *
	 * <p>该值可以是 SpEL 表达式，用于动态生成防重提交的 Key。
	 * 例如：#{#request.getHeader('X-Request-ID')} 或 #{#user.id}</p>
	 */
	AVOID_REPEAT_SUBMIT_KEY("避免重复提交的 Key（支持 SpEL 表达式）", "avoid_repeat_submit_key", "#{magicapi:repeat:submit}") ,

	/**
	 * 避免重复提交的 Key 后缀（Spring Bean 名称）
	 */
	AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN("避免重复提交的 Key 后缀（Spring Bean 名称）", "avoid_repeat_submit_key_subfix_spring_bean", "springbean"),

	/**
	 * 避免重复提交的提示信息
	 */
	AVOID_REPEAT_SUBMIT_MSG("避免重复提交提示信息", "avoid_repeat_submit_msg", "重复提交，请稍后再试。") ,

	/**************AVOID_REPEAT_SUBMIT_ENABLE*******************/


	/**************HOT_DATA_ENABLE*******************/

	/**
	 * 启动热数据
	 */
	HOT_DATA_ENABLE("启动热数据", "hot_data_enable", "true") ,

	/**
	 * 热点数据的 Key（支持 SpEL 表达式）
	 */
	HOT_DATA_KEY("热点数据的 Key（支持 SpEL 表达式）", "hot_data_key", "#{magicapi:hot:data}") ,

	/**
	 * 热点数据的等级(QPS , THREAD)
	 */
	HOT_DATA_GRADE("热点数据的等级(QPS , THREAD)", "hot_data_grade", "QPS") ,

	/**
	 * 热点数据的计数
	 */
	HOT_DATA_COUNT("热点数据的计数", "hot_data_count", "0") ,

	/**
	 * 热点数据的时长
	 */
	HOT_DATA_DURATION("热点数据的时长（秒）", "hot_data_duration", "1000") ,

	/**************HOT_DATA_ENABLE*******************/


	/**************FALLBACK_ENABLE*******************/

	FALLBACK_ENABLE("启动熔断降级", "fallback_enable", "true") ,


	FALLBACK_COUNT("熔断降级的计数", "fallback_count", "0") ,

	/**************FALLBACK_ENABLE*******************/


	/**************PRINT_PARAMETERS*******************/

	PRINT_REQUEST_PARAMETERS("打印请求入参（调试用）", "print_request_parameters", "true") ,

	PRINT_RESPONSE_PARAMETERS("打印响应出参（调试用）", "print_response_parameters", "true") ,

	/**************PRINT_PARAMETERS*******************/


	;


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

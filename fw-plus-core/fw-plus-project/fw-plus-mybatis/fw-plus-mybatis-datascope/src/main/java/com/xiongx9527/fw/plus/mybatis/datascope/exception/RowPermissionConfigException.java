package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 行级数据权限配置异常。
 *
 * <p>文案由国际化资源文件按状态码提供，见 {@code i18n/messages.properties}
 * 中 {@code 50003101} - {@code 50003106} 段。</p>
 */
public class RowPermissionConfigException extends BizException {
	private static final long serialVersionUID = 1L;

	/** 状态码 50003001（SERVER/AUTHORIZATION/001 行级数据权限配置解析失败，类通用兜底码） */
	public static final int CODE = 50_003_001;

	/** 行权限配置输入流为空 */
	public static final int INPUT_STREAM_EMPTY = 50_003_101;
	/** 必须配置 dbType */
	public static final int DB_TYPE_REQUIRED = 50_003_102;
	/** 必须配置 tableName */
	public static final int TABLE_NAME_REQUIRED = 50_003_103;
	/** 必须配置 scope */
	public static final int SCOPE_REQUIRED = 50_003_104;
	/** scope 重复定义 */
	public static final int SCOPE_DUPLICATED = 50_003_105;
	/** 加载行权限配置失败 */
	public static final int LOAD_FAILED = 50_003_106;
	/** scope-clause 解析失败 */
	public static final int SCOPE_CLAUSE_PARSE_FAILED = 50_003_107;

	public RowPermissionConfigException(String message) {
		super(CODE, message);
	}

	public RowPermissionConfigException(String message, Throwable cause) {
		super(CODE, message, cause);
	}

	/**
	 * 以「具体错误码 + 参数」构造，文案在读取时按当前请求语言解析。
	 *
	 * @param messageCode 具体错误码（本类的 {@code *_REQUIRED} 等常量）
	 * @param args        文案占位参数
	 */
	private RowPermissionConfigException(int messageCode, Object[] args) {
		super(messageCode, args);
	}

	/**
	 * 国际化构造入口。
	 *
	 * @param messageCode 具体错误码
	 * @param args        文案占位参数
	 */
	public static RowPermissionConfigException of(int messageCode, Object... args) {
		return new RowPermissionConfigException(messageCode, args);
	}
}

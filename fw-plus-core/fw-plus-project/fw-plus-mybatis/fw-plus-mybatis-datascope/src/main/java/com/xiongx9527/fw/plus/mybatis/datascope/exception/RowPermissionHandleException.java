package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 行级数据权限处理异常。
 *
 * <p>文案由国际化资源文件按状态码提供，见 {@code i18n/messages.properties}
 * 中 {@code 50003201} 起的 {@code 500032xx} 段。</p>
 */
public class RowPermissionHandleException extends BizException {
	private static final long serialVersionUID = 1L;

	/** 状态码 50003002（SERVER/AUTHORIZATION/002 行级数据权限处理失败，类通用兜底码） */
	public static final int CODE = 50_003_002;

	/** 未能从 jdbcUrl 获取 dbType */
	public static final int DB_TYPE_UNRESOLVED = 50_003_201;
	/** 不支持的数据库类型 */
	public static final int UNSUPPORTED_DB_TYPE = 50_003_202;
	/** 暂不支持的 SQL */
	public static final int UNSUPPORTED_SQL = 50_003_203;
	/** 不支持的 query 类型 */
	public static final int UNSUPPORTED_QUERY_TYPE = 50_003_204;
	/** 不支持的 tableSource 类型 */
	public static final int UNSUPPORTED_TABLE_SOURCE_TYPE = 50_003_205;
	/** 不支持 Join Using */
	public static final int UNSUPPORTED_JOIN_USING = 50_003_206;
	/** 暂时不支持 update from 语句 */
	public static final int UNSUPPORTED_UPDATE_FROM = 50_003_207;
	/** 暂时不支持 update with 语句 */
	public static final int UNSUPPORTED_UPDATE_WITH = 50_003_208;
	/** 暂时不支持 delete with 语句 */
	public static final int UNSUPPORTED_DELETE_WITH = 50_003_209;
	/** 表未定义行权限 */
	public static final int TABLE_PERMISSION_UNDEFINED = 50_003_210;
	/** 表达式没有预编译 */
	public static final int EXPRESSION_NOT_PRECOMPILED = 50_003_211;
	/** 不支持的数据类型 */
	public static final int UNSUPPORTED_VALUE_TYPE = 50_003_212;
	/** 行权限条件解析失败 */
	public static final int CONDITION_PARSE_FAILED = 50_003_213;
	/** 行权限条件必须返回数字状态 */
	public static final int CONDITION_NOT_NUMERIC = 50_003_214;
	/** 无效的行权限状态 */
	public static final int INVALID_PERMISSION_STATUS = 50_003_215;
	/** 表未定义 scope 的行权限 */
	public static final int SCOPE_PERMISSION_UNDEFINED = 50_003_216;

	public RowPermissionHandleException(String message) {
		super(CODE, message);
	}

	public RowPermissionHandleException(String message, Throwable cause) {
		super(CODE, message, cause);
	}

	/**
	 * 以「具体错误码 + 参数」构造，文案在读取时按当前请求语言解析。
	 */
	private RowPermissionHandleException(int messageCode, Object[] args) {
		super(messageCode, args);
	}

	/**
	 * 国际化构造入口。
	 *
	 * @param messageCode 具体错误码
	 * @param args        文案占位参数
	 */
	public static RowPermissionHandleException of(int messageCode, Object... args) {
		return new RowPermissionHandleException(messageCode, args);
	}
}

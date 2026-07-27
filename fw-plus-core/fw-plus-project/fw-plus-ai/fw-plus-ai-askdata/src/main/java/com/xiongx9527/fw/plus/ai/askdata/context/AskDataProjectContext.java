package com.xiongx9527.fw.plus.ai.askdata.context;

/**
 * AskData 请求的项目与操作人隔离上下文。
 *
 * <p>项目标识限定可访问的数据、元数据和会话范围，操作人标识用于权限判定与审计。Core API 要求二者同时存在，
 * 二次开发的模型、数据源或 Web 适配层必须原样传递并执行该隔离边界。</p>
 */
public class AskDataProjectContext {

	private final String projectId;
	private final String operatorId;

	/**
	 * 创建项目与操作人隔离上下文。
	 *
	 * <p>{@code projectId} 与 {@code operatorId} 均拒绝 {@code null}、空字符串和只包含空白字符的值。
	 * 构造时会去除首尾空白，避免同一隔离主体因输入格式不同而产生不同边界。</p>
	 *
	 * @param projectId 项目唯一标识，不能为空白
	 * @param operatorId 操作人唯一标识，不能为空白
	 * @throws IllegalArgumentException 当项目或操作人标识为空、空白时抛出
	 */
	public AskDataProjectContext(String projectId, String operatorId) {
		this.projectId = requireNonBlank(projectId, "projectId");
		this.operatorId = requireNonBlank(operatorId, "operatorId");
	}

	/**
	 * 获取项目唯一标识。
	 *
	 * @return 非空白项目标识
	 */
	public String getProjectId() {
		return projectId;
	}

	/**
	 * 获取操作人唯一标识。
	 *
	 * @return 非空白操作人标识
	 */
	public String getOperatorId() {
		return operatorId;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || isBlank(value)) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}

	private static boolean isBlank(String value) {
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)) {
				return false;
			}
		}
		return true;
	}
}

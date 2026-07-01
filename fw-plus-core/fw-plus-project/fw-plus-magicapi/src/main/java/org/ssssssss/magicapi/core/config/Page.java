package org.ssssssss.magicapi.core.config;

import lombok.Data;

/**
 * 分页配置
 *
 * @author mxd
 */
@Data
public class Page {

	/**
	 * 默认page表达式
	 */
	private String page = "page";

	/**
	 * 默认size表达式
	 */
	private String size = "size";

	/**
	 * 默认total表达式
	 */
	private String total = "total";

	/**
	 * 默认list表达式
	 */
	private String list = "list";

	/**
	 * 默认首页
	 */
	private long defaultPage = 1;

	/**
	 * 默认页大小
	 */
	private long defaultSize = 10;

	/**
	 * 最大页大小， -1 为不限制
	 */
	private long maxPageSize = -1;
}

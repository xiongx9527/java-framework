package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class WsDataSourceInfo extends MagicEntity {
	/**
	 * 数据源key
	 */
	private String key;

	/**
	 * 密钥
	 */
	private String secret;

	/**
	 * 数据源绑定的端点路径，仅作为数据源元信息保留，不参与握手鉴权解析。
	 *
	 * <p>握手鉴权已从 ws 地基移除；appId 由请求路径命中 {@code fw.plus.ws.servers} 的实例得到，
	 * 详见 {@code ws-spring-boot-starter} 能力规格。</p>
	 */
	private String path;

	/**
	 * 描述
	 */
	private String description;

	/**
	 * 其它配置
	 */
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();

	@Override
	public MagicEntity simple() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		super.simple(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setPath(this.path);
		dataSourceInfo.setProperties(copyProperties());
		return dataSourceInfo;
	}

	@Override
	public MagicEntity copy() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		super.copyTo(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setPath(this.path);
		dataSourceInfo.setProperties(copyProperties());
		return dataSourceInfo;
	}

	private Map<String, Object> copyProperties() {
		return this.properties == null
				? new LinkedHashMap<String, Object>()
				: new LinkedHashMap<String, Object>(this.properties);
	}
}

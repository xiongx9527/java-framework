package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class McpResourceInfo extends PathMagicEntity {

	private String uri;

	private String description;

	private String mimeType;

	private String annotationsJson;

	@Override
	public MagicEntity simple() {
		McpResourceInfo info = new McpResourceInfo();
		super.simple(info);
		copyMcpFields(info);
		return info;
	}

	@Override
	public McpResourceInfo copy() {
		McpResourceInfo info = new McpResourceInfo();
		super.copyTo(info);
		copyMcpFields(info);
		return info;
	}

	private void copyMcpFields(McpResourceInfo info) {
		info.setUri(this.uri);
		info.setDescription(this.description);
		info.setMimeType(this.mimeType);
		info.setAnnotationsJson(this.annotationsJson);
	}
}

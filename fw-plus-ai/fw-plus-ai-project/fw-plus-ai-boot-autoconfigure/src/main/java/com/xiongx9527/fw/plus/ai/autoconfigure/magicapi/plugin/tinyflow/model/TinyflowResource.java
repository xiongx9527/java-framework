package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model;

import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

public class TinyflowResource extends PathMagicEntity {

	public String getContent() {
		return getScript();
	}

	public void setContent(String content) {
		setScript(content);
	}

	@Override
	public TinyflowResource simple() {
		TinyflowResource resource = new TinyflowResource();
		super.simple(resource);
		return resource;
	}

	@Override
	public MagicEntity copy() {
		TinyflowResource resource = new TinyflowResource();
		super.copyTo(resource);
		return resource;
	}
}

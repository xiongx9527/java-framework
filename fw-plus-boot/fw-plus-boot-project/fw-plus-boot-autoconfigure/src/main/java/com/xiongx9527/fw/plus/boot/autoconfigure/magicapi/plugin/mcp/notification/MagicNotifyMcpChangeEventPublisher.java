package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicNotifyService;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Map;

public class MagicNotifyMcpChangeEventPublisher implements McpChangeEventPublisher {

	public static final String TYPE = "mcp-change";

	private final MagicNotifyService magicNotifyService;

	private final String originId;

	public MagicNotifyMcpChangeEventPublisher(MagicNotifyService magicNotifyService, String originId) {
		this.magicNotifyService = magicNotifyService;
		this.originId = originId;
	}

	@Override
	public String getOriginId() {
		return originId;
	}

	@Override
	public void publish(McpChangeEvent event) {
		if (magicNotifyService == null || event == null) {
			return;
		}
		MagicNotify notify = new MagicNotify(originId, event.getMethod(), null, TYPE);
		notify.setContent(JsonUtils.toJsonString(event.toMessage()));
		magicNotifyService.sendNotify(notify);
	}

	public static McpChangeEvent fromNotify(MagicNotify notify) {
		if (notify == null || !TYPE.equals(notify.getType()) || notify.getContent() == null) {
			return null;
		}
		return new McpChangeEvent(notify.getFrom(), JsonUtils.readValue(notify.getContent(), Map.class));
	}
}

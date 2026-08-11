package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MagicResourceTinyflowKnowledgeAuditSink implements MagicTinyflowKnowledgeAuditSink {

	private static final String AUDIT_DIRECTORY = ".tinyflow-audit";
	private final MagicResourceService resources;

	public MagicResourceTinyflowKnowledgeAuditSink(MagicResourceService resources) {
		if (resources == null) throw new IllegalArgumentException("MagicResourceService must not be null");
		this.resources = resources;
	}

	@Override
	public void record(AuditEvent event) {
		if (event == null) throw new IllegalArgumentException("Audit event must not be null");
		Resource workspace = resources.getResource();
		if (workspace == null) throw new IllegalStateException("Magic workspace is unavailable");
		Resource directory = workspace.getDirectory(AUDIT_DIRECTORY);
		if (!directory.exists() && !directory.mkdir() && !directory.exists()) {
			throw new IllegalStateException("Tinyflow audit directory cannot be created");
		}
		if (!directory.isDirectory()) throw new IllegalStateException("Tinyflow audit path is not a directory");

		long recordedAt = System.currentTimeMillis();
		String eventId = UUID.randomUUID().toString();
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("eventId", eventId);
		payload.put("recordedAt", recordedAt);
		payload.put("tenantId", event.getTenantId());
		payload.put("action", event.getAction());
		payload.put("resourceId", event.getResourceId());
		payload.put("success", event.isSuccess());
		payload.put("errorType", event.getErrorType());
		Resource target = directory.getResource(recordedAt + "-" + eventId + ".json");
		if (!target.write(JSON.toJSONString(payload, JSONWriter.Feature.WriteNulls))) {
			throw new IllegalStateException("Tinyflow audit event cannot be persisted");
		}
	}
}

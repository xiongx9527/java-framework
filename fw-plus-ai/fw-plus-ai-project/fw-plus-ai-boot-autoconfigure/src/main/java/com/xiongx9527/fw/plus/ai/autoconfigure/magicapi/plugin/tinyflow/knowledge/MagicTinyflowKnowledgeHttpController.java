package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowIngestionTask;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;

import java.util.List;

public final class MagicTinyflowKnowledgeHttpController extends MagicController {

	private final MagicTinyflowKnowledgeController delegate;

	public MagicTinyflowKnowledgeHttpController(MagicConfiguration configuration) {
		this(configuration, null);
	}

	MagicTinyflowKnowledgeHttpController(MagicConfiguration configuration,
			MagicTinyflowKnowledgeController delegate) {
		super(configuration);
		this.delegate = delegate;
	}

	@RequestMapping(value = "/tinyflow/knowledge/create", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<MagicTinyflowKnowledgeController.KnowledgeView> create(
			@RequestBody MagicTinyflowKnowledgeController.KnowledgeRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().createKnowledge(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/get", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<MagicTinyflowKnowledgeController.KnowledgeView> get(@RequestParam String knowledgeId,
			MagicHttpServletRequest request) {
		read(request); return bean(service().getKnowledge(knowledgeId, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/list", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<MagicTinyflowKnowledgeController.PageResponse<MagicTinyflowKnowledgeController.KnowledgeView>> list(
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
			MagicHttpServletRequest request) {
		read(request); return bean(service().listKnowledge(page, size, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/update", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<MagicTinyflowKnowledgeController.KnowledgeView> update(@RequestParam String knowledgeId,
			@RequestBody MagicTinyflowKnowledgeController.KnowledgeRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().updateKnowledge(knowledgeId, body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/delete", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.DELETE)
	public JsonBean<Object> delete(@RequestParam String knowledgeId, @RequestParam long expectedVersion,
			MagicHttpServletRequest request) {
		authorize(request, Authorization.DELETE); service().deleteKnowledge(knowledgeId, expectedVersion, request); return ok();
	}

	@RequestMapping(value = "/tinyflow/knowledge/import/text", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<TinyflowIngestionTask> importText(
			@RequestBody MagicTinyflowKnowledgeController.TextImportRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().importText(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/import/file", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<TinyflowIngestionTask> importFile(
			@RequestBody MagicTinyflowKnowledgeController.FileImportRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().importFile(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/import/url", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<TinyflowIngestionTask> importUrl(
			@RequestBody MagicTinyflowKnowledgeController.UrlImportRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().importUrl(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/chunk/list", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<MagicTinyflowKnowledgeController.PageResponse<TinyflowChunk>> chunks(
			@RequestParam String knowledgeId, @RequestParam String documentId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
			MagicHttpServletRequest request) {
		read(request); return bean(service().listChunks(knowledgeId, documentId, page, size, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/chunk/update", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<TinyflowChunk> updateChunk(
			@RequestBody MagicTinyflowKnowledgeController.ChunkUpdateRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().updateChunk(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/task/list", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<MagicTinyflowKnowledgeController.PageResponse<TinyflowIngestionTask>> tasks(
			@RequestParam String knowledgeId, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size, MagicHttpServletRequest request) {
		read(request); return bean(service().listTasks(knowledgeId, page, size, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/task/retry", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<TinyflowIngestionTask> retry(@RequestParam String knowledgeId, @RequestParam String taskId,
			MagicHttpServletRequest request) {
		write(request); return bean(service().retryTask(knowledgeId, taskId, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/task/cancel", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<TinyflowIngestionTask> cancel(@RequestParam String knowledgeId, @RequestParam String taskId,
			MagicHttpServletRequest request) {
		write(request); return bean(service().cancelTask(knowledgeId, taskId, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/recall", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<TinyflowKnowledgeResult> recall(
			@RequestBody MagicTinyflowKnowledgeController.RecallRequest body, MagicHttpServletRequest request) {
		read(request); return bean(service().recall(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/options/model", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<List<MagicTinyflowKnowledgeOptionProvider.Option>> models(
			@RequestParam(defaultValue = "100") int limit, MagicHttpServletRequest request) {
		read(request); return bean(service().listModelOptions(limit, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/options/vector", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<List<MagicTinyflowKnowledgeOptionProvider.Option>> vectors(
			@RequestParam(defaultValue = "100") int limit, MagicHttpServletRequest request) {
		read(request); return bean(service().listVectorOptions(limit, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/options/s3", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<List<MagicTinyflowKnowledgeOptionProvider.Option>> s3(
			@RequestParam(defaultValue = "100") int limit, MagicHttpServletRequest request) {
		read(request); return bean(service().listS3Options(limit, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/key/create", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<MagicTinyflowKnowledgeController.CreatedKeyResponse> createKey(
			@RequestBody MagicTinyflowKnowledgeController.CreateKeyRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().createKey(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/key/list", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<List<MagicTinyflowKnowledgeController.AccessKeyView>> keys(@RequestParam String knowledgeId,
			MagicHttpServletRequest request) {
		read(request); return bean(service().listKeys(knowledgeId, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/key/revoke", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<Object> revokeKey(@RequestParam String knowledgeId, @RequestParam String accessKeyId,
			MagicHttpServletRequest request) {
		write(request); service().revokeKey(knowledgeId, accessKeyId, request); return ok();
	}

	@RequestMapping(value = "/tinyflow/knowledge/dify/save", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.SAVE)
	public JsonBean<MagicTinyflowKnowledgeController.DifyConnectionView> saveDify(
			@RequestBody MagicTinyflowKnowledgeController.DifyConnectionRequest body, MagicHttpServletRequest request) {
		write(request); return bean(service().saveDifyConnection(body, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/dify/get", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<MagicTinyflowKnowledgeController.DifyConnectionView> getDify(@RequestParam String connectionId,
			MagicHttpServletRequest request) {
		read(request); return bean(service().getDifyConnection(connectionId, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/dify/list", method = RequestMethod.GET)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<List<MagicTinyflowKnowledgeController.DifyConnectionView>> listDify(@RequestParam String knowledgeId,
			MagicHttpServletRequest request) {
		read(request); return bean(service().listDifyConnections(knowledgeId, request));
	}

	@RequestMapping(value = "/tinyflow/knowledge/dify/delete", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.DELETE)
	public JsonBean<Object> deleteDify(@RequestParam String connectionId, MagicHttpServletRequest request) {
		authorize(request, Authorization.DELETE); service().deleteDifyConnection(connectionId, request); return ok();
	}

	@RequestMapping(value = "/tinyflow/knowledge/dify/test", method = RequestMethod.POST)
	@ResponseBody @Valid(requireLogin = true, authorization = Authorization.VIEW)
	public JsonBean<Object> testDify(@RequestParam String connectionId, MagicHttpServletRequest request) {
		read(request); service().testDifyConnection(connectionId, request); return ok();
	}

	private MagicTinyflowKnowledgeController service() {
		return delegate == null ? SpringUtils.getBean(MagicTinyflowKnowledgeController.class) : delegate;
	}

	private void read(MagicHttpServletRequest request) { authorize(request, Authorization.VIEW); }
	private void write(MagicHttpServletRequest request) { authorize(request, Authorization.SAVE); }
	private void authorize(MagicHttpServletRequest request, Authorization authorization) {
		MagicUser user = (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
		isTrue(configuration.getAuthorizationInterceptor().allowVisit(user, request, authorization), PERMISSION_INVALID);
	}
	private <T> JsonBean<T> bean(T value) { return new JsonBean<T>(value); }
	private JsonBean<Object> ok() { return new JsonBean<Object>("ok"); }
}

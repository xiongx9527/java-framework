package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserNamespace;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserObjectDetail;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserObjectInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewResult;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserRoutineInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service.DbBrowserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.List;

public class MagicDbBrowserController extends MagicController implements MagicExceptionHandler {

	private final MagicConfiguration configuration;
	private final DbBrowserService service;

	public MagicDbBrowserController(MagicConfiguration configuration, DbBrowserService service) {
		super(configuration);
		this.configuration = configuration;
		this.service = service;
	}

	@GetMapping("/db/browser/datasources")
	@ResponseBody
	@Valid(requireLogin = true)
	public JsonBean<List<DbBrowserDataSourceInfo>> listDataSources(MagicHttpServletRequest request) {
		requireViewPermission(request);
		return new JsonBean<>(service.listDataSources());
	}

	@GetMapping("/db/browser/namespaces")
	@ResponseBody
	@Valid(requireLogin = true)
	public JsonBean<List<DbBrowserNamespace>> listNamespaces(@RequestParam String key, MagicHttpServletRequest request) {
		requireViewPermission(request);
		return new JsonBean<>(service.listNamespaces(key));
	}

	@GetMapping("/db/browser/objects")
	@ResponseBody
	@Valid(requireLogin = true)
	public JsonBean<List<DbBrowserObjectInfo>> listObjects(@RequestParam String key,
			@RequestParam(required = false) String catalog, @RequestParam(required = false) String schema,
			@RequestParam(required = false) String type, MagicHttpServletRequest request) {
		requireViewPermission(request);
		return new JsonBean<>(service.listObjects(key, catalog, schema, type));
	}

	@GetMapping("/db/browser/routines")
	@ResponseBody
	@Valid(requireLogin = true)
	public JsonBean<List<DbBrowserRoutineInfo>> listRoutines(@RequestParam String key,
			@RequestParam(required = false) String catalog, @RequestParam(required = false) String schema,
			@RequestParam(required = false) String type, MagicHttpServletRequest request) {
		requireViewPermission(request);
		return new JsonBean<>(service.listRoutines(key, catalog, schema, type));
	}

	@GetMapping("/db/browser/object/detail")
	@ResponseBody
	@Valid(requireLogin = true)
	public JsonBean<DbBrowserObjectDetail> getObjectDetail(@RequestParam String key,
			@RequestParam(required = false) String catalog, @RequestParam(required = false) String schema,
			@RequestParam String objectName, @RequestParam String objectType, MagicHttpServletRequest request) {
		requireViewPermission(request);
		return new JsonBean<>(service.getObjectDetail(key, catalog, schema, objectName, objectType));
	}

	@PostMapping("/db/browser/preview")
	@ResponseBody
	@Valid(requireLogin = true)
	public JsonBean<DbBrowserPreviewResult> preview(@RequestBody DbBrowserPreviewRequest request,
			MagicHttpServletRequest httpRequest) {
		requireViewPermission(httpRequest);
		return new JsonBean<>(service.preview(request));
	}

	private void requireViewPermission(MagicHttpServletRequest request) {
		MagicUser magicUser = (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
		isTrue(configuration.getAuthorizationInterceptor().allowVisit(magicUser, request, Authorization.VIEW), PERMISSION_INVALID);
	}
}

package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}")
public class TinyEngineAppWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineAppWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@GetMapping("/app-center/api/apps/page")
	public Mono<ResponseEntity<? extends Result<?>>> appsPage(@RequestParam Integer currentPage,
			@RequestParam Integer pageSize, @RequestParam(required = false) String name,
			@RequestParam(required = false) Integer industryId, @RequestParam(required = false) Integer sceneId,
			@RequestParam(required = false) String framework, @RequestParam(required = false) String orderBy,
			@RequestParam(required = false) String createdBy) {
		App app = new App();
		app.setName(name);
		app.setIndustryId(industryId);
		app.setSceneId(sceneId);
		app.setFramework(framework);
		app.setCreatedBy(createdBy);
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.appsPage(currentPage, pageSize, orderBy, app));
	}

	@GetMapping("/app-center/api/apps/list/{platformId}")
	public Mono<ResponseEntity<? extends Result<?>>> appList(@PathVariable Integer platformId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> {
			App app = new App();
			app.setPlatformId(platformId);
			AppDto page = facade.appsPage(Integer.valueOf(1), Integer.valueOf(1000), null, app);
			return page == null ? null : page.getApps();
		});
	}

	@PostMapping("/app-center/api/apps/create")
	public Mono<ResponseEntity<? extends Result<?>>> createApp(@RequestBody App app) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createApp(app));
	}

	@PostMapping("/app-center/api/apps/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updateApp(@PathVariable Integer id, @RequestBody App app) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateApp(id, app));
	}

	@GetMapping("/app-center/api/apps/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deleteApp(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteApp(id));
	}

	@GetMapping("/app-center/api/apps/save/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> saveApp(@PathVariable Integer id,
			@RequestParam Map<String, Object> request) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.saveApp(id, request));
	}

	@PostMapping("/app-center/api/apps/publish/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> publishApp(@PathVariable Integer id,
			@RequestBody(required = false) Map<String, Object> request) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.publishApp(id, request));
	}

	@GetMapping("/app-center/api/apps/preview/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> previewApp(@PathVariable Integer id,
			@RequestParam Map<String, Object> request) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.previewApp(id, request));
	}

	@GetMapping("/app-center/api/apps/extension/list")
	public Mono<ResponseEntity<? extends Result<?>>> appExtensions(@RequestParam Integer app,
			@RequestParam String category) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.appExtensions(app, category));
	}

	@PostMapping("/app-center/api/apps/extension/create")
	public Mono<ResponseEntity<? extends Result<?>>> createAppExtension(@RequestBody AppExtension extension) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createAppExtension(extension));
	}

	@PostMapping("/app-center/api/apps/extension/update")
	public Mono<ResponseEntity<? extends Result<?>>> updateAppExtension(@RequestBody AppExtension extension) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateAppExtension(extension));
	}

	@GetMapping("/app-center/api/apps/extension/delete")
	public Mono<ResponseEntity<? extends Result<?>>> deleteAppExtension(@RequestParam Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteAppExtension(id));
	}

	@GetMapping("/app-center/api/apps/canvas/lock")
	public Mono<ResponseEntity<? extends Result<?>>> lockCanvas(@RequestParam Integer id,
			@RequestParam String state, @RequestParam String type) {
		Map<String, Object> request = new LinkedHashMap<String, Object>();
		request.put("state", state);
		request.put("type", type);
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.lockCanvas(id, request));
	}

	@GetMapping({"/app-center/api/apps/{id}", "/app-center/api/apps/detail/{id}"})
	public Mono<ResponseEntity<? extends Result<?>>> appDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			App app = facade.appDetail(id);
			if (app == null) {
				return TinyEngineWebResponses.notFound("TinyEngine app not found: " + id);
			}
			return TinyEngineWebResponses.ok(app);
		});
	}

	@GetMapping({"/app-center/v1/api/apps/schema/{id}", "/app-center/api/apps/schema/{id}"})
	public Mono<ResponseEntity<? extends Result<?>>> appSchema(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			Map<String, Object> schema = facade.appSchema(id);
			if (schema == null) {
				return TinyEngineWebResponses.notFound("TinyEngine app schema not found: " + id);
			}
			return TinyEngineWebResponses.ok(schema);
		});
	}

	@GetMapping("/app-center/api/apps/schema/components/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> appSchemaComponents(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.appSchemaComponents(id));
	}
}

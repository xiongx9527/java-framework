package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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

	@PostMapping("/app-center/api/apps/create")
	public Mono<ResponseEntity<? extends Result<?>>> createApp(@RequestBody App app) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createApp(app));
	}

	@GetMapping({"/app-center/api/apps/{id}", "/app-center/api/apps/detail/{id}"})
	public Mono<ResponseEntity<? extends Result<?>>> appDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.appDetail(id));
	}

	@GetMapping("/app-center/v1/api/apps/schema/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> appSchema(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.appSchema(id));
	}
}

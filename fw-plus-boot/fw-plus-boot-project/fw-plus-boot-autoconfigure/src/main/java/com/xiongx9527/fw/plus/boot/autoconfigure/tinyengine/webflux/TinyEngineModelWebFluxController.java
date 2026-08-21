package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}")
public class TinyEngineModelWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineModelWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@GetMapping({"/material-center/api/model/list", "/material-center/api/model"})
	public Mono<ResponseEntity<? extends Result<?>>> models(@RequestParam(required = false) Integer currentPage,
			@RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String nameCn,
			@RequestParam(required = false) String nameEn) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.models(currentPage, pageSize, nameCn, nameEn));
	}

	@GetMapping("/material-center/api/model/detail/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> modelDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.modelDetail(id));
	}

	@PostMapping("/material-center/api/model/create")
	public Mono<ResponseEntity<? extends Result<?>>> createModel(@RequestBody Model model) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createModel(model));
	}

	@PutMapping("/material-center/api/model/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updateModel(@PathVariable Integer id, @RequestBody Model model) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateModel(id, model));
	}

	@DeleteMapping("/material-center/api/model/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deleteModel(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteModel(id));
	}

	@GetMapping("/app-center/api/sources/list/{appId}")
	public Mono<ResponseEntity<? extends Result<?>>> datasources(@PathVariable Integer appId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.datasources(appId));
	}

	@GetMapping("/app-center/api/sources/detail/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> datasourceDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.datasourceDetail(id));
	}

	@PostMapping("/app-center/api/sources/create")
	public Mono<ResponseEntity<? extends Result<?>>> createDatasource(@RequestBody Datasource datasource) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createDatasource(datasource));
	}

	@PostMapping("/app-center/api/sources/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updateDatasource(@PathVariable Integer id,
			@RequestBody Datasource datasource) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateDatasource(id, datasource));
	}

	@GetMapping("/app-center/api/sources/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deleteDatasource(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteDatasource(id));
	}

	@GetMapping("/app-center/api/app-template/list")
	public Mono<ResponseEntity<? extends Result<?>>> appTemplates(@RequestParam Integer currentPage,
			@RequestParam Integer pageSize, @RequestParam(required = false) String orderBy,
			@RequestParam(required = false) String name) {
		App app = new App();
		app.setName(name);
		return TinyEngineWebFluxSupport.submit(executor,
				() -> facade.appTemplates(currentPage, pageSize, orderBy, app));
	}

	@PostMapping("/app-center/api/app-template/create")
	public Mono<ResponseEntity<? extends Result<?>>> createAppTemplate(@RequestBody App app) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createAppTemplate(app));
	}

	@PostMapping("/app-center/api/app-template/set")
	public Mono<ResponseEntity<? extends Result<?>>> setAppTemplate(@RequestBody java.util.Map<String, Object> request) {
		Integer id = Integer.valueOf(String.valueOf(request.get("id")));
		Boolean isTemplate = Boolean.valueOf(String.valueOf(request.get("isTemplate")));
		String templateType = request.get("templateType") == null ? null : String.valueOf(request.get("templateType"));
		Integer industryId = request.get("industryId") == null ? null : Integer.valueOf(String.valueOf(request.get("industryId")));
		Integer sceneId = request.get("sceneId") == null ? null : Integer.valueOf(String.valueOf(request.get("sceneId")));
		String framework = request.get("framework") == null ? null : String.valueOf(request.get("framework"));
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.setAppTemplate(id, isTemplate, templateType, industryId, sceneId, framework));
	}

	@GetMapping("/app-center/api/i18n/entries")
	public Mono<ResponseEntity<? extends Result<?>>> i18nEntries(@RequestParam(required = false) Integer host,
			@RequestParam(value = "host_type", required = false) String hostType) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.i18nEntries(host, hostType));
	}

	@PostMapping({"/app-center/api/i18n/entries", "/app-center/api/i18n/entries/create"})
	public Mono<ResponseEntity<? extends Result<?>>> createI18nEntries(@RequestBody OperateI18nEntries entries) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createI18nEntries(entries));
	}

	@PostMapping("/app-center/api/apps/{appId}/i18n/entries/update")
	public Mono<ResponseEntity<? extends Result<?>>> updateI18nEntriesFile(@PathVariable Integer appId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateI18nEntriesFile(appId));
	}
}

package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
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

import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}")
public class TinyEngineAppMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEngineAppMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@GetMapping("/app-center/api/apps/page")
	public ResponseEntity<? extends Result<?>> appsPage(@RequestParam Integer currentPage,
			@RequestParam Integer pageSize, @RequestParam(required = false) String name,
			@RequestParam(required = false) Integer industryId, @RequestParam(required = false) Integer sceneId,
			@RequestParam(required = false) String framework, @RequestParam(required = false) String orderBy,
			@RequestParam(required = false) String createdBy) {
		try {
			App app = new App();
			app.setName(name);
			app.setIndustryId(industryId);
			app.setSceneId(sceneId);
			app.setFramework(framework);
			app.setCreatedBy(createdBy);
			return TinyEngineWebResponses.ok(facade.appsPage(currentPage, pageSize, orderBy, app));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/create")
	public ResponseEntity<? extends Result<?>> createApp(@RequestBody App app) {
		try {
			return TinyEngineWebResponses.ok(facade.createApp(app));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping({"/app-center/api/apps/{id}", "/app-center/api/apps/detail/{id}"})
	public ResponseEntity<? extends Result<?>> appDetail(@PathVariable Integer id) {
		try {
			App app = facade.appDetail(id);
			if (app == null) {
				return TinyEngineWebResponses.notFound("TinyEngine app not found: " + id);
			}
			return TinyEngineWebResponses.ok(app);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/v1/api/apps/schema/{id}")
	public ResponseEntity<? extends Result<?>> appSchema(@PathVariable Integer id) {
		try {
			Map<String, Object> schema = facade.appSchema(id);
			if (schema == null) {
				return TinyEngineWebResponses.notFound("TinyEngine app schema not found: " + id);
			}
			return TinyEngineWebResponses.ok(schema);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

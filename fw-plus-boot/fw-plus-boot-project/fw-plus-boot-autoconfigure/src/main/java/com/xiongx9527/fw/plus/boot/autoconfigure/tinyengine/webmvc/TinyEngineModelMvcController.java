package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
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

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}")
public class TinyEngineModelMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEngineModelMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@GetMapping("/material-center/api/model/list")
	public ResponseEntity<? extends Result<?>> models(@RequestParam(required = false) Integer currentPage,
			@RequestParam(required = false) Integer pageSize, @RequestParam(required = false) String nameCn,
			@RequestParam(required = false) String nameEn) {
		try {
			return TinyEngineWebResponses.ok(facade.models(currentPage, pageSize, nameCn, nameEn));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/material-center/api/model/detail/{id}")
	public ResponseEntity<? extends Result<?>> modelDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.modelDetail(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/material-center/api/model/create")
	public ResponseEntity<? extends Result<?>> createModel(@RequestBody Model model) {
		try {
			return TinyEngineWebResponses.ok(facade.createModel(model));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/material-center/api/model/update/{id}")
	public ResponseEntity<? extends Result<?>> updateModel(@PathVariable Integer id, @RequestBody Model model) {
		try {
			return TinyEngineWebResponses.ok(facade.updateModel(id, model));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@DeleteMapping("/material-center/api/model/delete/{id}")
	public ResponseEntity<? extends Result<?>> deleteModel(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteModel(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/sources/list/{appId}")
	public ResponseEntity<? extends Result<?>> datasources(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.datasources(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/sources/detail/{id}")
	public ResponseEntity<? extends Result<?>> datasourceDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.datasourceDetail(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/sources/create")
	public ResponseEntity<? extends Result<?>> createDatasource(@RequestBody Datasource datasource) {
		try {
			return TinyEngineWebResponses.ok(facade.createDatasource(datasource));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/sources/update/{id}")
	public ResponseEntity<? extends Result<?>> updateDatasource(@PathVariable Integer id,
			@RequestBody Datasource datasource) {
		try {
			return TinyEngineWebResponses.ok(facade.updateDatasource(id, datasource));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/sources/delete/{id}")
	public ResponseEntity<? extends Result<?>> deleteDatasource(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteDatasource(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/app-template/list")
	public ResponseEntity<? extends Result<?>> appTemplates(@RequestParam Integer currentPage,
			@RequestParam Integer pageSize, @RequestParam(required = false) String orderBy,
			@RequestParam(required = false) String name) {
		try {
			App app = new App();
			app.setName(name);
			return TinyEngineWebResponses.ok(facade.appTemplates(currentPage, pageSize, orderBy, app));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/app-template/create")
	public ResponseEntity<? extends Result<?>> createAppTemplate(@RequestBody App app) {
		try {
			return TinyEngineWebResponses.ok(facade.createAppTemplate(app));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/i18n/entries")
	public ResponseEntity<? extends Result<?>> i18nEntries(@RequestParam(required = false) Integer host,
			@RequestParam(value = "host_type", required = false) String hostType) {
		try {
			return TinyEngineWebResponses.ok(facade.i18nEntries(host, hostType));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping({"/app-center/api/i18n/entries", "/app-center/api/i18n/entries/create"})
	public ResponseEntity<? extends Result<?>> createI18nEntries(@RequestBody OperateI18nEntries entries) {
		try {
			return TinyEngineWebResponses.ok(facade.createI18nEntries(entries));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/{appId}/i18n/entries/update")
	public ResponseEntity<? extends Result<?>> updateI18nEntriesFile(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.updateI18nEntriesFile(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

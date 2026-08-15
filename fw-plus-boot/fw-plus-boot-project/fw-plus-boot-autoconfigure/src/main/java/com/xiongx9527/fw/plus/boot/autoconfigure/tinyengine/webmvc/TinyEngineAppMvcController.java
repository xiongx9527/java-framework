package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberRoleRequest;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	@GetMapping("/app-center/api/apps/list/{platformId}")
	public ResponseEntity<? extends Result<?>> appList(@PathVariable Integer platformId) {
		try {
			App app = new App();
			app.setPlatformId(platformId);
			AppDto page = facade.appsPage(Integer.valueOf(1), Integer.valueOf(1000), null, app);
			return TinyEngineWebResponses.ok(page == null ? null : page.getApps());
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

	@PostMapping("/app-center/api/apps/update/{id}")
	public ResponseEntity<? extends Result<?>> updateApp(@PathVariable Integer id, @RequestBody App app) {
		try {
			return TinyEngineWebResponses.ok(facade.updateApp(id, app));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/delete/{id}")
	public ResponseEntity<? extends Result<?>> deleteApp(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteApp(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/{appId}/members")
	public ResponseEntity<? extends Result<?>> appMembers(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.appMembers(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/{appId}/members")
	public ResponseEntity<? extends Result<?>> addAppMember(@PathVariable Integer appId,
			@RequestBody AppMemberRoleRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.addAppMember(appId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/app-center/api/apps/{appId}/members/{userId}")
	public ResponseEntity<? extends Result<?>> changeAppMember(@PathVariable Integer appId,
			@PathVariable String userId, @RequestBody AppMemberRoleRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.changeAppMember(appId, userId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@DeleteMapping("/app-center/api/apps/{appId}/members/{userId}")
	public ResponseEntity<? extends Result<?>> removeAppMember(@PathVariable Integer appId,
			@PathVariable String userId) {
		try {
			facade.removeAppMember(appId, userId);
			return TinyEngineWebResponses.ok(Boolean.TRUE);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/{appId}/ai-model-services")
	public ResponseEntity<? extends Result<?>> usableAiModelServices(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.usableAiModelServices(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/{appId}/ai-model-services/manage")
	public ResponseEntity<? extends Result<?>> manageableAiModelServices(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.manageableAiModelServices(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/{appId}/ai-model-services/settings")
	public ResponseEntity<? extends Result<?>> aiModelSettings(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.aiModelSettings(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/app-center/api/apps/{appId}/ai-model-services/settings")
	public ResponseEntity<? extends Result<?>> updateAiModelSettings(@PathVariable Integer appId,
			@RequestBody AppAiModelSettings settings) {
		try {
			return TinyEngineWebResponses.ok(facade.updateAiModelSettings(appId, settings));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/{appId}/ai-model-services")
	public ResponseEntity<? extends Result<?>> createAiModelService(@PathVariable Integer appId,
			@RequestBody AiModelServiceRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.createAiModelService(appId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/app-center/api/apps/{appId}/ai-model-services/{serviceId}")
	public ResponseEntity<? extends Result<?>> updateAiModelService(@PathVariable Integer appId,
			@PathVariable Long serviceId, @RequestBody AiModelServiceRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.updateAiModelService(appId, serviceId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@DeleteMapping("/app-center/api/apps/{appId}/ai-model-services/{serviceId}")
	public ResponseEntity<? extends Result<?>> deleteAiModelService(@PathVariable Integer appId,
			@PathVariable Long serviceId) {
		try {
			facade.deleteAiModelService(appId, serviceId);
			return TinyEngineWebResponses.ok(Boolean.TRUE);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/{appId}/ai-model-services/{serviceId}/models")
	public ResponseEntity<? extends Result<?>> addAiModel(@PathVariable Integer appId, @PathVariable Long serviceId,
			@RequestBody AiModelRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.addAiModel(appId, serviceId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/app-center/api/apps/{appId}/ai-model-services/{serviceId}/models/{modelId}")
	public ResponseEntity<? extends Result<?>> updateAiModel(@PathVariable Integer appId, @PathVariable Long serviceId,
			@PathVariable Long modelId, @RequestBody AiModelRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.updateAiModel(appId, serviceId, modelId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@DeleteMapping("/app-center/api/apps/{appId}/ai-model-services/{serviceId}/models/{modelId}")
	public ResponseEntity<? extends Result<?>> deleteAiModel(@PathVariable Integer appId, @PathVariable Long serviceId,
			@PathVariable Long modelId) {
		try {
			facade.deleteAiModel(appId, serviceId, modelId);
			return TinyEngineWebResponses.ok(Boolean.TRUE);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/{appId}/ai-model-services/import-local")
	public ResponseEntity<? extends Result<?>> importAiModelServices(@PathVariable Integer appId,
			@RequestBody AiModelImportRequest request) {
		try {
			return TinyEngineWebResponses.ok(facade.importAiModelServices(appId, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/save/{id}")
	public ResponseEntity<? extends Result<?>> saveApp(@PathVariable Integer id,
			@RequestParam Map<String, Object> request) {
		try {
			return TinyEngineWebResponses.ok(facade.saveApp(id, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/publish/{id}")
	public ResponseEntity<? extends Result<?>> publishApp(@PathVariable Integer id,
			@RequestBody(required = false) Map<String, Object> request) {
		try {
			return TinyEngineWebResponses.ok(facade.publishApp(id, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/preview/{id}")
	public ResponseEntity<? extends Result<?>> previewApp(@PathVariable Integer id,
			@RequestParam Map<String, Object> request) {
		try {
			return TinyEngineWebResponses.ok(facade.previewApp(id, request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/extension/list")
	public ResponseEntity<? extends Result<?>> appExtensions(@RequestParam Integer app,
			@RequestParam String category) {
		try {
			return TinyEngineWebResponses.ok(facade.appExtensions(app, category));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/extension/create")
	public ResponseEntity<? extends Result<?>> createAppExtension(@RequestBody AppExtension extension) {
		try {
			return TinyEngineWebResponses.ok(facade.createAppExtension(extension));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/app-center/api/apps/extension/update")
	public ResponseEntity<? extends Result<?>> updateAppExtension(@RequestBody AppExtension extension) {
		try {
			return TinyEngineWebResponses.ok(facade.updateAppExtension(extension));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/extension/delete")
	public ResponseEntity<? extends Result<?>> deleteAppExtension(@RequestParam Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteAppExtension(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/app-center/api/apps/canvas/lock")
	public ResponseEntity<? extends Result<?>> lockCanvas(@RequestParam Integer id,
			@RequestParam String state, @RequestParam String type) {
		try {
			Map<String, Object> request = new java.util.LinkedHashMap<String, Object>();
			request.put("state", state);
			request.put("type", type);
			return TinyEngineWebResponses.ok(facade.lockCanvas(id, request));
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

	@GetMapping({"/app-center/v1/api/apps/schema/{id}", "/app-center/api/apps/schema/{id}"})
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

	@GetMapping("/app-center/api/apps/schema/components/{id}")
	public ResponseEntity<? extends Result<?>> appSchemaComponents(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.appSchemaComponents(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

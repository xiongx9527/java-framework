package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineDynamicModelMvcController.DeleteRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineDynamicModelMvcController.InsertRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineDynamicModelMvcController.QueryRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineDynamicModelMvcController.UpdateRequest;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsertResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/platform-center/api/model-data")
public class TinyEngineDynamicModelWebFluxController {

	private final DynamicService dynamicService;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineDynamicModelWebFluxController(DynamicService dynamicService, TinyEngineReactiveExecutor executor) {
		this.dynamicService = dynamicService;
		this.executor = executor;
	}

	@PostMapping("/queryApi")
	public Mono<ResponseEntity<? extends Result<?>>> query(@RequestBody QueryRequest request) {
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			try {
				requireModelName(request == null ? null : request.getNameEn());
				DynamicQuery query = new DynamicQuery();
				query.setTableName(request.getNameEn());
				query.setFields(request.getFields());
				query.setConditions(request.getParams());
				query.setCurrentPage(request.getCurrentPage());
				query.setPageSize(request.getPageSize());
				query.setOrderBy(request.getOrderBy());
				query.setOrderType(request.getOrderType());
				DynamicPageResult page = dynamicService.query(query);
				Map<String, Object> data = new LinkedHashMap<String, Object>();
				data.put("list", page.getList());
				data.put("total", Long.valueOf(page.getTotal()));
				data.put("pageNum", Integer.valueOf(page.getCurrentPage()));
				data.put("pageSize", Integer.valueOf(page.getPageSize()));
				data.put("pages", Long.valueOf(page.getPages()));
				return TinyEngineWebResponses.ok(data);
			} catch (IllegalArgumentException ex) {
				return badRequest(ex);
			} catch (TinyEngineSqlIdentifierException ex) {
				return badRequest(ex);
			}
		});
	}

	@PostMapping("/insertApi")
	public Mono<ResponseEntity<? extends Result<?>>> insert(@RequestBody InsertRequest request) {
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			try {
				requireModelName(request == null ? null : request.getNameEn());
				DynamicInsertResult inserted = dynamicService.insert(new DynamicInsert(request.getNameEn(),
						request.getParams()));
				Map<String, Object> data = new LinkedHashMap<String, Object>();
				data.put("insert", Integer.valueOf(inserted.getInsert()));
				data.put("id", inserted.getId());
				return TinyEngineWebResponses.ok(data);
			} catch (IllegalArgumentException ex) {
				return badRequest(ex);
			} catch (TinyEngineSqlIdentifierException ex) {
				return badRequest(ex);
			}
		});
	}

	@PostMapping("/updateApi")
	public Mono<ResponseEntity<? extends Result<?>>> update(@RequestBody UpdateRequest request) {
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			try {
				requireModelName(request == null ? null : request.getNameEn());
				int updated = dynamicService.update(new DynamicUpdate(request.getNameEn(), request.getData(),
						request.getParams()));
				return TinyEngineWebResponses.ok(singleValue("update", Integer.valueOf(updated)));
			} catch (IllegalArgumentException ex) {
				return badRequest(ex);
			} catch (TinyEngineSqlIdentifierException ex) {
				return badRequest(ex);
			}
		});
	}

	@PostMapping("/deleteApi")
	public Mono<ResponseEntity<? extends Result<?>>> delete(@RequestBody DeleteRequest request) {
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			try {
				requireModelName(request == null ? null : request.getNameEn());
				if (request.getId() == null) {
					throw new IllegalArgumentException("Dynamic delete id must not be null");
				}
				Map<String, Object> conditions = new LinkedHashMap<String, Object>();
				conditions.put("id", request.getId());
				int deleted = dynamicService.delete(new DynamicDelete(request.getNameEn(), conditions));
				return TinyEngineWebResponses.ok(singleValue("delete", Integer.valueOf(deleted)));
			} catch (IllegalArgumentException ex) {
				return badRequest(ex);
			} catch (TinyEngineSqlIdentifierException ex) {
				return badRequest(ex);
			}
		});
	}

	private ResponseEntity<Result<Object>> badRequest(RuntimeException ex) {
		return ResponseEntity.badRequest().body(Result.failed("400", ex.getMessage()));
	}

	private Map<String, Object> singleValue(String name, Object value) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put(name, value);
		return result;
	}

	private void requireModelName(String nameEn) {
		if (nameEn == null || nameEn.trim().isEmpty()) {
			throw new IllegalArgumentException("Dynamic model nameEn must not be empty");
		}
	}
}

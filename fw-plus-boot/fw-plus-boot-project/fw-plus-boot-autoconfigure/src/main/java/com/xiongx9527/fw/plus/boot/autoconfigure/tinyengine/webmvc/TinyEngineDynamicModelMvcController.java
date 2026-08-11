package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/platform-center/api/model-data")
public class TinyEngineDynamicModelMvcController {

	private final DynamicService dynamicService;

	public TinyEngineDynamicModelMvcController(DynamicService dynamicService) {
		this.dynamicService = dynamicService;
	}

	@PostMapping("/queryApi")
	public ResponseEntity<? extends Result<?>> query(@RequestBody QueryRequest request) {
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
		} catch (RuntimeException ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/insertApi")
	public ResponseEntity<? extends Result<?>> insert(@RequestBody InsertRequest request) {
		try {
			requireModelName(request == null ? null : request.getNameEn());
			DynamicInsertResult inserted = dynamicService.insert(new DynamicInsert(request.getNameEn(), request.getParams()));
			Map<String, Object> data = new LinkedHashMap<String, Object>();
			data.put("insert", Integer.valueOf(inserted.getInsert()));
			data.put("id", inserted.getId());
			return TinyEngineWebResponses.ok(data);
		} catch (IllegalArgumentException ex) {
			return badRequest(ex);
		} catch (TinyEngineSqlIdentifierException ex) {
			return badRequest(ex);
		} catch (RuntimeException ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/updateApi")
	public ResponseEntity<? extends Result<?>> update(@RequestBody UpdateRequest request) {
		try {
			requireModelName(request == null ? null : request.getNameEn());
			int updated = dynamicService.update(new DynamicUpdate(request.getNameEn(), request.getData(), request.getParams()));
			return TinyEngineWebResponses.ok(singleValue("update", Integer.valueOf(updated)));
		} catch (IllegalArgumentException ex) {
			return badRequest(ex);
		} catch (TinyEngineSqlIdentifierException ex) {
			return badRequest(ex);
		} catch (RuntimeException ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/deleteApi")
	public ResponseEntity<? extends Result<?>> delete(@RequestBody DeleteRequest request) {
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
		} catch (RuntimeException ex) {
			return TinyEngineWebResponses.error(ex);
		}
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

	public static class QueryRequest {
		private String nameEn;
		private String nameCh;
		private List<String> fields = new ArrayList<String>();
		private Map<String, Object> params = new LinkedHashMap<String, Object>();
		private Integer currentPage = Integer.valueOf(1);
		private Integer pageSize = Integer.valueOf(10);
		private String orderBy;
		private String orderType = "ASC";
		public String getNameEn() { return nameEn; }
		public void setNameEn(String nameEn) { this.nameEn = nameEn; }
		public String getNameCh() { return nameCh; }
		public void setNameCh(String nameCh) { this.nameCh = nameCh; }
		public List<String> getFields() { return fields; }
		public void setFields(List<String> fields) { this.fields = fields == null ? new ArrayList<String>() : fields; }
		public Map<String, Object> getParams() { return params; }
		public void setParams(Map<String, Object> params) { this.params = params == null ? new LinkedHashMap<String, Object>() : params; }
		public Integer getCurrentPage() { return currentPage; }
		public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
		public Integer getPageSize() { return pageSize; }
		public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
		public String getOrderBy() { return orderBy; }
		public void setOrderBy(String orderBy) { this.orderBy = orderBy; }
		public String getOrderType() { return orderType; }
		public void setOrderType(String orderType) { this.orderType = orderType; }
	}

	public static class InsertRequest {
		private String nameEn;
		private Map<String, Object> params = new LinkedHashMap<String, Object>();
		public String getNameEn() { return nameEn; }
		public void setNameEn(String nameEn) { this.nameEn = nameEn; }
		public Map<String, Object> getParams() { return params; }
		public void setParams(Map<String, Object> params) { this.params = params == null ? new LinkedHashMap<String, Object>() : params; }
	}

	public static class UpdateRequest {
		private String nameEn;
		private Map<String, Object> data = new LinkedHashMap<String, Object>();
		private Map<String, Object> params = new LinkedHashMap<String, Object>();
		public String getNameEn() { return nameEn; }
		public void setNameEn(String nameEn) { this.nameEn = nameEn; }
		public Map<String, Object> getData() { return data; }
		public void setData(Map<String, Object> data) { this.data = data == null ? new LinkedHashMap<String, Object>() : data; }
		public Map<String, Object> getParams() { return params; }
		public void setParams(Map<String, Object> params) { this.params = params == null ? new LinkedHashMap<String, Object>() : params; }
	}

	public static class DeleteRequest {
		private String nameEn;
		private Integer id;
		public String getNameEn() { return nameEn; }
		public void setNameEn(String nameEn) { this.nameEn = nameEn; }
		public Integer getId() { return id; }
		public void setId(Integer id) { this.id = id; }
	}
}

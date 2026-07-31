package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEnginePageMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEnginePageMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@PostMapping("/pages/create")
	public ResponseEntity<? extends Result<?>> createPage(@RequestBody Page page) {
		try {
			return TinyEngineWebResponses.ok(facade.createPage(page));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/pages/update/{id}")
	public ResponseEntity<? extends Result<?>> updatePage(@PathVariable Integer id, @RequestBody Page page) {
		try {
			return TinyEngineWebResponses.ok(facade.updatePage(id, page));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/pages/list/{appId}")
	public ResponseEntity<? extends Result<?>> pages(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.pages(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/pages/detail/{id}")
	public ResponseEntity<? extends Result<?>> pageDetail(@PathVariable Integer id) {
		try {
			Page page = facade.pageDetail(id);
			if (page == null) {
				return TinyEngineWebResponses.notFound("TinyEngine page not found: " + id);
			}
			return TinyEngineWebResponses.ok(page);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

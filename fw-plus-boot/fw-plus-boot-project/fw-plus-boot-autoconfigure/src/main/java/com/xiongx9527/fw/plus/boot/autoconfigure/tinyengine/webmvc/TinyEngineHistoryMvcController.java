package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
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
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEngineHistoryMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEngineHistoryMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@GetMapping("/pages/histories")
	public ResponseEntity<? extends Result<?>> pageHistories(@RequestParam("page") Integer pageId) {
		try {
			return TinyEngineWebResponses.ok(facade.pageHistories(pageId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping({"/pages/histories/{id}", "/pages/histories/detail/{id}"})
	public ResponseEntity<? extends Result<?>> pageHistoryDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.pageHistoryDetail(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/pageHistory/restore")
	public ResponseEntity<? extends Result<?>> restorePageHistory(@RequestBody Map<String, Object> request) {
		try {
			return TinyEngineWebResponses.ok(facade.restorePageHistory(request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEngineTaskMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEngineTaskMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@GetMapping("/tasks/status/{id}")
	public ResponseEntity<? extends Result<?>> taskStatus(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.taskStatus(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

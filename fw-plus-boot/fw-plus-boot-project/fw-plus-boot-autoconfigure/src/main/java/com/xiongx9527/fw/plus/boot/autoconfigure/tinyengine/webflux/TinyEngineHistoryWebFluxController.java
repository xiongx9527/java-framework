package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEngineHistoryWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineHistoryWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@GetMapping("/pages/histories")
	public Mono<ResponseEntity<? extends Result<?>>> pageHistories(@RequestParam("page") Integer pageId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.pageHistories(pageId));
	}

	@GetMapping({"/pages/histories/{id}", "/pages/histories/detail/{id}"})
	public Mono<ResponseEntity<? extends Result<?>>> pageHistoryDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.pageHistoryDetail(id));
	}

	@PostMapping("/pageHistory/restore")
	public Mono<ResponseEntity<? extends Result<?>>> restorePageHistory(@RequestBody Map<String, Object> request) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.restorePageHistory(request));
	}
}

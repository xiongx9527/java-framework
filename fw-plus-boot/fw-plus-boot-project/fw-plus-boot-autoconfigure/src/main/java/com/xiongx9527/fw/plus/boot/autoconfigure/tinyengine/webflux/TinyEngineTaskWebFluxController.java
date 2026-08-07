package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEngineTaskWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineTaskWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@GetMapping("/tasks/status/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> taskStatus(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.taskStatus(id));
	}
}

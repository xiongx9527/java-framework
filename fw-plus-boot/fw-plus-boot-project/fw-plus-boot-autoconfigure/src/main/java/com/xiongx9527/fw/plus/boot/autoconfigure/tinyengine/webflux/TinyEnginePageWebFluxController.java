package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEnginePageWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEnginePageWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@PostMapping("/pages/create")
	public Mono<ResponseEntity<? extends Result<?>>> createPage(@RequestBody Page page) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createPage(page));
	}

	@PostMapping("/pages/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updatePage(@PathVariable Integer id, @RequestBody Page page) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updatePage(id, page));
	}

	@GetMapping("/pages/list/{appId}")
	public Mono<ResponseEntity<? extends Result<?>>> pages(@PathVariable Integer appId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.pages(appId));
	}

	@GetMapping("/pages/detail/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> pageDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.pageDetail(id));
	}
}

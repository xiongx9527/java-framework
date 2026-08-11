package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api/i18n")
public class TinyEngineI18nWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineI18nWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@PostMapping("/entries/bulk/delete")
	public Mono<ResponseEntity<? extends Result<?>>> bulkDelete(@RequestBody DeleteI18nEntry request) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.bulkDeleteI18nEntries(request));
	}
}

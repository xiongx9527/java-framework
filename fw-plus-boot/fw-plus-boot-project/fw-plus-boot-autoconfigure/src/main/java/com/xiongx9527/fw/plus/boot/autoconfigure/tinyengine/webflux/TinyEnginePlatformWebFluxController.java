package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/platform-center/api")
public class TinyEnginePlatformWebFluxController {

	private final TinyEngineExternalCapability externalCapability;
	private final TinyEngineReactiveExecutor executor;

	public TinyEnginePlatformWebFluxController(TinyEngineExternalCapability externalCapability,
			TinyEngineReactiveExecutor executor) {
		this.externalCapability = externalCapability;
		this.executor = executor;
	}

	@GetMapping("/courses")
	public Mono<ResponseEntity<? extends Result<?>>> courses(@RequestParam Map<String, Object> query) {
		return TinyEngineWebFluxSupport.submit(executor, () -> externalCapability.courses(query));
	}

	@GetMapping("/courses/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> courseDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> externalCapability.courseDetail(id));
	}
}

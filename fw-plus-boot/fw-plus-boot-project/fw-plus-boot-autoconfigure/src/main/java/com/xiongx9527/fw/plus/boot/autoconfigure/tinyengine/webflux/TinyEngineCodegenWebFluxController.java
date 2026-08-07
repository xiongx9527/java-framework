package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineCodeQuery;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaCodeParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api")
public class TinyEngineCodegenWebFluxController {

	private final TinyEngineExternalCapability externalCapability;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineCodegenWebFluxController(TinyEngineExternalCapability externalCapability,
			TinyEngineReactiveExecutor executor) {
		this.externalCapability = externalCapability;
		this.executor = executor;
	}

	@PostMapping("/schema2code")
	public Mono<ResponseEntity<? extends Result<?>>> schemaToCode(@RequestBody SchemaCodeParam request) {
		return TinyEngineWebFluxSupport.submit(executor, () -> externalCapability.schemaToCode(request));
	}

	@GetMapping("/code")
	public Mono<ResponseEntity<? extends Result<?>>> generateCode(@ModelAttribute TinyEngineCodeQuery query) {
		return TinyEngineWebFluxSupport.submit(executor, () -> externalCapability.generateCode(toSchemaCodeParam(query)));
	}

	@GetMapping("/source_tpl")
	public Mono<ResponseEntity<? extends Result<?>>> sourceTemplate(@RequestParam Map<String, Object> query) {
		if (!hasExactlyOneSourceTemplateSelector(query)) {
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Result.failed("400", "Exactly one of platform or id is required")));
		}
		return TinyEngineWebFluxSupport.submit(executor, () -> externalCapability.sourceTemplate(query));
	}

	private static SchemaCodeParam toSchemaCodeParam(TinyEngineCodeQuery query) {
		SchemaCodeParam request = new SchemaCodeParam();
		request.setPlatform(query.getPlatform());
		request.setApp(query.getApp());
		request.setType(query.getType());
		request.setId(query.getId());
		request.setHistory(query.getHistory());
		return request;
	}

	private static boolean hasExactlyOneSourceTemplateSelector(Map<String, Object> query) {
		boolean hasPlatform = hasText(query.get("platform"));
		boolean hasId = hasText(query.get("id"));
		return hasPlatform != hasId;
	}

	private static boolean hasText(Object value) {
		return value != null && value.toString().trim().length() > 0;
	}
}

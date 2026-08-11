package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/platform-center/api")
public class TinyEnginePlatformMvcController {

	private final TinyEngineExternalCapability externalCapability;

	public TinyEnginePlatformMvcController(TinyEngineExternalCapability externalCapability) {
		this.externalCapability = externalCapability;
	}

	@GetMapping("/courses")
	public ResponseEntity<? extends Result<?>> courses(@RequestParam Map<String, Object> query) {
		try {
			return TinyEngineWebResponses.ok(externalCapability.courses(query));
		} catch (RuntimeException ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/courses/{id}")
	public ResponseEntity<? extends Result<?>> courseDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(externalCapability.courseDetail(id));
		} catch (RuntimeException ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/app-center/api/i18n")
public class TinyEngineI18nMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEngineI18nMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@PostMapping("/entries/bulk/delete")
	public ResponseEntity<? extends Result<?>> bulkDelete(@RequestBody DeleteI18nEntry request) {
		try {
			return TinyEngineWebResponses.ok(facade.bulkDeleteI18nEntries(request));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

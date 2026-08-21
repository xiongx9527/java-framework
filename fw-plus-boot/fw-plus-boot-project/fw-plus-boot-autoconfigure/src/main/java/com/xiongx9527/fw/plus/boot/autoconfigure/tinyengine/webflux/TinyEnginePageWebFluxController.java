package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PreviewParam;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
		return TinyEngineWebFluxSupport.submitResponse(executor, () -> {
			Page page = facade.pageDetail(id);
			if (page == null) {
				return TinyEngineWebResponses.notFound("TinyEngine page not found: " + id);
			}
			return TinyEngineWebResponses.ok(page);
		});
	}

	@GetMapping("/pages/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deletePage(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deletePage(id));
	}

	@PostMapping("/pages/copy")
	public Mono<ResponseEntity<? extends Result<?>>> copyPage(@RequestBody Page page) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.copyPage(page));
	}

	@GetMapping("/preview/metadata")
	public Mono<ResponseEntity<? extends Result<?>>> previewMetadata(@ModelAttribute PreviewParam previewParam) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.previewMetadata(previewParam));
	}
}

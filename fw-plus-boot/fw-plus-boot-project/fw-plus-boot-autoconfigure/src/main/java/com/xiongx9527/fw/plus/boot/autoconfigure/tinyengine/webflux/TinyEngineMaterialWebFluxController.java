package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/material-center/api")
public class TinyEngineMaterialWebFluxController {

	private final TinyEngineApiFacade facade;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineMaterialWebFluxController(TinyEngineApiFacade facade, TinyEngineReactiveExecutor executor) {
		this.facade = facade;
		this.executor = executor;
	}

	@GetMapping("/blocks")
	public Mono<ResponseEntity<? extends Result<?>>> blocks(@RequestParam(required = false) String appId,
			@RequestParam(required = false) String groupId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.blocks(appId, groupId));
	}

	@GetMapping("/block")
	public Mono<ResponseEntity<? extends Result<?>>> blockList(@ModelAttribute BlockParamDto blockParamDto) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.blockList(blockParamDto));
	}

	@GetMapping("/block/detail/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> blockDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.blockDetail(id));
	}

	@PostMapping("/block/create")
	public Mono<ResponseEntity<? extends Result<?>>> createBlock(@RequestBody BlockParam blockParam) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createBlock(blockParam));
	}

	@PostMapping("/block/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updateBlock(@PathVariable Integer id,
			@RequestBody BlockParam blockParam) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateBlock(id, blockParam));
	}

	@GetMapping("/block/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deleteBlock(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteBlock(id));
	}

	@PostMapping("/block/deploy")
	public Mono<ResponseEntity<? extends Result<?>>> deployBlock(@RequestBody BlockBuildDto blockBuildDto) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deployBlock(blockBuildDto));
	}

	@GetMapping("/block/tags")
	public Mono<ResponseEntity<? extends Result<?>>> blockTags() {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.blockTags());
	}

	@GetMapping("/block/notgroup/{groupId}")
	public Mono<ResponseEntity<? extends Result<?>>> blocksNotInGroup(@PathVariable Integer groupId,
			@RequestParam(value = "label_contains", required = false) String label,
			@RequestParam(value = "name_cn_contains", required = false) String name,
			@RequestParam(value = "tags_contains", required = false) String[] tags,
			@RequestParam(value = "createdBy", required = false) String createdBy) {
		return TinyEngineWebFluxSupport.submit(executor,
				() -> facade.blocksNotInGroup(groupId, label, name, tags, createdBy));
	}

	@GetMapping({"/block-groups", "/block-categories"})
	public Mono<ResponseEntity<? extends Result<?>>> blockGroups(
			@RequestParam(value = "id", required = false) List<Integer> ids,
			@RequestParam(value = "app", required = false) Integer appId,
			@RequestParam(value = "from", required = false) String from) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.blockGroups(ids, appId, from));
	}

	@PostMapping({"/block-groups/create", "/block-categories"})
	public Mono<ResponseEntity<? extends Result<?>>> createBlockGroup(@RequestBody BlockGroup blockGroup) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createBlockGroup(blockGroup));
	}

	@RequestMapping(path = {"/block-groups/update/{id}", "/block-categories/{id}"},
			method = {RequestMethod.POST, RequestMethod.PUT})
	public Mono<ResponseEntity<? extends Result<?>>> updateBlockGroup(@PathVariable Integer id,
			@RequestBody BlockGroup blockGroup) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateBlockGroup(id, blockGroup));
	}

	@RequestMapping(path = {"/block-groups/delete/{id}", "/block-categories/{id}"},
			method = {RequestMethod.GET, RequestMethod.DELETE})
	public Mono<ResponseEntity<? extends Result<?>>> deleteBlockGroup(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteBlockGroup(id));
	}

	@GetMapping("/business-category/list")
	public Mono<ResponseEntity<? extends Result<?>>> businessCategories() {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.businessCategories());
	}

	@GetMapping("/business-category/find")
	public Mono<ResponseEntity<? extends Result<?>>> businessCategoriesByGroup(@RequestParam String group) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.businessCategoriesByGroup(group));
	}

	@GetMapping("/resource-group/list")
	public Mono<ResponseEntity<? extends Result<?>>> resourceGroups() {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.resourceGroups());
	}

	@GetMapping("/resource-group/{appId}")
	public Mono<ResponseEntity<? extends Result<?>>> resourceGroupsByApp(@PathVariable Integer appId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.resourceGroupsByApp(appId));
	}

	@PostMapping("/resource-group/create")
	public Mono<ResponseEntity<? extends Result<?>>> createResourceGroup(@RequestBody ResourceGroup resourceGroup) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createResourceGroup(resourceGroup));
	}

	@PutMapping("/resource-group/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updateResourceGroup(@PathVariable Integer id,
			@RequestBody ResourceGroup resourceGroup) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateResourceGroup(id, resourceGroup));
	}

	@DeleteMapping("/resource-group/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deleteResourceGroup(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteResourceGroup(id));
	}

	@GetMapping("/resource-group/detail/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> resourceGroupDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.resourceGroupDetail(id));
	}

	@GetMapping("/resource/list")
	public Mono<ResponseEntity<? extends Result<?>>> resources() {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.resources());
	}

	@GetMapping("/resource/find/{resourceGroupId}")
	public Mono<ResponseEntity<? extends Result<?>>> resourcesByGroup(@PathVariable Integer resourceGroupId) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.resourcesByGroup(resourceGroupId));
	}

	@PostMapping("/resource/create")
	public Mono<ResponseEntity<? extends Result<?>>> createResource(@RequestBody Resource resource) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.createResource(resource));
	}

	@PutMapping("/resource/update/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> updateResource(@PathVariable Integer id,
			@RequestBody Resource resource) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.updateResource(id, resource));
	}

	@DeleteMapping("/resource/delete/{id}")
	public Mono<ResponseEntity<? extends Result<?>>> deleteResource(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.deleteResource(id));
	}

	@GetMapping({"/resource/{id}", "/resource/detail/{id}"})
	public Mono<ResponseEntity<? extends Result<?>>> resourceDetail(@PathVariable Integer id) {
		return TinyEngineWebFluxSupport.submit(executor, () -> facade.resourceDetail(id));
	}
}

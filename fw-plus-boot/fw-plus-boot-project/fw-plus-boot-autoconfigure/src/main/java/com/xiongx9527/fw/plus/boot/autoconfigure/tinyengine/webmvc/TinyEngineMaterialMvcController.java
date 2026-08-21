package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.common.util.TinyEngineFileSecurityUtils;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ResourceRequestDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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

import java.util.List;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/material-center/api")
public class TinyEngineMaterialMvcController {

	private final TinyEngineApiFacade facade;

	public TinyEngineMaterialMvcController(TinyEngineApiFacade facade) {
		this.facade = facade;
	}

	@GetMapping("/blocks")
	public ResponseEntity<? extends Result<?>> blocks(@RequestParam(required = false) String appId,
			@RequestParam(required = false) String groupId) {
		try {
			return TinyEngineWebResponses.ok(facade.blocks(appId, groupId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block")
	public ResponseEntity<? extends Result<?>> blockList(@ModelAttribute BlockParamDto blockParamDto) {
		try {
			return TinyEngineWebResponses.ok(facade.blockList(blockParamDto));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block/detail/{id}")
	public ResponseEntity<? extends Result<?>> blockDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.blockDetail(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block-history")
	public ResponseEntity<? extends Result<?>> blockHistories(@RequestParam("block") Integer blockId) {
		try {
			return TinyEngineWebResponses.ok(facade.blockHistories(blockId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block/tenants")
	public ResponseEntity<? extends Result<?>> blockTenants() {
		try {
			return TinyEngineWebResponses.ok(facade.blockTenants());
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block/users")
	public ResponseEntity<? extends Result<?>> blockUsers() {
		try {
			return TinyEngineWebResponses.ok(facade.blockUsers());
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/block/create")
	public ResponseEntity<? extends Result<?>> createBlock(@RequestBody BlockParam blockParam) {
		try {
			return TinyEngineWebResponses.ok(facade.createBlock(blockParam));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/block/update/{id}")
	public ResponseEntity<? extends Result<?>> updateBlock(@PathVariable Integer id, @RequestBody BlockParam blockParam) {
		try {
			return TinyEngineWebResponses.ok(facade.updateBlock(id, blockParam));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block/delete/{id}")
	public ResponseEntity<? extends Result<?>> deleteBlock(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteBlock(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/block/deploy")
	public ResponseEntity<? extends Result<?>> deployBlock(@RequestBody BlockBuildDto blockBuildDto) {
		try {
			return TinyEngineWebResponses.ok(facade.deployBlock(blockBuildDto));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block/tags")
	public ResponseEntity<? extends Result<?>> blockTags() {
		try {
			return TinyEngineWebResponses.ok(facade.blockTags());
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/block/notgroup/{groupId}")
	public ResponseEntity<? extends Result<?>> blocksNotInGroup(@PathVariable Integer groupId,
			@RequestParam(value = "label_contains", required = false) String label,
			@RequestParam(value = "name_cn_contains", required = false) String name,
			@RequestParam(value = "tags_contains", required = false) String[] tags,
			@RequestParam(value = "createdBy", required = false) String createdBy) {
		try {
			return TinyEngineWebResponses.ok(facade.blocksNotInGroup(groupId, label, name, tags, createdBy));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping({"/block-groups", "/block-categories"})
	public ResponseEntity<? extends Result<?>> blockGroups(
			@RequestParam(value = "id", required = false) List<Integer> ids,
			@RequestParam(value = "app", required = false) Integer appId,
			@RequestParam(value = "from", required = false) String from) {
		try {
			return TinyEngineWebResponses.ok(facade.blockGroups(ids, appId, from));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping({"/block-groups/create", "/block-categories"})
	public ResponseEntity<? extends Result<?>> createBlockGroup(@RequestBody BlockGroup blockGroup) {
		try {
			return TinyEngineWebResponses.ok(facade.createBlockGroup(blockGroup));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@RequestMapping(path = {"/block-groups/update/{id}", "/block-categories/{id}"},
			method = {RequestMethod.POST, RequestMethod.PUT})
	public ResponseEntity<? extends Result<?>> updateBlockGroup(@PathVariable Integer id,
			@RequestBody BlockGroup blockGroup) {
		try {
			return TinyEngineWebResponses.ok(facade.updateBlockGroup(id, blockGroup));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@RequestMapping(path = {"/block-groups/delete/{id}", "/block-categories/{id}"},
			method = {RequestMethod.GET, RequestMethod.DELETE})
	public ResponseEntity<? extends Result<?>> deleteBlockGroup(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteBlockGroup(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/business-category/list")
	public ResponseEntity<? extends Result<?>> businessCategories() {
		try {
			return TinyEngineWebResponses.ok(facade.businessCategories());
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/business-category/find")
	public ResponseEntity<? extends Result<?>> businessCategoriesByGroup(@RequestParam String group) {
		try {
			return TinyEngineWebResponses.ok(facade.businessCategoriesByGroup(group));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/resource-group/list")
	public ResponseEntity<? extends Result<?>> resourceGroups() {
		try {
			return TinyEngineWebResponses.ok(facade.resourceGroups());
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/resource-group/{appId}")
	public ResponseEntity<? extends Result<?>> resourceGroupsByApp(@PathVariable Integer appId) {
		try {
			return TinyEngineWebResponses.ok(facade.resourceGroupsByApp(appId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/resource-group/create")
	public ResponseEntity<? extends Result<?>> createResourceGroup(@RequestBody ResourceGroup resourceGroup) {
		try {
			return TinyEngineWebResponses.ok(facade.createResourceGroup(resourceGroup));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/resource-group/update/{id}")
	public ResponseEntity<? extends Result<?>> updateResourceGroup(@PathVariable Integer id,
			@RequestBody ResourceGroup resourceGroup) {
		try {
			return TinyEngineWebResponses.ok(facade.updateResourceGroup(id, resourceGroup));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@DeleteMapping("/resource-group/delete/{id}")
	public ResponseEntity<? extends Result<?>> deleteResourceGroup(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteResourceGroup(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/resource-group/detail/{id}")
	public ResponseEntity<? extends Result<?>> resourceGroupDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.resourceGroupDetail(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/resource/list")
	public ResponseEntity<? extends Result<?>> resources() {
		try {
			return TinyEngineWebResponses.ok(facade.resources());
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/resource/find/{resourceGroupId}")
	public ResponseEntity<? extends Result<?>> resourcesByGroup(@PathVariable Integer resourceGroupId) {
		try {
			return TinyEngineWebResponses.ok(facade.resourcesByGroup(resourceGroupId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/resource/create")
	public ResponseEntity<? extends Result<?>> createResource(@RequestBody Resource resource) {
		try {
			return TinyEngineWebResponses.ok(facade.createResource(resource));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/resource/create/batch")
	public ResponseEntity<? extends Result<?>> createResources(@RequestBody List<Resource> resources) {
		try {
			return TinyEngineWebResponses.ok(facade.createResources(resources));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping(value = "/resource/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<? extends Result<?>> uploadResource(@RequestParam("file") MultipartFile file,
			@RequestParam(required = false) Integer appId) {
		try {
			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("Resource upload file is required");
			}
			String fileName = file.getOriginalFilename();
			TinyEngineFileSecurityUtils.validateRelativePath(fileName);
			ResourceRequestDto request = new ResourceRequestDto();
			request.setName(fileName);
			request.setAppId(appId);
			request.setIsResource(true);
			return TinyEngineWebResponses.ok(facade.uploadResource(request));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Result.failed("400", ex.getMessage()));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PutMapping("/resource/update/{id}")
	public ResponseEntity<? extends Result<?>> updateResource(@PathVariable Integer id, @RequestBody Resource resource) {
		try {
			return TinyEngineWebResponses.ok(facade.updateResource(id, resource));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@DeleteMapping("/resource/delete/{id}")
	public ResponseEntity<? extends Result<?>> deleteResource(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.deleteResource(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping({"/resource/{id}", "/resource/detail/{id}"})
	public ResponseEntity<? extends Result<?>> resourceDetail(@PathVariable Integer id) {
		try {
			return TinyEngineWebResponses.ok(facade.resourceDetail(id));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}
}

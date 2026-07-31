package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.NotGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BusinessCategory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.AppTemplateService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockService;
import com.xiongx9527.fw.plus.tinyengine.service.BusinessCategoryService;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultTinyEngineApiFacade implements TinyEngineApiFacade {

	private final AppService appService;
	private final PageService pageService;
	private final BlockService blockService;
	private final BlockGroupService blockGroupService;
	private final BusinessCategoryService businessCategoryService;
	private final ModelService modelService;
	private final DatasourceService datasourceService;
	private final AppTemplateService appTemplateService;
	private final ResourceGroupService resourceGroupService;
	private final ResourceService resourceService;
	private final I18nEntryService i18nEntryService;
	private final TinyEngineExternalCapability externalCapability;

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService) {
		this(appService, pageService, null, null, null, null, null, null, null, null, null,
				new DefaultTinyEngineExternalCapability());
	}

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService, BlockService blockService,
			BlockGroupService blockGroupService, BusinessCategoryService businessCategoryService,
			ModelService modelService, DatasourceService datasourceService, AppTemplateService appTemplateService,
			ResourceGroupService resourceGroupService, ResourceService resourceService, I18nEntryService i18nEntryService,
			TinyEngineExternalCapability externalCapability) {
		this.appService = appService;
		this.pageService = pageService;
		this.blockService = blockService;
		this.blockGroupService = blockGroupService;
		this.businessCategoryService = businessCategoryService;
		this.modelService = modelService;
		this.datasourceService = datasourceService;
		this.appTemplateService = appTemplateService;
		this.resourceGroupService = resourceGroupService;
		this.resourceService = resourceService;
		this.i18nEntryService = i18nEntryService;
		this.externalCapability = externalCapability;
	}

	@Override
	public AppDto appsPage(Integer currentPage, Integer pageSize, String orderBy, App app) {
		return appService.queryAllAppByPage(currentPage, pageSize, orderBy, app);
	}

	@Override
	public App createApp(App app) {
		return appService.createApp(app);
	}

	@Override
	public App appDetail(Integer id) {
		return appService.queryAppById(id);
	}

	@Override
	public Map<String, Object> appSchema(Integer id) {
		App app = appDetail(id);
		if (app == null) {
			return null;
		}
		Map<String, Object> config = app.getConfig();
		if (config == null) {
			return Collections.emptyMap();
		}
		return config;
	}

	@Override
	public Page createPage(Page page) {
		if (Boolean.FALSE.equals(page.getIsPage())) {
			return pageService.createFolder(page);
		}
		return pageService.createPage(page);
	}

	@Override
	public Page updatePage(Integer id, Page page) {
		page.setId(id);
		page.setCreatedTime(null);
		page.setLastUpdatedTime(null);
		page.setLastUpdatedBy(null);
		if (Boolean.FALSE.equals(page.getIsPage())) {
			return pageService.update(page);
		}
		return pageService.updatePage(page);
	}

	@Override
	public List<Page> pages(Integer appId) {
		return pageService.queryAllPage(appId);
	}

	@Override
	public Page pageDetail(Integer id) {
		return pageService.queryPageById(id);
	}

	@Override
	public List<Block> blocks(String appId, String groupId) {
		return blockService.listNew(appId, groupId);
	}

	@Override
	public List<Block> blockList(BlockParamDto blockParamDto) {
		IPage<Block> page = blockService.findBlocksByPagetionList(blockParamDto);
		return page == null ? Collections.<Block>emptyList() : page.getRecords();
	}

	@Override
	public BlockDto blockDetail(Integer id) {
		return blockService.queryBlockById(id);
	}

	@Override
	public BlockDto createBlock(BlockParam blockParam) {
		return blockService.createBlock(blockParam);
	}

	@Override
	public BlockDto updateBlock(Integer id, BlockParam blockParam) {
		blockParam.setId(id);
		return blockService.updateBlockById(blockParam);
	}

	@Override
	public BlockDto deleteBlock(Integer id) {
		BlockDto block = blockService.queryBlockById(id);
		blockService.deleteBlockById(id);
		return block;
	}

	@Override
	public BlockDto deployBlock(BlockBuildDto blockBuildDto) {
		return blockService.deploy(blockBuildDto);
	}

	@Override
	public List<String> blockTags() {
		return blockService.allTags();
	}

	@Override
	public List<BlockDto> blocksNotInGroup(Integer groupId, String label, String name, String[] tags, String createdBy) {
		NotGroupDto dto = new NotGroupDto();
		dto.setGroupId(groupId);
		dto.setLabel(label);
		dto.setName(name);
		dto.setTags(tags == null ? null : String.join(",", Arrays.asList(tags)));
		dto.setCreatedBy(createdBy);
		return blockService.getNotInGroupBlocks(dto);
	}

	@Override
	public List<BlockGroup> blockGroups(List<Integer> ids, Integer appId, String from) {
		return blockGroupService.getBlockGroupByIdsOrAppId(ids, appId, from);
	}

	@Override
	public BlockGroup createBlockGroup(BlockGroup blockGroup) {
		return blockGroupService.createBlockGroup(blockGroup);
	}

	@Override
	public List<BlockGroup> updateBlockGroup(Integer id, BlockGroup blockGroup) {
		blockGroup.setId(id);
		blockGroupService.updateBlockGroupById(blockGroup);
		return singletonIfPresent(blockGroupService.findBlockGroupById(id));
	}

	@Override
	public List<BlockGroup> deleteBlockGroup(Integer id) {
		BlockGroup blockGroup = blockGroupService.findBlockGroupById(id);
		blockGroupService.deleteBlockGroupById(id);
		return singletonIfPresent(blockGroup);
	}

	@Override
	public List<BusinessCategory> businessCategories() {
		return businessCategoryService.queryAllBusinessCategory();
	}

	@Override
	public List<BusinessCategory> businessCategoriesByGroup(String group) {
		BusinessCategory category = new BusinessCategory();
		category.setBusinessGroup(group);
		return businessCategoryService.queryBusinessCategoryByCondition(category);
	}

	@Override
	public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Model> models(Integer currentPage, Integer pageSize,
			String nameCn, String nameEn) {
		return modelService.pageQuery(defaultNumber(currentPage, 1), defaultNumber(pageSize, 20), nameCn, nameEn);
	}

	@Override
	public Model modelDetail(Integer id) {
		return modelService.queryModelById(id);
	}

	@Override
	public Model createModel(Model model) {
		modelService.save(model);
		return model;
	}

	@Override
	public Model updateModel(Integer id, Model model) {
		model.setId(id);
		modelService.updateById(model);
		Model updated = modelService.queryModelById(id);
		return updated == null ? model : updated;
	}

	@Override
	public Model deleteModel(Integer id) {
		Model model = modelService.queryModelById(id);
		modelService.removeById(id);
		return model;
	}

	@Override
	public List<Datasource> datasources(Integer appId) {
		Datasource datasource = new Datasource();
		datasource.setApp(appId);
		return datasourceService.queryDatasourceByCondition(datasource);
	}

	@Override
	public Datasource datasourceDetail(Integer id) {
		return datasourceService.queryDatasourceById(id);
	}

	@Override
	public Datasource createDatasource(Datasource datasource) {
		return datasourceService.createDatasource(datasource);
	}

	@Override
	public Datasource updateDatasource(Integer id, Datasource datasource) {
		datasource.setId(id);
		return datasourceService.updateDatasourceById(datasource);
	}

	@Override
	public Datasource deleteDatasource(Integer id) {
		return datasourceService.deleteDatasourceById(id);
	}

	@Override
	public AppDto appTemplates(Integer currentPage, Integer pageSize, String orderBy, App app) {
		return appTemplateService.queryAllAppTemplate(currentPage, pageSize, orderBy, app);
	}

	@Override
	public App createAppTemplate(App app) {
		return appTemplateService.createAppByTemplate(app);
	}

	@Override
	public List<ResourceGroup> resourceGroups() {
		return resourceGroupService.queryAllResourceGroupAndResource();
	}

	@Override
	public List<ResourceGroup> resourceGroupsByApp(Integer appId) {
		return resourceGroupService.queryResourceGroupByAppId(appId);
	}

	@Override
	public ResourceGroup resourceGroupDetail(Integer id) {
		return resourceGroupService.queryResourceGroupById(id);
	}

	@Override
	public ResourceGroup createResourceGroup(ResourceGroup resourceGroup) {
		return resourceGroupService.createResourceGroup(resourceGroup);
	}

	@Override
	public ResourceGroup updateResourceGroup(Integer id, ResourceGroup resourceGroup) {
		resourceGroup.setId(id);
		return resourceGroupService.updateResourceGroupById(resourceGroup);
	}

	@Override
	public ResourceGroup deleteResourceGroup(Integer id) {
		return resourceGroupService.deleteResourceGroupById(id);
	}

	@Override
	public List<Resource> resources() {
		return resourceService.queryAllResource();
	}

	@Override
	public List<Resource> resourcesByGroup(Integer resourceGroupId) {
		return resourceService.queryResourceByResourceGroupId(resourceGroupId);
	}

	@Override
	public Resource resourceDetail(Integer id) {
		return resourceService.queryResourceById(id);
	}

	@Override
	public Resource createResource(Resource resource) {
		resourceService.save(resource);
		return resource;
	}

	@Override
	public Resource updateResource(Integer id, Resource resource) {
		resource.setId(id);
		return resourceService.updateResourceById(resource);
	}

	@Override
	public Resource deleteResource(Integer id) {
		return resourceService.deleteResourceById(id);
	}

	@Override
	public I18nEntryListResult i18nEntries(Integer host, String hostType) {
		if (host == null) {
			return i18nEntryService.findAllI18nEntry();
		}
		return i18nEntryService.findI18nEntryByApp(host, hostType);
	}

	@Override
	public List<I18nEntry> createI18nEntries(OperateI18nEntries entries) {
		return i18nEntryService.create(entries);
	}

	@Override
	public Object updateI18nEntriesFile(Integer appId) {
		return externalCapability.unsupported("i18n-entry-file-update");
	}

	private static int defaultNumber(Integer value, int defaultValue) {
		return value == null ? defaultValue : value.intValue();
	}

	private static <T> List<T> singletonIfPresent(T value) {
		if (value == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(value);
	}
}

package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.NotGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PreviewDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PreviewParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ResourceRequestDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaUtils;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BusinessCategory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;
import com.xiongx9527.fw.plus.tinyengine.service.AppExtensionService;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.AppTemplateService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockService;
import com.xiongx9527.fw.plus.tinyengine.service.BusinessCategoryService;
import com.xiongx9527.fw.plus.tinyengine.service.CanvasService;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.service.TaskRecordService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
	private final AppExtensionService appExtensionService;
	private final CanvasService canvasService;
	private final PageHistoryService pageHistoryService;
	private final TaskRecordService taskRecordService;
	private final TinyEngineExternalCapability externalCapability;

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService) {
		this(appService, pageService, null, null, null, null, null, null, null, null, null, null, null,
				null, null, new DefaultTinyEngineExternalCapability());
	}

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService, BlockService blockService,
			BlockGroupService blockGroupService, BusinessCategoryService businessCategoryService,
			ModelService modelService, DatasourceService datasourceService, AppTemplateService appTemplateService,
			ResourceGroupService resourceGroupService, ResourceService resourceService, I18nEntryService i18nEntryService,
			AppExtensionService appExtensionService, TinyEngineExternalCapability externalCapability) {
		this(appService, pageService, blockService, blockGroupService, businessCategoryService, modelService,
				datasourceService, appTemplateService, resourceGroupService, resourceService, i18nEntryService,
				appExtensionService, null, null, null, externalCapability);
	}

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService, BlockService blockService,
			BlockGroupService blockGroupService, BusinessCategoryService businessCategoryService,
			ModelService modelService, DatasourceService datasourceService, AppTemplateService appTemplateService,
			ResourceGroupService resourceGroupService, ResourceService resourceService, I18nEntryService i18nEntryService,
			AppExtensionService appExtensionService, CanvasService canvasService,
			TinyEngineExternalCapability externalCapability) {
		this(appService, pageService, blockService, blockGroupService, businessCategoryService, modelService,
				datasourceService, appTemplateService, resourceGroupService, resourceService, i18nEntryService,
				appExtensionService, canvasService, null, null, externalCapability);
	}

	public DefaultTinyEngineApiFacade(AppService appService, PageService pageService, BlockService blockService,
			BlockGroupService blockGroupService, BusinessCategoryService businessCategoryService,
			ModelService modelService, DatasourceService datasourceService, AppTemplateService appTemplateService,
			ResourceGroupService resourceGroupService, ResourceService resourceService, I18nEntryService i18nEntryService,
			AppExtensionService appExtensionService, CanvasService canvasService, PageHistoryService pageHistoryService,
			TaskRecordService taskRecordService, TinyEngineExternalCapability externalCapability) {
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
		this.appExtensionService = appExtensionService;
		this.canvasService = canvasService;
		this.pageHistoryService = pageHistoryService;
		this.taskRecordService = taskRecordService;
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
	public App updateApp(Integer id, App app) {
		app.setId(id);
		app.setTenantId(null);
		return appService.updateAppById(app);
	}

	@Override
	public App deleteApp(Integer id) {
		return appService.deleteAppById(id);
	}

	@Override
	public Object saveApp(Integer id, Map<String, Object> request) {
		return appService.saveApp(id, request);
	}

	@Override
	public Map<String, Object> publishApp(Integer id, Map<String, Object> request) {
		throw new TinyEngineUnsupportedCapabilityException("TinyEngine 应用发布能力未配置");
	}

	@Override
	public Object previewApp(Integer id, Map<String, Object> request) {
		return appService.requireEnabledApp(id);
	}

	@Override
	public App appDetail(Integer id) {
		return appService.requireEnabledApp(id);
	}

	@Override
	public Map<String, Object> appSchema(Integer id) {
		App app = appDetail(id);
		if (app == null) {
			return null;
		}
		Map<String, Object> schema = new LinkedHashMap<String, Object>();
		if (app.getConfig() != null) {
			schema.putAll(app.getConfig());
		}
		schema.put("componentsTree", toComponentTrees(pages(id), app.getHomePage()));
		schema.put("componentsMap", Collections.emptyList());
		schema.put("dataSource", appDataSource(id, app));

		I18nEntryListResult i18n = i18nEntryService == null
				? null : i18nEntryService.findI18nEntryByApp(id, "app");
		schema.put("i18n", normalizeI18n(i18n));

		List<AppExtension> extensions = appExtensionService == null
				? Collections.<AppExtension>emptyList() : appExtensions(id, null);
		schema.put("utils", toSchemaUtils(extensions, "utils"));
		schema.put("bridge", toSchemaUtils(extensions, "bridge"));
		schema.put("packages", Collections.emptyList());
		schema.put("meta", appMeta(app));
		schema.put("globalState", app.getGlobalState() == null
				? Collections.emptyList() : app.getGlobalState());
		schema.put("config", app.getConfig() == null
				? Collections.<String, Object>emptyMap() : app.getConfig());
		schema.put("constants", app.getConstants() == null ? "" : app.getConstants());
		schema.put("css", app.getCss() == null ? "" : app.getCss());
		schema.put("version", "");
		return schema;
	}

	@Override
	public Object appSchemaComponents(Integer id) {
		Map<String, Object> schema = appSchema(id);
		Map<String, Object> components = new LinkedHashMap<String, Object>();
		components.put("componentsTree", schema.get("componentsTree"));
		components.put("componentsMap", schema.get("componentsMap"));
		return components;
	}

	@Override
	public Object lockCanvas(Integer appId, Map<String, Object> request) {
		if (canvasService == null) {
			return externalCapability.unsupported("canvas-lock");
		}
		String state = request == null || request.get("state") == null
				? null : String.valueOf(request.get("state"));
		String type = request == null || request.get("type") == null
				? null : String.valueOf(request.get("type"));
		if ("page".equals(type)) {
			Page page = pageService.queryPageById(appId);
			if (page != null) {
				appService.requireEnabledApp(page.getApp());
			}
		} else {
			appService.requireEnabledApp(appId);
		}
		return canvasService.lockCanvas(appId, state, type);
	}

	@Override
	public List<AppExtension> appExtensions(Integer app, String category) {
		appService.requireEnabledApp(app);
		if (appExtensionService == null) {
			return Collections.emptyList();
		}
		AppExtension condition = new AppExtension();
		condition.setApp(app);
		condition.setCategory(category);
		List<AppExtension> extensions = appExtensionService.findAppExtensionByCondition(condition);
		return extensions == null ? Collections.<AppExtension>emptyList() : extensions;
	}

	@Override
	public AppExtension createAppExtension(AppExtension extension) {
		requireEnabledApp(extension == null ? null : extension.getApp());
		return appExtensionService.createAppExtension(extension);
	}

	@Override
	public AppExtension updateAppExtension(AppExtension extension) {
		AppExtension stored = extension == null || extension.getId() == null
				? null : appExtensionService.findAppExtensionById(extension.getId());
		requireEnabledApp(extension != null && extension.getApp() != null
				? extension.getApp() : stored == null ? null : stored.getApp());
		return appExtensionService.updateAppExtensionById(extension);
	}

	@Override
	public AppExtension deleteAppExtension(Integer id) {
		AppExtension stored = appExtensionService.findAppExtensionById(id);
		requireEnabledApp(stored == null ? null : stored.getApp());
		return appExtensionService.deleteAppExtensionById(id);
	}

	@Override
	public Page createPage(Page page) {
		requireEnabledApp(page == null ? null : page.getApp());
		if (Boolean.FALSE.equals(page.getIsPage())) {
			return pageService.createFolder(page);
		}
		return pageService.createPage(page);
	}

	@Override
	public Page updatePage(Integer id, Page page) {
		Page stored = pageService.queryPageById(id);
		requireEnabledApp(page != null && page.getApp() != null
				? page.getApp() : stored == null ? null : stored.getApp());
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
		appService.requireEnabledApp(appId);
		return pageService.queryAllPage(appId);
	}

	@Override
	public Page pageDetail(Integer id) {
		Page page = pageService.queryPageById(id);
		requireEnabledApp(page == null ? null : page.getApp());
		return page;
	}

	@Override
	public Page deletePage(Integer id) {
		Page page = pageService.queryPageById(id);
		requireEnabledApp(page == null ? null : page.getApp());
		return pageService.delPage(id);
	}

	@Override
	public Page copyPage(Page page) {
		Page source = page == null || page.getId() == null ? null : pageService.queryPageById(page.getId());
		requireEnabledApp(source == null ? null : source.getApp());
		return pageService.copyPage(page);
	}

	@Override
	public List<PageHistory> pageHistories(Integer pageId) {
		Page page = pageService.queryPageById(pageId);
		requireEnabledApp(page == null ? null : page.getApp());
		return pageHistoryService.findPageHistories(pageId);
	}

	@Override
	public PageHistory pageHistoryDetail(Integer historyId) {
		PageHistory history = pageHistoryService.findPageHistoryById(historyId);
		requireEnabledApp(history == null ? null : history.getApp());
		return history;
	}

	@Override
	public Page restorePageHistory(Map<String, Object> request) {
		Object value = request == null ? null : request.get("id");
		if (value == null && request != null) {
			value = request.get("historyId");
		}
		if (value == null) {
			throw new IllegalArgumentException("TinyEngine page history id is required");
		}
		Integer historyId = value instanceof Number
				? Integer.valueOf(((Number) value).intValue()) : Integer.valueOf(String.valueOf(value));
		PageHistory history = pageHistoryService.findPageHistoryById(historyId);
		requireEnabledApp(history == null ? null : history.getApp());
		return pageService.restorePageHistory(historyId);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PreviewDto previewMetadata(PreviewParam previewParam) {
		PreviewDto preview = new PreviewDto();
		Integer appId = previewParam.getApp() == null ? previewParam.getId() : previewParam.getApp();

		if ("block".equals(previewParam.getType())) {
			BlockDto block = blockService.queryBlockById(previewParam.getId());
			requireEnabledApp(block == null ? null : block.getAppId());
			preview.setUtils(toSchemaUtils(appExtensions(block == null ? null : block.getAppId(), "utils")));
			Map<String, Object> content = block == null ? null : block.getContent();
			Object blockDataSource = content == null ? null : content.get("dataSource");
			preview.setDataSource(blockDataSource instanceof Map
					? new LinkedHashMap<String, Object>((Map<String, Object>) blockDataSource)
					: new LinkedHashMap<String, Object>());
			I18nEntryListResult blockI18n = i18nEntryService.findI18nEntryByApp(previewParam.getId(), "block");
			preview.setI18n(normalizeI18n(blockI18n));
			return preview;
		}

		App app = appService.requireEnabledApp(appId);
		preview.setUtils(toSchemaUtils(appExtensions(appId, "utils")));
		Map<String, Object> dataSource = new LinkedHashMap<String, Object>();
		Datasource condition = new Datasource();
		condition.setApp(appId);
		List<Datasource> sources = datasourceService.queryDatasourceByCondition(condition);
		dataSource.put("list", sources == null ? Collections.<Datasource>emptyList() : sources);
		if (app != null && app.getDataSourceGlobal() != null) {
			dataSource.putAll(app.getDataSourceGlobal());
		}
		preview.setDataSource(dataSource);
		preview.setGlobalState(app == null ? null : app.getGlobalState());
		I18nEntryListResult appI18n = i18nEntryService.findI18nEntryByApp(appId, "app");
		preview.setI18n(normalizeI18n(appI18n));
		return preview;
	}

	@Override
	public List<Block> blocks(String appId, String groupId) {
		requireEnabledApp(integerValue(appId));
		return blockService.listNew(appId, groupId);
	}

	@Override
	public List<Block> blockList(BlockParamDto blockParamDto) {
		requireEnabledApp(blockParamDto == null ? null : integerValue(blockParamDto.getAppId()));
		IPage<Block> page = blockService.findBlocksByPagetionList(blockParamDto);
		return page == null ? Collections.<Block>emptyList() : page.getRecords();
	}

	@Override
	public List<BlockHistory> blockHistories(Integer blockId) {
		requireBlockApplication(blockId);
		return blockService.blockHistories(blockId);
	}

	@Override
	public List<Tenant> blockTenants() {
		return blockService.blockTenants();
	}

	@Override
	public List<User> blockUsers() {
		return blockService.blockUsers();
	}

	@Override
	public BlockDto blockDetail(Integer id) {
		BlockDto block = blockService.queryBlockById(id);
		requireEnabledApp(block == null ? null : block.getAppId());
		return block;
	}

	@Override
	public BlockDto createBlock(BlockParam blockParam) {
		requireEnabledApp(blockParam == null ? null : blockParam.getAppId());
		return blockService.createBlock(blockParam);
	}

	@Override
	public BlockDto updateBlock(Integer id, BlockParam blockParam) {
		Integer appId = blockParam == null ? null : blockParam.getAppId();
		if (appId == null) {
			BlockDto stored = blockService.queryBlockById(id);
			appId = stored == null ? null : stored.getAppId();
		}
		requireEnabledApp(appId);
		blockParam.setId(id);
		return blockService.updateBlockById(blockParam);
	}

	@Override
	public BlockDto deleteBlock(Integer id) {
		BlockDto block = blockService.queryBlockById(id);
		requireEnabledApp(block == null ? null : block.getAppId());
		blockService.deleteBlockById(id);
		return block;
	}

	@Override
	public BlockDto deployBlock(BlockBuildDto blockBuildDto) {
		BlockDto block = blockBuildDto == null ? null : blockBuildDto.getBlock();
		if (block != null && block.getAppId() == null && block.getId() != null) {
			block = blockService.queryBlockById(block.getId());
		}
		requireEnabledApp(block == null ? null : block.getAppId());
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
		requireEnabledApp(appId);
		return blockGroupService.getBlockGroupByIdsOrAppId(ids, appId, from);
	}

	@Override
	public BlockGroup createBlockGroup(BlockGroup blockGroup) {
		requireEnabledApp(blockGroup == null ? null : blockGroup.getAppId());
		return blockGroupService.createBlockGroup(blockGroup);
	}

	@Override
	public List<BlockGroup> updateBlockGroup(Integer id, BlockGroup blockGroup) {
		BlockGroup stored = blockGroupService.findBlockGroupById(id);
		requireEnabledApp(blockGroup != null && blockGroup.getAppId() != null
				? blockGroup.getAppId() : stored == null ? null : stored.getAppId());
		blockGroup.setId(id);
		blockGroupService.updateBlockGroupById(blockGroup);
		return singletonIfPresent(blockGroupService.findBlockGroupById(id));
	}

	@Override
	public List<BlockGroup> deleteBlockGroup(Integer id) {
		BlockGroup blockGroup = blockGroupService.findBlockGroupById(id);
		requireEnabledApp(blockGroup == null ? null : blockGroup.getAppId());
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
		Model model = modelService.queryModelDetailById(id);
		requireEnabledApp(model == null ? null : model.getAppId());
		return model;
	}

	@Override
	public Model createModel(Model model) {
		requireEnabledApp(model == null ? null : model.getAppId());
		modelService.save(model);
		return model;
	}

	@Override
	public Model updateModel(Integer id, Model model) {
		Model stored = modelService.queryModelById(id);
		requireEnabledApp(model != null && model.getAppId() != null
				? model.getAppId() : stored == null ? null : stored.getAppId());
		model.setId(id);
		modelService.updateById(model);
		Model updated = modelService.queryModelById(id);
		return updated == null ? model : updated;
	}

	@Override
	public Model deleteModel(Integer id) {
		Model model = modelService.queryModelById(id);
		requireEnabledApp(model == null ? null : model.getAppId());
		modelService.removeById(id);
		return model;
	}

	@Override
	public List<Datasource> datasources(Integer appId) {
		requireEnabledApp(appId);
		Datasource datasource = new Datasource();
		datasource.setApp(appId);
		return datasourceService.queryDatasourceByCondition(datasource);
	}

	@Override
	public Datasource datasourceDetail(Integer id) {
		Datasource datasource = datasourceService.queryDatasourceById(id);
		requireEnabledApp(datasource == null ? null : datasource.getApp());
		return datasource;
	}

	@Override
	public Datasource createDatasource(Datasource datasource) {
		requireEnabledApp(datasource == null ? null : datasource.getApp());
		return datasourceService.createDatasource(datasource);
	}

	@Override
	public Datasource updateDatasource(Integer id, Datasource datasource) {
		Datasource stored = datasourceService.queryDatasourceById(id);
		requireEnabledApp(datasource != null && datasource.getApp() != null
				? datasource.getApp() : stored == null ? null : stored.getApp());
		datasource.setId(id);
		return datasourceService.updateDatasourceById(datasource);
	}

	@Override
	public Datasource deleteDatasource(Integer id) {
		Datasource stored = datasourceService.queryDatasourceById(id);
		requireEnabledApp(stored == null ? null : stored.getApp());
		return datasourceService.deleteDatasourceById(id);
	}

	@Override
	public AppDto appTemplates(Integer currentPage, Integer pageSize, String orderBy, App app) {
		return appTemplateService.queryAllAppTemplate(currentPage, pageSize, orderBy, app);
	}

	@Override
	public App setAppTemplate(Integer id, Boolean isTemplate, String templateType) {
		return appService.setAppTemplate(id, isTemplate, templateType);
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
		requireEnabledApp(appId);
		return resourceGroupService.queryResourceGroupByAppId(appId);
	}

	@Override
	public ResourceGroup resourceGroupDetail(Integer id) {
		ResourceGroup group = resourceGroupService.queryResourceGroupById(id);
		requireEnabledApp(group == null ? null : group.getAppId());
		return group;
	}

	@Override
	public ResourceGroup createResourceGroup(ResourceGroup resourceGroup) {
		requireEnabledApp(resourceGroup == null ? null : resourceGroup.getAppId());
		return resourceGroupService.createResourceGroup(resourceGroup);
	}

	@Override
	public ResourceGroup updateResourceGroup(Integer id, ResourceGroup resourceGroup) {
		ResourceGroup stored = resourceGroupService.queryResourceGroupById(id);
		requireEnabledApp(resourceGroup != null && resourceGroup.getAppId() != null
				? resourceGroup.getAppId() : stored == null ? null : stored.getAppId());
		resourceGroup.setId(id);
		return resourceGroupService.updateResourceGroupById(resourceGroup);
	}

	@Override
	public ResourceGroup deleteResourceGroup(Integer id) {
		ResourceGroup stored = resourceGroupService.queryResourceGroupById(id);
		requireEnabledApp(stored == null ? null : stored.getAppId());
		return resourceGroupService.deleteResourceGroupById(id);
	}

	@Override
	public List<Resource> resources() {
		return resourceService.queryAllResource();
	}

	@Override
	public List<Resource> resourcesByGroup(Integer resourceGroupId) {
		ResourceGroup group = resourceGroupService.queryResourceGroupById(resourceGroupId);
		requireEnabledApp(group == null ? null : group.getAppId());
		return resourceService.queryResourceByResourceGroupId(resourceGroupId);
	}

	@Override
	public Resource resourceDetail(Integer id) {
		Resource resource = resourceService.queryResourceById(id);
		requireResourceApplication(resource);
		return resource;
	}

	@Override
	public Resource createResource(Resource resource) {
		requireResourceApplication(resource);
		resourceService.save(resource);
		return resource;
	}

	@Override
	public List<Resource> createResources(List<Resource> resources) {
		if (resources != null) {
			for (Resource resource : resources) {
				requireResourceApplication(resource);
			}
		}
		return resourceService.createResources(resources);
	}

	@Override
	public Object uploadResource(ResourceRequestDto request) {
		requireEnabledApp(request == null ? null : request.getAppId());
		return resourceService.uploadResource(request);
	}

	@Override
	public Resource updateResource(Integer id, Resource resource) {
		Resource stored = resourceService.queryResourceById(id);
		requireResourceApplication(resource != null && resource.getAppId() != null ? resource : stored);
		resource.setId(id);
		return resourceService.updateResourceById(resource);
	}

	@Override
	public Resource deleteResource(Integer id) {
		Resource stored = resourceService.queryResourceById(id);
		requireResourceApplication(stored);
		return resourceService.deleteResourceById(id);
	}

	@Override
	public I18nEntryListResult i18nEntries(Integer host, String hostType) {
		if (host == null) {
			return i18nEntryService.findAllI18nEntry();
		}
		requireI18nHost(host, hostType);
		return i18nEntryService.findI18nEntryByApp(host, hostType);
	}

	@Override
	public List<I18nEntry> createI18nEntries(OperateI18nEntries entries) {
		requireI18nHost(entries == null ? null : integerValue(entries.getHost()),
				entries == null ? null : entries.getHostType());
		return i18nEntryService.create(entries);
	}

	@Override
	public List<I18nEntryDto> deleteI18nEntries(DeleteI18nEntry entry) {
		requireI18nHost(entry == null ? null : integerValue(entry.getHost()),
				entry == null ? null : entry.getHostType());
		return i18nEntryService.deleteI18nEntriesByHostAndHostTypeAndKey(entry);
	}

	@Override
	public List<I18nEntryDto> bulkDeleteI18nEntries(DeleteI18nEntry entry) {
		requireI18nHost(entry == null ? null : integerValue(entry.getHost()),
				entry == null ? null : entry.getHostType());
		return i18nEntryService.deleteI18nEntriesByHostAndHostTypeAndKey(entry);
	}

	@Override
	public TaskRecord taskStatus(Integer id) {
		return taskRecordService.taskStatus(id);
	}

	@Override
	public Object updateI18nEntriesFile(Integer appId) {
		requireEnabledApp(appId);
		return externalCapability.unsupported("i18n-entry-file-update");
	}

	private void requireEnabledApp(Integer appId) {
		if (appId != null) {
			appService.requireEnabledApp(appId);
		}
	}

	private void requireBlockApplication(Integer blockId) {
		BlockDto block = blockId == null ? null : blockService.queryBlockById(blockId);
		requireEnabledApp(block == null ? null : block.getAppId());
	}

	private void requireResourceApplication(Resource resource) {
		if (resource == null) {
			return;
		}
		Integer appId = resource.getAppId();
		if (appId == null && resource.getResourceGroupId() != null) {
			ResourceGroup group = resourceGroupService.queryResourceGroupById(resource.getResourceGroupId());
			appId = group == null ? null : group.getAppId();
		}
		requireEnabledApp(appId);
	}

	private void requireI18nHost(Integer host, String hostType) {
		if ("app".equals(hostType)) {
			requireEnabledApp(host);
		} else if ("block".equals(hostType)) {
			requireBlockApplication(host);
		}
	}

	private static Integer integerValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return Integer.valueOf(value);
	}

	private static int defaultNumber(Integer value, int defaultValue) {
		return value == null ? defaultValue : value.intValue();
	}

	private Map<String, Object> appDataSource(Integer appId, App app) {
		Map<String, Object> dataSource = new LinkedHashMap<String, Object>();
		List<Datasource> sources = Collections.emptyList();
		if (datasourceService != null) {
			Datasource condition = new Datasource();
			condition.setApp(appId);
			List<Datasource> queried = datasourceService.queryDatasourceByCondition(condition);
			if (queried != null) {
				sources = queried;
			}
		}
		dataSource.put("list", sources);
		if (app != null && app.getDataSourceGlobal() != null) {
			dataSource.putAll(app.getDataSourceGlobal());
		}
		return dataSource;
	}

	private static Map<String, Object> appMeta(App app) {
		Map<String, Object> meta = new LinkedHashMap<String, Object>();
		meta.put("appId", String.valueOf(app.getId()));
		meta.put("name", app.getName());
		meta.put("description", app.getDescription());
		meta.put("creator", app.getCreatedBy());
		meta.put("branch", app.getBranch());
		meta.put("gitGroup", app.getGitGroup());
		meta.put("projectName", app.getProjectName());
		meta.put("isDemo", app.getIsDemo());
		meta.put("globalState", app.getGlobalState() == null
				? Collections.emptyList() : app.getGlobalState());
		meta.put("gmtCreate", app.getCreatedTime());
		meta.put("gmtModified", app.getLastUpdatedTime());
		return meta;
	}

	private static List<Map<String, Object>> toComponentTrees(List<Page> pages, Integer homePage) {
		List<Map<String, Object>> trees = new ArrayList<Map<String, Object>>();
		if (pages == null) {
			return trees;
		}
		for (Page page : pages) {
			Map<String, Object> tree = new LinkedHashMap<String, Object>();
			if (Boolean.FALSE.equals(page.getIsPage())) {
				tree.put("componentName", "Folder");
				tree.put("folderName", page.getName());
				tree.put("id", page.getId());
				tree.put("parentId", page.getParentId());
				tree.put("depth", page.getDepth());
				tree.put("router", page.getRoute());
			} else {
				if (page.getPageContent() != null) {
					tree.putAll(page.getPageContent());
				}
				tree.put("componentName", valueOrDefault(tree.get("componentName"), "Page"));
				tree.put("fileName", page.getName());
				Map<String, Object> meta = new LinkedHashMap<String, Object>();
				meta.put("id", page.getId());
				meta.put("name", page.getName());
				meta.put("title", page.getName());
				meta.put("parentId", page.getParentId());
				meta.put("router", page.getRoute());
				meta.put("group", normalizePageGroup(page.getGroup()));
				meta.put("rootElement", Boolean.TRUE.equals(page.getIsBody()) ? "body" : "div");
				meta.put("isHome", homePage != null && homePage.equals(page.getId()));
				meta.put("occupier", page.getOccupier());
				tree.put("meta", meta);
			}
			trees.add(tree);
		}
		return trees;
	}

	private static Object valueOrDefault(Object value, Object defaultValue) {
		return value == null ? defaultValue : value;
	}

	private static String normalizePageGroup(String group) {
		if ("static".equals(group) || "public".equals(group)) {
			return group + "Pages";
		}
		return group == null ? "" : group;
	}

	private static SchemaI18n normalizeI18n(I18nEntryListResult result) {
		SchemaI18n i18n = result == null ? null : result.getMessages();
		if (i18n == null) {
			i18n = new SchemaI18n();
		}
		if (i18n.getZhCn() == null) {
			i18n.setZhCn(new LinkedHashMap<String, String>());
		}
		if (i18n.getEnUs() == null) {
			i18n.setEnUs(new LinkedHashMap<String, String>());
		}
		return i18n;
	}

	private static List<SchemaUtils> toSchemaUtils(List<AppExtension> extensions) {
		return toSchemaUtils(extensions, null);
	}

	private static List<SchemaUtils> toSchemaUtils(List<AppExtension> extensions, String category) {
		List<SchemaUtils> result = new ArrayList<SchemaUtils>();
		if (extensions == null) {
			return result;
		}
		for (AppExtension extension : extensions) {
			if (category != null && !category.equals(extension.getCategory())) {
				continue;
			}
			SchemaUtils schema = new SchemaUtils();
			schema.setName(extension.getName());
			schema.setType(extension.getType());
			schema.setContent(extension.getContent());
			result.add(schema);
		}
		return result;
	}

	private static <T> List<T> singletonIfPresent(T value) {
		if (value == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(value);
	}
}

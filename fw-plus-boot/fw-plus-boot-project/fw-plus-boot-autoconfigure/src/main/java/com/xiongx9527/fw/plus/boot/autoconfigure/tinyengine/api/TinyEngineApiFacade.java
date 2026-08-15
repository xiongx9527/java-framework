package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberRoleRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PreviewDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PreviewParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.ResourceRequestDto;
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

import java.util.List;
import java.util.Map;

public interface TinyEngineApiFacade {

	AppDto appsPage(Integer currentPage, Integer pageSize, String orderBy, App app);

	App createApp(App app);

	App updateApp(Integer id, App app);

	App deleteApp(Integer id);

	List<AppMemberDto> appMembers(Integer appId);
	AppMemberDto addAppMember(Integer appId, AppMemberRoleRequest request);
	AppMemberDto changeAppMember(Integer appId, String userId, AppMemberRoleRequest request);
	void removeAppMember(Integer appId, String userId);

	List<AiModelServiceDto> usableAiModelServices(Integer appId);
	List<AiModelServiceDto> manageableAiModelServices(Integer appId);
	AppAiModelSettings aiModelSettings(Integer appId);
	AppAiModelSettings updateAiModelSettings(Integer appId, AppAiModelSettings settings);
	AiModelServiceDto createAiModelService(Integer appId, AiModelServiceRequest request);
	AiModelServiceDto updateAiModelService(Integer appId, Long serviceId, AiModelServiceRequest request);
	void deleteAiModelService(Integer appId, Long serviceId);
	AiModelServiceDto addAiModel(Integer appId, Long serviceId, AiModelRequest request);
	AiModelServiceDto updateAiModel(Integer appId, Long serviceId, Long modelId, AiModelRequest request);
	void deleteAiModel(Integer appId, Long serviceId, Long modelId);
	List<AiModelServiceDto> importAiModelServices(Integer appId, AiModelImportRequest request);

	Object saveApp(Integer id, Map<String, Object> request);

	Object publishApp(Integer id, Map<String, Object> request);

	Object previewApp(Integer id, Map<String, Object> request);

	App appDetail(Integer id);

	Map<String, Object> appSchema(Integer id);

	Object appSchemaComponents(Integer id);

	Object lockCanvas(Integer appId, Map<String, Object> request);

	List<AppExtension> appExtensions(Integer app, String category);

	AppExtension createAppExtension(AppExtension extension);

	AppExtension updateAppExtension(AppExtension extension);

	AppExtension deleteAppExtension(Integer id);

	Page createPage(Page page);

	Page updatePage(Integer id, Page page);

	List<Page> pages(Integer appId);

	Page pageDetail(Integer id);

	Page deletePage(Integer id);

	Page copyPage(Page page);

	List<PageHistory> pageHistories(Integer pageId);

	PageHistory pageHistoryDetail(Integer historyId);

	Page restorePageHistory(Map<String, Object> request);

	PreviewDto previewMetadata(PreviewParam previewParam);

	List<Block> blocks(String appId, String groupId);

	List<Block> blockList(BlockParamDto blockParamDto);

	List<BlockHistory> blockHistories(Integer blockId);

	List<Tenant> blockTenants();

	List<User> blockUsers();

	BlockDto blockDetail(Integer id);

	BlockDto createBlock(BlockParam blockParam);

	BlockDto updateBlock(Integer id, BlockParam blockParam);

	BlockDto deleteBlock(Integer id);

	BlockDto deployBlock(BlockBuildDto blockBuildDto);

	List<String> blockTags();

	List<BlockDto> blocksNotInGroup(Integer groupId, String label, String name, String[] tags, String createdBy);

	List<BlockGroup> blockGroups(List<Integer> ids, Integer appId, String from);

	BlockGroup createBlockGroup(BlockGroup blockGroup);

	List<BlockGroup> updateBlockGroup(Integer id, BlockGroup blockGroup);

	List<BlockGroup> deleteBlockGroup(Integer id);

	List<BusinessCategory> businessCategories();

	List<BusinessCategory> businessCategoriesByGroup(String group);

	com.baomidou.mybatisplus.extension.plugins.pagination.Page<Model> models(Integer currentPage, Integer pageSize,
			String nameCn, String nameEn);

	Model modelDetail(Integer id);

	Model createModel(Model model);

	Model updateModel(Integer id, Model model);

	Model deleteModel(Integer id);

	List<Datasource> datasources(Integer appId);

	Datasource datasourceDetail(Integer id);

	Datasource createDatasource(Datasource datasource);

	Datasource updateDatasource(Integer id, Datasource datasource);

	Datasource deleteDatasource(Integer id);

	AppDto appTemplates(Integer currentPage, Integer pageSize, String orderBy, App app);

	App createAppTemplate(App app);
	App setAppTemplate(Integer id, Boolean isTemplate, String templateType, Integer industryId, Integer sceneId, String framework);

	List<ResourceGroup> resourceGroups();

	List<ResourceGroup> resourceGroupsByApp(Integer appId);

	ResourceGroup resourceGroupDetail(Integer id);

	ResourceGroup createResourceGroup(ResourceGroup resourceGroup);

	ResourceGroup updateResourceGroup(Integer id, ResourceGroup resourceGroup);

	ResourceGroup deleteResourceGroup(Integer id);

	List<Resource> resources();

	List<Resource> resourcesByGroup(Integer resourceGroupId);

	Resource resourceDetail(Integer id);

	Resource createResource(Resource resource);

	List<Resource> createResources(List<Resource> resources);

	Object uploadResource(ResourceRequestDto request);

	Resource updateResource(Integer id, Resource resource);

	Resource deleteResource(Integer id);

	I18nEntryListResult i18nEntries(Integer host, String hostType);

	List<I18nEntry> createI18nEntries(OperateI18nEntries entries);

	List<I18nEntryDto> deleteI18nEntries(DeleteI18nEntry entry);

	List<I18nEntryDto> bulkDeleteI18nEntries(DeleteI18nEntry entry);

	TaskRecord taskStatus(Integer id);

	Object updateI18nEntriesFile(Integer appId);
}

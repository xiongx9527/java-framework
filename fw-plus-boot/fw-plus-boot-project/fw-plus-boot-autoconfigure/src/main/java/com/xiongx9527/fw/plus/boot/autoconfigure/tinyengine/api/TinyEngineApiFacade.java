package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
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

import java.util.List;
import java.util.Map;

public interface TinyEngineApiFacade {

	AppDto appsPage(Integer currentPage, Integer pageSize, String orderBy, App app);

	App createApp(App app);

	App appDetail(Integer id);

	Map<String, Object> appSchema(Integer id);

	Page createPage(Page page);

	Page updatePage(Integer id, Page page);

	List<Page> pages(Integer appId);

	Page pageDetail(Integer id);

	List<Block> blocks(String appId, String groupId);

	List<Block> blockList(BlockParamDto blockParamDto);

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

	Resource updateResource(Integer id, Resource resource);

	Resource deleteResource(Integer id);

	I18nEntryListResult i18nEntries(Integer host, String hostType);

	List<I18nEntry> createI18nEntries(OperateI18nEntries entries);

	Object updateI18nEntriesFile(Integer appId);
}

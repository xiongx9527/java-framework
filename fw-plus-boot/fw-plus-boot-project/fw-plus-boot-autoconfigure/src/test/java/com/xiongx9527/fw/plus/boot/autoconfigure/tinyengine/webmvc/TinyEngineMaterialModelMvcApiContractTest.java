package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BusinessCategory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineMaterialModelMvcApiContractTest {

	private AppService appService;
	private PageService pageService;
	private BlockService blockService;
	private BlockGroupService blockGroupService;
	private BusinessCategoryService businessCategoryService;
	private ModelService modelService;
	private DatasourceService datasourceService;
	private AppTemplateService appTemplateService;
	private ResourceGroupService resourceGroupService;
	private ResourceService resourceService;
	private I18nEntryService i18nEntryService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		appService = mock(AppService.class);
		pageService = mock(PageService.class);
		blockService = mock(BlockService.class);
		blockGroupService = mock(BlockGroupService.class);
		businessCategoryService = mock(BusinessCategoryService.class);
		modelService = mock(ModelService.class);
		datasourceService = mock(DatasourceService.class);
		appTemplateService = mock(AppTemplateService.class);
		resourceGroupService = mock(ResourceGroupService.class);
		resourceService = mock(ResourceService.class);
		i18nEntryService = mock(I18nEntryService.class);
		TinyEngineApiFacade facade = new DefaultTinyEngineApiFacade(appService, pageService, blockService,
				blockGroupService, businessCategoryService, modelService, datasourceService, appTemplateService,
				resourceGroupService, resourceService, i18nEntryService, new DefaultTinyEngineExternalCapability());
		mvc = MockMvcBuilders.standaloneSetup(new TinyEngineMaterialMvcController(facade),
				new TinyEngineModelMvcController(facade)).build();
	}

	@Test
	void blockRoutesDelegateToFacadeBackedCoreServices() throws Exception {
		Block block = block(7, "hero");
		BlockDto blockDto = new BlockDto();
		blockDto.setId(Integer.valueOf(7));
		blockDto.setLabel("hero");
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Block> blockPage =
				new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Block>(1, 10);
		blockPage.setRecords(Collections.singletonList(block));
		when(blockService.listNew("9", "2")).thenReturn(Collections.singletonList(block));
		when(blockService.findBlocksByPagetionList(any())).thenReturn(blockPage);
		when(blockService.queryBlockById(Integer.valueOf(7))).thenReturn(blockDto);
		when(blockService.createBlock(any(BlockParam.class))).thenReturn(blockDto);
		when(blockService.updateBlockById(any(BlockParam.class))).thenReturn(blockDto);
		when(blockService.deleteBlockById(Integer.valueOf(7))).thenReturn(Integer.valueOf(1));
		when(blockService.deploy(any())).thenReturn(blockDto);
		when(blockService.allTags()).thenReturn(Arrays.asList("chart", "form"));
		when(blockService.getNotInGroupBlocks(any())).thenReturn(Collections.singletonList(blockDto));

		mvc.perform(get("/material-center/api/blocks").param("appId", "9").param("groupId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].label", is("hero")));
		verify(blockService).listNew("9", "2");

		mvc.perform(get("/material-center/api/block").param("name_cn", "hero").param("limit", "10"))
				.andExpect(status().isOk());
		verify(blockService).findBlocksByPagetionList(any());

		mvc.perform(get("/material-center/api/block/detail/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.label", is("hero")));
		verify(blockService).queryBlockById(Integer.valueOf(7));

		mvc.perform(post("/material-center/api/block/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"label\":\"hero\"}"))
				.andExpect(status().isOk());
		verify(blockService).createBlock(any(BlockParam.class));

		mvc.perform(post("/material-center/api/block/update/7")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"label\":\"hero\"}"))
				.andExpect(status().isOk());
		ArgumentCaptor<BlockParam> blockParamCaptor = ArgumentCaptor.forClass(BlockParam.class);
		verify(blockService).updateBlockById(blockParamCaptor.capture());
		assertThat(blockParamCaptor.getValue().getId()).isEqualTo(Integer.valueOf(7));

		mvc.perform(get("/material-center/api/block/delete/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.label", is("hero")));
		verify(blockService).deleteBlockById(Integer.valueOf(7));

		mvc.perform(post("/material-center/api/block/deploy")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"version\":\"1.0.0\"}"))
				.andExpect(status().isOk());
		verify(blockService).deploy(any());

		mvc.perform(get("/material-center/api/block/tags"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[1]", is("form")));
		verify(blockService).allTags();

		mvc.perform(get("/material-center/api/block/notgroup/2")
						.param("label_contains", "hero")
						.param("name_cn_contains", "Hero")
						.param("tags_contains", "chart")
						.param("createdBy", "alice"))
				.andExpect(status().isOk());
		verify(blockService).getNotInGroupBlocks(any());
	}

	@Test
	void groupAndCategoryRoutesDelegateToDedicatedServices() throws Exception {
		BlockGroup group = blockGroup(3, "basic");
		BusinessCategory category = businessCategory(5, "industry");
		when(blockGroupService.getBlockGroupByIdsOrAppId(any(), eq(Integer.valueOf(9)), eq("block")))
				.thenReturn(Collections.singletonList(group));
		when(blockGroupService.createBlockGroup(any(BlockGroup.class))).thenReturn(group);
		when(blockGroupService.updateBlockGroupById(any(BlockGroup.class))).thenReturn(Integer.valueOf(1));
		when(blockGroupService.findBlockGroupById(Integer.valueOf(3))).thenReturn(group);
		when(blockGroupService.deleteBlockGroupById(Integer.valueOf(3))).thenReturn(Integer.valueOf(1));
		when(businessCategoryService.queryAllBusinessCategory()).thenReturn(Collections.singletonList(category));
		when(businessCategoryService.queryBusinessCategoryByCondition(any(BusinessCategory.class)))
				.thenReturn(Collections.singletonList(category));

		mvc.perform(get("/material-center/api/block-groups").param("app", "9").param("from", "block"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name", is("basic")));
		verify(blockGroupService).getBlockGroupByIdsOrAppId(any(), eq(Integer.valueOf(9)), eq("block"));

		mvc.perform(post("/material-center/api/block-groups/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"basic\"}"))
				.andExpect(status().isOk());
		verify(blockGroupService).createBlockGroup(any(BlockGroup.class));

		mvc.perform(post("/material-center/api/block-groups/update/3")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"basic\"}"))
				.andExpect(status().isOk());
		verify(blockGroupService).updateBlockGroupById(any(BlockGroup.class));

		mvc.perform(get("/material-center/api/block-groups/delete/3"))
				.andExpect(status().isOk());
		verify(blockGroupService).deleteBlockGroupById(Integer.valueOf(3));

		mvc.perform(get("/material-center/api/block-categories").param("app", "9"))
				.andExpect(status().isOk());
		mvc.perform(post("/material-center/api/block-categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"basic\"}"))
				.andExpect(status().isOk());
		mvc.perform(put("/material-center/api/block-categories/3")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"basic\"}"))
				.andExpect(status().isOk());
		mvc.perform(delete("/material-center/api/block-categories/3"))
				.andExpect(status().isOk());

		mvc.perform(get("/material-center/api/business-category/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name", is("industry")));
		verify(businessCategoryService).queryAllBusinessCategory();

		mvc.perform(get("/material-center/api/business-category/find").param("group", "industry"))
				.andExpect(status().isOk());
		verify(businessCategoryService).queryBusinessCategoryByCondition(any(BusinessCategory.class));
	}

	@Test
	void modelRoutesDelegateToModelService() throws Exception {
		Model model = model(6, "用户模型", "UserModel");
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Model> page =
				new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Model>(1, 10);
		page.setRecords(Collections.singletonList(model));
		when(modelService.pageQuery(1, 10, "用户", "User")).thenReturn(page);
		when(modelService.queryModelById(Integer.valueOf(6))).thenReturn(model);
		when(modelService.save(any(Model.class))).thenReturn(true);
		when(modelService.updateById(any(Model.class))).thenReturn(true);
		when(modelService.removeById(Integer.valueOf(6))).thenReturn(true);

		mvc.perform(get("/material-center/api/model/list")
						.param("currentPage", "1")
						.param("pageSize", "10")
						.param("nameCn", "用户")
						.param("nameEn", "User"))
				.andExpect(status().isOk());
		verify(modelService).pageQuery(1, 10, "用户", "User");

		mvc.perform(get("/material-center/api/model/detail/6"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nameCn", is("用户模型")));
		verify(modelService).queryModelById(Integer.valueOf(6));

		mvc.perform(post("/material-center/api/model/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nameCn\":\"用户模型\",\"nameEn\":\"UserModel\"}"))
				.andExpect(status().isOk());
		verify(modelService).save(any(Model.class));

		mvc.perform(put("/material-center/api/model/update/6")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nameCn\":\"用户模型\"}"))
				.andExpect(status().isOk());
		verify(modelService).updateById(any(Model.class));

		mvc.perform(delete("/material-center/api/model/delete/6"))
				.andExpect(status().isOk());
		verify(modelService).removeById(Integer.valueOf(6));
	}

	@Test
	void datasourceRoutesDelegateToDatasourceService() throws Exception {
		Datasource datasource = datasource(8, "orders");
		when(datasourceService.queryDatasourceByCondition(any(Datasource.class)))
				.thenReturn(Collections.singletonList(datasource));
		when(datasourceService.queryDatasourceById(Integer.valueOf(8))).thenReturn(datasource);
		when(datasourceService.createDatasource(any(Datasource.class))).thenReturn(datasource);
		when(datasourceService.updateDatasourceById(any(Datasource.class))).thenReturn(datasource);
		when(datasourceService.deleteDatasourceById(Integer.valueOf(8))).thenReturn(datasource);

		mvc.perform(get("/app-center/api/sources/list/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name", is("orders")));
		verify(datasourceService).queryDatasourceByCondition(any(Datasource.class));

		mvc.perform(get("/app-center/api/sources/detail/8"))
				.andExpect(status().isOk());
		verify(datasourceService).queryDatasourceById(Integer.valueOf(8));

		mvc.perform(post("/app-center/api/sources/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"orders\"}"))
				.andExpect(status().isOk());
		verify(datasourceService).createDatasource(any(Datasource.class));

		mvc.perform(post("/app-center/api/sources/update/8")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"orders\"}"))
				.andExpect(status().isOk());
		verify(datasourceService).updateDatasourceById(any(Datasource.class));

		mvc.perform(get("/app-center/api/sources/delete/8"))
				.andExpect(status().isOk());
		verify(datasourceService).deleteDatasourceById(Integer.valueOf(8));
	}

	@Test
	void templateResourceAndI18nRoutesDelegateOrReportStableUnsupportedCapability() throws Exception {
		AppDto templates = new AppDto();
		App template = new App();
		template.setId(Integer.valueOf(11));
		template.setName("template");
		templates.setApps(Collections.singletonList(template));
		ResourceGroup group = resourceGroup(12, "media");
		Resource resource = resource(13, "logo");
		I18nEntry entry = i18nEntry(14, "hello");
		when(appTemplateService.queryAllAppTemplate(eq(Integer.valueOf(1)), eq(Integer.valueOf(20)), eq("name asc"),
				any(App.class))).thenReturn(templates);
		when(appTemplateService.createAppByTemplate(any(App.class))).thenReturn(template);
		when(resourceGroupService.queryAllResourceGroupAndResource()).thenReturn(Collections.singletonList(group));
		when(resourceGroupService.createResourceGroup(any(ResourceGroup.class))).thenReturn(group);
		when(resourceGroupService.queryResourceGroupById(Integer.valueOf(12))).thenReturn(group);
		when(resourceService.queryAllResource()).thenReturn(Collections.singletonList(resource));
		when(resourceService.save(any(Resource.class))).thenReturn(true);
		when(resourceService.queryResourceById(Integer.valueOf(13))).thenReturn(resource);
		when(i18nEntryService.findI18nEntryByApp(Integer.valueOf(11), "app")).thenReturn(null);
		when(i18nEntryService.create(any())).thenReturn(Collections.singletonList(entry));

		mvc.perform(get("/app-center/api/app-template/list")
						.param("currentPage", "1")
						.param("pageSize", "20")
						.param("orderBy", "name asc"))
				.andExpect(status().isOk());
		verify(appTemplateService).queryAllAppTemplate(eq(Integer.valueOf(1)), eq(Integer.valueOf(20)),
				eq("name asc"), any(App.class));

		mvc.perform(post("/app-center/api/app-template/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"template\"}"))
				.andExpect(status().isOk());
		verify(appTemplateService).createAppByTemplate(any(App.class));

		mvc.perform(get("/material-center/api/resource-group/list"))
				.andExpect(status().isOk());
		verify(resourceGroupService).queryAllResourceGroupAndResource();
		mvc.perform(post("/material-center/api/resource-group/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"media\"}"))
				.andExpect(status().isOk());
		verify(resourceGroupService).createResourceGroup(any(ResourceGroup.class));
		mvc.perform(get("/material-center/api/resource-group/detail/12"))
				.andExpect(status().isOk());
		verify(resourceGroupService).queryResourceGroupById(Integer.valueOf(12));

		mvc.perform(get("/material-center/api/resource/list"))
				.andExpect(status().isOk());
		verify(resourceService).queryAllResource();
		mvc.perform(post("/material-center/api/resource/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"logo\"}"))
				.andExpect(status().isOk());
		verify(resourceService).save(any(Resource.class));
		mvc.perform(get("/material-center/api/resource/detail/13"))
				.andExpect(status().isOk());
		verify(resourceService).queryResourceById(Integer.valueOf(13));

		mvc.perform(get("/app-center/api/i18n/entries").param("host", "11").param("host_type", "app"))
				.andExpect(status().isOk());
		verify(i18nEntryService).findI18nEntryByApp(Integer.valueOf(11), "app");
		mvc.perform(post("/app-center/api/i18n/entries/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"host\":\"11\",\"host_type\":\"app\",\"key\":\"hello\"}"))
				.andExpect(status().isOk());
		verify(i18nEntryService).create(any());

		mvc.perform(post("/app-center/api/apps/11/i18n/entries/update"))
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.success", is(false)))
				.andExpect(jsonPath("$.code", is("501")));
	}

	private static Block block(Integer id, String label) {
		Block block = new Block();
		block.setId(id);
		block.setLabel(label);
		return block;
	}

	private static BlockGroup blockGroup(Integer id, String name) {
		BlockGroup group = new BlockGroup();
		group.setId(id);
		group.setName(name);
		return group;
	}

	private static BusinessCategory businessCategory(Integer id, String name) {
		BusinessCategory category = new BusinessCategory();
		category.setId(id);
		category.setName(name);
		return category;
	}

	private static Model model(Integer id, String nameCn, String nameEn) {
		Model model = new Model();
		model.setId(id);
		model.setNameCn(nameCn);
		model.setNameEn(nameEn);
		return model;
	}

	private static Datasource datasource(Integer id, String name) {
		Datasource datasource = new Datasource();
		datasource.setId(id);
		datasource.setName(name);
		return datasource;
	}

	private static ResourceGroup resourceGroup(Integer id, String name) {
		ResourceGroup group = new ResourceGroup();
		group.setId(id);
		group.setName(name);
		return group;
	}

	private static Resource resource(Integer id, String name) {
		Resource resource = new Resource();
		resource.setId(id);
		resource.setName(name);
		return resource;
	}

	private static I18nEntry i18nEntry(Integer id, String key) {
		I18nEntry entry = new I18nEntry();
		entry.setId(id);
		entry.setKey(key);
		return entry;
	}
}

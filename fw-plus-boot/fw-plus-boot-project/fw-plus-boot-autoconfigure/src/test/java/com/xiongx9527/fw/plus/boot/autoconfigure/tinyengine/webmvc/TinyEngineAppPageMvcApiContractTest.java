package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.CanvasDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.service.AppExtensionService;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.CanvasService;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineAppPageMvcApiContractTest {

	private AppService appService;
	private PageService pageService;
	private AppExtensionService appExtensionService;
	private DatasourceService datasourceService;
	private I18nEntryService i18nEntryService;
	private CanvasService canvasService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		appService = mock(AppService.class);
		when(appService.requireEnabledApp(any(Integer.class))).thenAnswer(invocation ->
				appService.queryAppById(invocation.getArgument(0)));
		pageService = mock(PageService.class);
		appExtensionService = mock(AppExtensionService.class);
		datasourceService = mock(DatasourceService.class);
		i18nEntryService = mock(I18nEntryService.class);
		canvasService = mock(CanvasService.class);
		TinyEngineApiFacade facade = new DefaultTinyEngineApiFacade(appService, pageService, null, null, null,
				null, datasourceService, null, null, null, i18nEntryService, appExtensionService,
				canvasService, new DefaultTinyEngineExternalCapability());
		mvc = MockMvcBuilders.standaloneSetup(new TinyEngineAppMvcController(facade),
				new TinyEnginePageMvcController(facade)).build();
	}

	@Test
	void appLifecycleRoutesDelegateToCoreServices() throws Exception {
		App updated = app(1, "example-updated");
		when(appService.updateAppById(any(App.class))).thenReturn(updated);
		when(appService.deleteAppById(Integer.valueOf(1))).thenReturn(updated);
		when(appService.saveApp(eq(Integer.valueOf(1)), any(Map.class))).thenReturn(updated);
		when(appService.queryAppById(Integer.valueOf(1))).thenReturn(updated);

		mvc.perform(post("/app-center/api/apps/update/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"example-updated\",\"tenantId\":\"attacker-tenant\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(1)));
		ArgumentCaptor<App> updateCaptor = ArgumentCaptor.forClass(App.class);
		verify(appService).updateAppById(updateCaptor.capture());
		assertThat(updateCaptor.getValue().getTenantId()).isNull();
		mvc.perform(get("/app-center/api/apps/delete/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(1)));
		mvc.perform(get("/app-center/api/apps/save/1").param("version", "1.2.3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(1)));
		mvc.perform(post("/app-center/api/apps/publish/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"branch\":\"release/1.2\"}"))
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.success", is(false)))
				.andExpect(jsonPath("$.error.message", is("TinyEngine 应用发布能力未配置")));
		verify(appService, never()).publishApp(eq(Integer.valueOf(1)), any(Map.class));
		mvc.perform(get("/app-center/api/apps/preview/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(1)));
	}

	@Test
	void extensionCrudAndCanvasRoutesUseOfficialResponseShape() throws Exception {
		AppExtension extension = new AppExtension();
		extension.setId(Integer.valueOf(8));
		extension.setApp(Integer.valueOf(1));
		extension.setCategory("utils");
		when(appExtensionService.createAppExtension(any(AppExtension.class))).thenReturn(extension);
		when(appExtensionService.updateAppExtensionById(any(AppExtension.class))).thenReturn(extension);
		when(appExtensionService.deleteAppExtensionById(Integer.valueOf(8))).thenReturn(extension);
		CanvasDto canvas = new CanvasDto();
		canvas.setOperate("success");
		when(canvasService.lockCanvas(Integer.valueOf(7), "occupy", "page")).thenReturn(canvas);

		mvc.perform(post("/app-center/api/apps/extension/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"app\":1,\"category\":\"utils\",\"content\":{}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.app", is(1)));
		mvc.perform(post("/app-center/api/apps/extension/update")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":8,\"app\":1,\"category\":\"utils\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(8)));
		mvc.perform(get("/app-center/api/apps/extension/delete").param("id", "8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(8)));
		mvc.perform(get("/app-center/api/apps/canvas/lock")
						.param("id", "7").param("state", "occupy").param("type", "page"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.operate", is("success")));
	}

	@Test
	void appPageDelegatesToCoreServiceWithFilters() throws Exception {
		AppDto dto = new AppDto();
		App app = app(1, "demo-app");
		dto.setApps(Collections.singletonList(app));
		dto.setTotal(Integer.valueOf(1));
		when(appService.queryAllAppByPage(eq(Integer.valueOf(1)), eq(Integer.valueOf(20)), eq("created_time desc"),
				any(App.class))).thenReturn(dto);

		mvc.perform(get("/app-center/api/apps/page")
						.param("currentPage", "1")
						.param("pageSize", "20")
						.param("name", "demo")
						.param("industryId", "2")
						.param("sceneId", "3")
						.param("framework", "Vue")
						.param("orderBy", "created_time desc")
						.param("createdBy", "alice"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.total", is(1)))
				.andExpect(jsonPath("$.data.apps[0].name", is("demo-app")));

		ArgumentCaptor<App> appCaptor = ArgumentCaptor.forClass(App.class);
		verify(appService).queryAllAppByPage(eq(Integer.valueOf(1)), eq(Integer.valueOf(20)),
				eq("created_time desc"), appCaptor.capture());
		assertThat(appCaptor.getValue().getName()).isEqualTo("demo");
		assertThat(appCaptor.getValue().getIndustryId()).isEqualTo(Integer.valueOf(2));
		assertThat(appCaptor.getValue().getSceneId()).isEqualTo(Integer.valueOf(3));
		assertThat(appCaptor.getValue().getFramework()).isEqualTo("Vue");
		assertThat(appCaptor.getValue().getCreatedBy()).isEqualTo("alice");
	}

	@Test
	void disabledApplicationRemainsListedButRejectsApplicationOperations() throws Exception {
		App disabled = app(1, "disabled-app");
		disabled.setEnable(Boolean.FALSE);
		AppDto dto = new AppDto();
		dto.setApps(Collections.singletonList(disabled));
		dto.setTotal(Integer.valueOf(1));
		when(appService.queryAllAppByPage(eq(Integer.valueOf(1)), eq(Integer.valueOf(20)), isNull(),
				any(App.class))).thenReturn(dto);
		when(appService.requireEnabledApp(Integer.valueOf(1)))
				.thenThrow(new TinyEngineAccessDeniedException("TinyEngine application is disabled"));
		Page page = page(7, "home");
		page.setApp(Integer.valueOf(1));
		when(pageService.queryPageById(Integer.valueOf(7))).thenReturn(page);

		mvc.perform(get("/app-center/api/apps/page")
						.param("currentPage", "1").param("pageSize", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.apps[0].enable", is(false)));
		mvc.perform(get("/app-center/api/apps/preview/1"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/app-center/api/pages/list/1"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/app-center/api/apps/extension/list")
						.param("app", "1").param("category", "utils"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/app-center/api/apps/canvas/lock")
						.param("id", "7").param("state", "occupy").param("type", "page"))
				.andExpect(status().isForbidden());

		verify(canvasService, never()).lockCanvas(Integer.valueOf(7), "occupy", "page");
	}

	@Test
	void appCreateDetailAndSchemaUseCoreService() throws Exception {
		App created = app(9, "created-app");
		when(appService.createApp(any(App.class))).thenReturn(created);
		Map<String, Object> schema = new LinkedHashMap<String, Object>();
		schema.put("appId", Integer.valueOf(9));
		created.setConfig(schema);
		when(appService.queryAppById(Integer.valueOf(9))).thenReturn(created);
		AppDto list = new AppDto();
		list.setApps(Collections.singletonList(created));
		list.setTotal(Integer.valueOf(1));
		when(appService.queryAllAppByPage(eq(Integer.valueOf(1)), eq(Integer.valueOf(1000)), isNull(),
				any(App.class))).thenReturn(list);

		mvc.perform(post("/app-center/api/apps/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"created-app\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(9)));
		verify(appService).createApp(any(App.class));

		mvc.perform(get("/app-center/api/apps/detail/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name", is("created-app")));
		verify(appService).queryAppById(Integer.valueOf(9));
		mvc.perform(get("/app-center/api/apps/list/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name", is("created-app")));
		ArgumentCaptor<App> listCaptor = ArgumentCaptor.forClass(App.class);
		verify(appService).queryAllAppByPage(eq(Integer.valueOf(1)), eq(Integer.valueOf(1000)), isNull(),
				listCaptor.capture());
		assertThat(listCaptor.getValue().getPlatformId()).isEqualTo(Integer.valueOf(9));

		mvc.perform(get("/app-center/v1/api/apps/schema/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.appId", is(9)));
		mvc.perform(get("/app-center/api/apps/schema/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.appId", is(9)));
	}

	@Test
	void appSchemaProvidesFrontendDefaultsWhenStoredConfigIsEmpty() throws Exception {
		App app = app(1, "example");
		app.setConfig(Collections.<String, Object>emptyMap());
		app.setGlobalState(Collections.<Map<String, Object>>emptyList());
		Datasource datasource = new Datasource();
		datasource.setName("main");
		when(appService.queryAppById(Integer.valueOf(1))).thenReturn(app);
		when(pageService.queryAllPage(Integer.valueOf(1))).thenReturn(Collections.<Page>emptyList());
		when(datasourceService.queryDatasourceByCondition(any(Datasource.class)))
				.thenReturn(Collections.singletonList(datasource));
		when(appExtensionService.findAppExtensionByCondition(any(AppExtension.class)))
				.thenReturn(Collections.<AppExtension>emptyList());

		mvc.perform(get("/app-center/v1/api/apps/schema/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.componentsTree").isArray())
				.andExpect(jsonPath("$.data.componentsMap").isArray())
				.andExpect(jsonPath("$.data.dataSource.list[0].name", is("main")))
				.andExpect(jsonPath("$.data.i18n.zh_CN").isMap())
				.andExpect(jsonPath("$.data.i18n.en_US").isMap())
				.andExpect(jsonPath("$.data.utils").isArray())
				.andExpect(jsonPath("$.data.bridge").isArray())
				.andExpect(jsonPath("$.data.packages").isArray())
				.andExpect(jsonPath("$.data.meta.appId", is("1")))
				.andExpect(jsonPath("$.data.meta.globalState").isArray())
				.andExpect(jsonPath("$.data.globalState").isArray());
		mvc.perform(get("/app-center/api/apps/schema/components/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.componentsTree").isArray())
				.andExpect(jsonPath("$.data.componentsMap").isArray());
	}

	@Test
	void pageSaveLoopDelegatesToCoreServiceAndSetsPathId() throws Exception {
		Page created = page(7, "home");
		Page updated = page(7, "home-updated");
		when(pageService.createPage(any(Page.class))).thenReturn(created);
		when(pageService.updatePage(any(Page.class))).thenReturn(updated);
		when(pageService.queryAllPage(Integer.valueOf(9))).thenReturn(Arrays.asList(created, updated));
		when(pageService.queryPageById(Integer.valueOf(7))).thenReturn(updated);

		mvc.perform(post("/app-center/api/pages/create")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"home\",\"app\":9,\"isPage\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(7)));
		verify(pageService).createPage(any(Page.class));

		mvc.perform(post("/app-center/api/pages/update/7")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"home-updated\",\"app\":9,\"isPage\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name", is("home-updated")));
		ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
		verify(pageService).updatePage(pageCaptor.capture());
		assertThat(pageCaptor.getValue().getId()).isEqualTo(Integer.valueOf(7));

		mvc.perform(get("/app-center/api/pages/list/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[1].name", is("home-updated")));
		verify(pageService).queryAllPage(Integer.valueOf(9));

		mvc.perform(get("/app-center/api/pages/detail/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(7)));
		verify(pageService, times(2)).queryPageById(Integer.valueOf(7));
	}

	@Test
	void detailReturns404WhenCoreServiceReturnsNull() throws Exception {
		when(appService.queryAppById(Integer.valueOf(404))).thenReturn(null);
		when(pageService.queryPageById(Integer.valueOf(404))).thenReturn(null);

		mvc.perform(get("/app-center/api/apps/detail/404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success", is(false)));
		mvc.perform(get("/app-center/api/pages/detail/404"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success", is(false)));
	}

	@Test
	void appExtensionListRouteIsAvailable() throws Exception {
		AppExtension extension = new AppExtension();
		extension.setName("formatDate");
		when(appExtensionService.findAppExtensionByCondition(any(AppExtension.class)))
				.thenReturn(Collections.singletonList(extension));

		mvc.perform(get("/app-center/api/apps/extension/list")
						.param("app", "1")
						.param("category", "utils"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name", is("formatDate")));

		ArgumentCaptor<AppExtension> extensionCaptor = ArgumentCaptor.forClass(AppExtension.class);
		verify(appExtensionService).findAppExtensionByCondition(extensionCaptor.capture());
		assertThat(extensionCaptor.getValue().getApp()).isEqualTo(Integer.valueOf(1));
		assertThat(extensionCaptor.getValue().getCategory()).isEqualTo("utils");
	}

	@Test
	void previewMetadataRouteIsAvailable() throws Exception {
		App app = app(1, "example");
		Map<String, Object> dataSourceGlobal = new LinkedHashMap<String, Object>();
		dataSourceGlobal.put("timeout", Integer.valueOf(3000));
		app.setDataSourceGlobal(dataSourceGlobal);
		Map<String, Object> state = new LinkedHashMap<String, Object>();
		state.put("name", "counter");
		app.setGlobalState(Collections.singletonList(state));
		Datasource datasource = new Datasource();
		datasource.setName("main");
		SchemaI18n messages = new SchemaI18n();
		Map<String, String> zhCn = new LinkedHashMap<String, String>();
		zhCn.put("hello", "你好");
		messages.setZhCn(zhCn);
		I18nEntryListResult i18n = new I18nEntryListResult();
		i18n.setMessages(messages);
		AppExtension extension = new AppExtension();
		extension.setName("formatDate");
		extension.setType("function");

		when(appService.queryAppById(Integer.valueOf(1))).thenReturn(app);
		when(datasourceService.queryDatasourceByCondition(any(Datasource.class)))
				.thenReturn(Collections.singletonList(datasource));
		when(i18nEntryService.findI18nEntryByApp(Integer.valueOf(1), "app")).thenReturn(i18n);
		when(appExtensionService.findAppExtensionByCondition(any(AppExtension.class)))
				.thenReturn(Collections.singletonList(extension));

		mvc.perform(get("/app-center/api/preview/metadata")
						.param("platform", "1")
						.param("app", "1")
						.param("type", "page")
						.param("id", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.dataSource.list[0].name", is("main")))
				.andExpect(jsonPath("$.data.dataSource.timeout", is(3000)))
				.andExpect(jsonPath("$.data.globalState[0].name", is("counter")))
				.andExpect(jsonPath("$.data.i18n.zh_CN.hello", is("你好")))
				.andExpect(jsonPath("$.data.utils[0].name", is("formatDate")));
	}

	private static App app(Integer id, String name) {
		App app = new App();
		app.setId(id);
		app.setName(name);
		return app;
	}

	private static Page page(Integer id, String name) {
		Page page = new Page();
		page.setId(id);
		page.setName(name);
		page.setIsPage(Boolean.TRUE);
		return page;
	}
}

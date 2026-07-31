package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineAppPageMvcApiContractTest {

	private AppService appService;
	private PageService pageService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		appService = mock(AppService.class);
		pageService = mock(PageService.class);
		TinyEngineApiFacade facade = new DefaultTinyEngineApiFacade(appService, pageService);
		mvc = MockMvcBuilders.standaloneSetup(new TinyEngineAppMvcController(facade),
				new TinyEnginePageMvcController(facade)).build();
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
	void appCreateDetailAndSchemaUseCoreService() throws Exception {
		App created = app(9, "created-app");
		when(appService.createApp(any(App.class))).thenReturn(created);
		Map<String, Object> schema = new LinkedHashMap<String, Object>();
		schema.put("appId", Integer.valueOf(9));
		created.setConfig(schema);
		when(appService.queryAppById(Integer.valueOf(9))).thenReturn(created);

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

		mvc.perform(get("/app-center/v1/api/apps/schema/9"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.appId", is(9)));
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
		verify(pageService).queryPageById(Integer.valueOf(7));
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

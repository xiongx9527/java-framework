package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

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

class TinyEnginePageHistoryI18nMvcApiContractTest {

	private TinyEngineApiFacade facade;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		facade = mock(TinyEngineApiFacade.class);
		mvc = MockMvcBuilders.standaloneSetup(new TinyEnginePageMvcController(facade),
				new TinyEngineHistoryMvcController(facade), new TinyEngineI18nMvcController(facade),
				new TinyEngineTaskMvcController(facade)).build();
	}

	@Test
	void pageDeleteAndCopyUseOfficialRoutes() throws Exception {
		Page page = new Page();
		page.setId(Integer.valueOf(7));
		when(facade.deletePage(Integer.valueOf(7))).thenReturn(page);
		when(facade.copyPage(any(Page.class))).thenReturn(page);

		mvc.perform(get("/app-center/api/pages/delete/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(7)));
		mvc.perform(post("/app-center/api/pages/copy")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":7,\"name\":\"homeCopy\",\"route\":\"/homeCopy\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(7)));
	}

	@Test
	void historyListDetailAndRestoreUseFrontendContract() throws Exception {
		PageHistory history = new PageHistory();
		history.setId(Integer.valueOf(12));
		history.setPage(Integer.valueOf(7));
		Page restored = new Page();
		restored.setId(Integer.valueOf(7));
		restored.setLatestHistoryId(Integer.valueOf(13));
		when(facade.pageHistories(Integer.valueOf(7))).thenReturn(Collections.singletonList(history));
		when(facade.pageHistoryDetail(Integer.valueOf(12))).thenReturn(history);
		when(facade.restorePageHistory(any(Map.class))).thenReturn(restored);

		mvc.perform(get("/app-center/api/pages/histories").param("page", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id", is(12)));
		mvc.perform(get("/app-center/api/pages/histories/12"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.page", is(7)));
		mvc.perform(post("/app-center/api/pageHistory/restore")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":12}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(7)))
				.andExpect(jsonPath("$.data.latestHistoryId", is(13)));
	}

	@Test
	void i18nBulkDeleteUsesOfficialSnakeCaseFields() throws Exception {
		when(facade.bulkDeleteI18nEntries(any(DeleteI18nEntry.class))).thenReturn(Collections.emptyList());

		mvc.perform(post("/app-center/api/i18n/entries/bulk/delete")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"host\":\"1\",\"host_type\":\"app\",\"key_in\":[\"home.title\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray());

		verify(facade).bulkDeleteI18nEntries(any(DeleteI18nEntry.class));
	}

	@Test
	void taskStatusReturnsFullOfficialTaskRecord() throws Exception {
		TaskRecord task = new TaskRecord();
		task.setId(Integer.valueOf(1));
		task.setTaskStatus(Integer.valueOf(3));
		when(facade.taskStatus(eq(Integer.valueOf(1)))).thenReturn(task);

		mvc.perform(get("/app-center/api/tasks/status/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id", is(1)))
				.andExpect(jsonPath("$.data.taskStatus", is(3)));
	}

	@Test
	void missingHistoryAndTaskKeepOfficialSuccessNullSemantics() throws Exception {
		when(facade.pageHistoryDetail(Integer.valueOf(404))).thenReturn(null);
		when(facade.taskStatus(Integer.valueOf(404))).thenReturn(null);

		mvc.perform(get("/app-center/api/pages/histories/404"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)));
		mvc.perform(get("/app-center/api/tasks/status/404"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)));
	}
}

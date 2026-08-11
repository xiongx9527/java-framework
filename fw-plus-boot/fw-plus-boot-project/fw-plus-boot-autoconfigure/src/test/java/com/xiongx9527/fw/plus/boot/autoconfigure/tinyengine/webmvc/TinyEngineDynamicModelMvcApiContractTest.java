package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsertResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigInteger;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEngineDynamicModelMvcApiContractTest {

	private DynamicService dynamicService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		dynamicService = mock(DynamicService.class);
		mvc = MockMvcBuilders.standaloneSetup(new TinyEngineDynamicModelMvcController(dynamicService)).build();
	}

	@Test
	void queryApiAdaptsOfficialRequestAndPaginationShape() throws Exception {
		when(dynamicService.query(any(DynamicQuery.class))).thenReturn(new DynamicPageResult(
				Collections.singletonList(Collections.<String, Object>singletonMap("id", Integer.valueOf(7))),
				11L, 2, 5));

		mvc.perform(post("/platform-center/api/model-data/queryApi")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nameEn\":\"Customer\",\"nameCh\":\"客户\",\"fields\":[\"id\"],"
							+ "\"params\":{\"enabled\":true},\"currentPage\":2,\"pageSize\":5,"
							+ "\"orderBy\":\"id\",\"orderType\":\"DESC\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].id", is(7)))
				.andExpect(jsonPath("$.data.total", is(11)))
				.andExpect(jsonPath("$.data.pageNum", is(2)))
				.andExpect(jsonPath("$.data.pageSize", is(5)))
				.andExpect(jsonPath("$.data.pages", is(3)));

		ArgumentCaptor<DynamicQuery> captor = ArgumentCaptor.forClass(DynamicQuery.class);
		verify(dynamicService).query(captor.capture());
		DynamicQuery query = captor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals("Customer", query.getTableName());
		org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, query.getConditions().get("enabled"));
		org.junit.jupiter.api.Assertions.assertEquals("DESC", query.getOrderType());
	}

	@Test
	void insertUpdateAndDeleteUseOfficialNamesAndRealGeneratedId() throws Exception {
		BigInteger generatedId = new BigInteger("9223372036854775808123");
		when(dynamicService.insert(any(DynamicInsert.class))).thenReturn(new DynamicInsertResult(1, generatedId));
		when(dynamicService.update(any(DynamicUpdate.class))).thenReturn(1);
		when(dynamicService.delete(any(DynamicDelete.class))).thenReturn(1);

		mvc.perform(post("/platform-center/api/model-data/insertApi")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nameEn\":\"Customer\",\"params\":{\"name\":\"Alice\"}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.insert", is(1)))
				.andExpect(jsonPath("$.data.id", is(generatedId)));

		mvc.perform(post("/platform-center/api/model-data/updateApi")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nameEn\":\"Customer\",\"data\":{\"name\":\"Bob\"},\"params\":{\"id\":7}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.update", is(1)));

		mvc.perform(post("/platform-center/api/model-data/deleteApi")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nameEn\":\"Customer\",\"id\":7}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.delete", is(1)));

		ArgumentCaptor<DynamicInsert> insertCaptor = ArgumentCaptor.forClass(DynamicInsert.class);
		verify(dynamicService).insert(insertCaptor.capture());
		org.junit.jupiter.api.Assertions.assertEquals("Customer", insertCaptor.getValue().getTableName());
		org.junit.jupiter.api.Assertions.assertEquals("Alice", insertCaptor.getValue().getData().get("name"));

		ArgumentCaptor<DynamicDelete> deleteCaptor = ArgumentCaptor.forClass(DynamicDelete.class);
		verify(dynamicService).delete(deleteCaptor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(7), deleteCaptor.getValue().getConditions().get("id"));
	}

	@Test
	void unsafeIdentifiersAndMissingDeleteIdReturnFourHundred() throws Exception {
		doThrow(new TinyEngineSqlIdentifierException("invalid table name"))
				.when(dynamicService).query(any(DynamicQuery.class));

		mvc.perform(post("/platform-center/api/model-data/queryApi")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nameEn\":\"a;drop\",\"params\":{}}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is("400")));

		mvc.perform(post("/platform-center/api/model-data/deleteApi")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"nameEn\":\"Customer\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is("400")));
		verify(dynamicService, never()).delete(any(DynamicDelete.class));
	}
}

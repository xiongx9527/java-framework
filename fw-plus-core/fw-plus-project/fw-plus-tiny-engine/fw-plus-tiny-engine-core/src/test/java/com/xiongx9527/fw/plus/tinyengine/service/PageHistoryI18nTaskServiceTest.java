package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.TaskRecordMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;
import com.xiongx9527.fw.plus.tinyengine.service.impl.I18nEntryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.PageHistoryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.PageServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.TaskRecordServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageHistoryI18nTaskServiceTest {

	@Test
	void copyClearsSourceHistoryPointersAndPersistsTheNewPointerAfterHistoryCreation() {
		PageMapper pageMapper = mock(PageMapper.class);
		PageHistoryService historyService = mock(PageHistoryService.class);
		PageServiceImpl service = new PageServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", pageMapper);
		ReflectionTestUtils.setField(service, "pageHistoryService", historyService);
		ReflectionTestUtils.setField(service, "userContext", context("tenant-9"));
		Page source = new Page();
		source.setId(Integer.valueOf(7));
		source.setApp(Integer.valueOf(1));
		source.setIsPage(Boolean.TRUE);
		source.setTenantId("tenant-9");
		source.setLatestVersion("9");
		source.setLatestHistoryId(Integer.valueOf(99));
		Page request = new Page();
		request.setId(Integer.valueOf(7));
		request.setName("homeCopy");
		request.setRoute("/homeCopy");
		request.setLatestVersion("9");
		request.setLatestHistoryId(Integer.valueOf(99));
		Page created = new Page();
		created.setId(Integer.valueOf(8));
		created.setApp(Integer.valueOf(1));
		created.setName("homeCopy");
		created.setIsPage(Boolean.TRUE);
		created.setTenantId("tenant-9");
		when(pageMapper.queryPageById(Integer.valueOf(7))).thenReturn(source);
		when(pageMapper.createPage(any(Page.class))).thenAnswer(invocation -> {
			Page inserted = invocation.getArgument(0);
			inserted.setId(Integer.valueOf(8));
			return Integer.valueOf(1);
		});
		when(pageMapper.queryPageById(Integer.valueOf(8))).thenReturn(created);
		when(historyService.createPageHistory(any(PageHistory.class))).thenAnswer(invocation -> {
			PageHistory history = invocation.getArgument(0);
			history.setId(Integer.valueOf(100));
			return Integer.valueOf(1);
		});
		when(pageMapper.updatePageHistoryPointer(Integer.valueOf(8), "0", Integer.valueOf(100), "tenant-9"))
				.thenReturn(Integer.valueOf(1));

		Page result = service.copyPage(request);

		ArgumentCaptor<Page> inserted = ArgumentCaptor.forClass(Page.class);
		InOrder order = inOrder(pageMapper, historyService);
		order.verify(pageMapper).createPage(inserted.capture());
		assertThat(inserted.getValue().getLatestVersion()).isNull();
		assertThat(inserted.getValue().getLatestHistoryId()).isNull();
		order.verify(historyService).createPageHistory(any(PageHistory.class));
		order.verify(pageMapper).updatePageHistoryPointer(Integer.valueOf(8), "0", Integer.valueOf(100), "tenant-9");
		assertThat(result.getLatestVersion()).isEqualTo("0");
		assertThat(result.getLatestHistoryId()).isEqualTo(Integer.valueOf(100));
	}

	@Test
	void historyQueriesAndTaskStatusUseCurrentTenant() {
		TinyEngineUserContext context = context("tenant-9");
		PageHistoryMapper historyMapper = mock(PageHistoryMapper.class);
		PageHistoryServiceImpl historyService = new PageHistoryServiceImpl();
		ReflectionTestUtils.setField(historyService, "baseMapper", historyMapper);
		ReflectionTestUtils.setField(historyService, "userContext", context);
		PageHistory history = new PageHistory();
		history.setId(Integer.valueOf(12));
		history.setTenantId("tenant-9");
		when(historyMapper.queryPageHistoryById(Integer.valueOf(12))).thenReturn(history);

		assertThat(historyService.findPageHistoryById(Integer.valueOf(12))).isSameAs(history);
		ArgumentCaptor<PageHistory> condition = ArgumentCaptor.forClass(PageHistory.class);
		historyService.findPageHistories(Integer.valueOf(7));
		verify(historyMapper).queryPageHistoryByCondition(condition.capture());
		assertThat(condition.getValue().getPage()).isEqualTo(Integer.valueOf(7));
		assertThat(condition.getValue().getTenantId()).isEqualTo("tenant-9");

		TaskRecordMapper taskMapper = mock(TaskRecordMapper.class);
		TaskRecordServiceImpl taskService = new TaskRecordServiceImpl();
		ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
		ReflectionTestUtils.setField(taskService, "userContext", context);
		TaskRecord task = new TaskRecord();
		task.setId(Integer.valueOf(1));
		when(taskMapper.queryTaskRecordByCondition(any(TaskRecord.class))).thenReturn(Collections.singletonList(task));
		assertThat(taskService.taskStatus(Integer.valueOf(1))).isSameAs(task);
		verify(taskMapper, never()).queryTaskRecordById(Integer.valueOf(1));
	}

	@Test
	void restoreUpdatesCurrentPageAndCreatesANewHistoryVersion() {
		PageMapper pageMapper = mock(PageMapper.class);
		PageHistoryService historyService = mock(PageHistoryService.class);
		PageServiceImpl service = new PageServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", pageMapper);
		ReflectionTestUtils.setField(service, "pageHistoryService", historyService);
		ReflectionTestUtils.setField(service, "userContext", context("tenant-9"));
		PageHistory history = new PageHistory();
		history.setId(Integer.valueOf(12));
		history.setPage(Integer.valueOf(7));
		history.setName("restored");
		history.setTenantId("tenant-9");
		Page current = new Page();
		current.setId(Integer.valueOf(7));
		current.setApp(Integer.valueOf(1));
		current.setTenantId("tenant-9");
		Page updated = new Page();
		updated.setId(Integer.valueOf(7));
		updated.setApp(Integer.valueOf(1));
		updated.setName("restored");
		updated.setTenantId("tenant-9");
		when(historyService.findPageHistoryById(Integer.valueOf(12))).thenReturn(history);
		when(pageMapper.queryPageById(Integer.valueOf(7))).thenReturn(current, current, updated, updated);
		when(pageMapper.updatePageById(any(Page.class))).thenReturn(Integer.valueOf(1));
		when(historyService.selectMaxVersionOfPageHistory("restored", Integer.valueOf(1))).thenReturn("4");
		when(historyService.createPageHistory(any(PageHistory.class))).thenAnswer(invocation -> {
			PageHistory created = invocation.getArgument(0);
			created.setId(Integer.valueOf(13));
			return Integer.valueOf(1);
		});
		when(pageMapper.updatePageHistoryPointer(Integer.valueOf(7), "4", Integer.valueOf(13), "tenant-9"))
				.thenReturn(Integer.valueOf(1));

		Page result = service.restorePageHistory(Integer.valueOf(12));

		assertThat(result).isSameAs(updated);
		assertThat(result.getLatestHistoryId()).isEqualTo(Integer.valueOf(13));
		ArgumentCaptor<PageHistory> created = ArgumentCaptor.forClass(PageHistory.class);
		InOrder order = inOrder(pageMapper, historyService);
		order.verify(pageMapper).updatePageById(any(Page.class));
		order.verify(historyService).createPageHistory(created.capture());
		order.verify(pageMapper).updatePageHistoryPointer(Integer.valueOf(7), "4", Integer.valueOf(13), "tenant-9");
		assertThat(created.getValue().getId()).isEqualTo(Integer.valueOf(13));
		assertThat(created.getValue().getPage()).isEqualTo(Integer.valueOf(7));
	}

	@Test
	void bulkDeletePropagatesDatabaseFailureAndIsTransactional() throws Exception {
		I18nEntryMapper mapper = mock(I18nEntryMapper.class);
		I18nEntryServiceImpl service = new I18nEntryServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context("tenant-9"));
		DeleteI18nEntry request = new DeleteI18nEntry();
		request.setHost("1");
		request.setHostType("app");
		request.setKeyIn(Arrays.asList("home.title", "home.subtitle"));
		I18nEntryDto first = new I18nEntryDto();
		first.setId(Integer.valueOf(21));
		I18nEntryDto second = new I18nEntryDto();
		second.setId(Integer.valueOf(22));
		when(mapper.queryI18nEntryByCondition(any())).thenReturn(Collections.singletonList(first),
				Collections.singletonList(second));
		when(mapper.deleteI18nEntryById(Integer.valueOf(21))).thenReturn(Integer.valueOf(1));
		when(mapper.deleteI18nEntryById(Integer.valueOf(22))).thenThrow(new IllegalStateException("db failed"));

		assertThatThrownBy(() -> service.deleteI18nEntriesByHostAndHostTypeAndKey(request))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("db failed");
		Transactional transaction = I18nEntryServiceImpl.class
				.getMethod("deleteI18nEntriesByHostAndHostTypeAndKey", DeleteI18nEntry.class)
				.getAnnotation(Transactional.class);
		assertThat(transaction).isNotNull();
		assertThat(transaction.rollbackFor()).contains(Exception.class);
	}

	private TinyEngineUserContext context(final String tenantId) {
		return new TinyEngineUserContext() {
			@Override
			public TinyEngineUser getCurrentUser() {
				return new TinyEngineUser("7", "alice", tenantId, "renter-1", Integer.valueOf(1));
			}
		};
	}
}

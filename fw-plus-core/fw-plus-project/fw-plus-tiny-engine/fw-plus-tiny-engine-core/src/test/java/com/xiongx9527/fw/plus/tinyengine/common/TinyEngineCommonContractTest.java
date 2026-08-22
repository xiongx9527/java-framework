package com.xiongx9527.fw.plus.tinyengine.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.support.context.SystemTinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineCommonContractTest {

	@Test
	void resultShouldExposeSuccessAndFailureShapes() {
		Result<String> success = Result.success("page-id");
		Result<Object> failure = Result.failed(ExceptionEnum.CM002, "invalid page");

		assertTrue(success.isSuccess());
		assertEquals(ExceptionEnum.SUCCESS.getResultCode(), success.getCode());
		assertEquals("page-id", success.getData());
		assertFalse(failure.isSuccess());
		assertEquals(ExceptionEnum.CM002.getResultCode(), failure.getCode());
		assertEquals("invalid page", failure.getMessage());
		assertEquals("invalid page", failure.getErrMsg());
		assertEquals(ExceptionEnum.CM002.getResultCode(), failure.getError().get("code"));
	}

	@Test
	void serviceExceptionShouldRetainCodeAndMessage() {
		ServiceException exception = new ServiceException("TE001", "page not found");

		assertEquals("TE001", exception.getServiceCode());
		assertEquals("page not found", exception.getMessage());
	}

	@Test
	void pageQueryShouldUseCoreOnlyDefaultsAndBounds() {
		PageQueryVo<String> query = new PageQueryVo<String>();
		query.setPageSize(PageQueryVo.PAGESIZE_MAX + 1);
		query.setCurrent(0);

		assertEquals(PageQueryVo.PAGESIZE_DEFAULT, new PageQueryVo<Object>().getPageSize());
		assertEquals(1, new PageQueryVo<Object>().getCurrent());
		assertEquals(PageQueryVo.PAGESIZE_MAX, query.getPageSize());
		assertEquals(1, query.getCurrent());

		query.setCurrent(-1);
		assertEquals(1, query.getCurrent());
		assertEquals(1, query.getPage().getCurrent());
		assertNull(query.getData());
	}

	@Test
	void exceptionEnumShouldUseItsDeclaredCode() {
		assertEquals("CM1110", ExceptionEnum.CM1110.getResultCode());
	}

	@Test
	void systemUserContextShouldReturnStableSystemUser() {
		SystemTinyEngineUserContext context = new SystemTinyEngineUserContext();
		TinyEngineUser first = context.getCurrentUser();
		TinyEngineUser second = context.getCurrentUser();

		assertEquals(first.getUserId(), second.getUserId());
		assertEquals(first.getUserName(), second.getUserName());
		assertEquals("system", first.getUserId());
		assertEquals("system", first.getUserName());
	}

	@Test
	void baseEntityIdShouldUseDatabaseAutoIncrement() throws Exception {
		Field id = BaseEntity.class.getDeclaredField("id");
		TableId tableId = id.getAnnotation(TableId.class);

		assertNotNull(tableId);
		assertEquals("id", tableId.value());
		assertEquals(IdType.AUTO, tableId.type());
	}
}

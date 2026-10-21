package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;

/**
 * 锁定 Boot 层框架异常的固定状态码。
 */
class BootExceptionCodeTest {

	@Test
	void dbTxExceptionCarriesDatabaseCode() {
		assertEquals(50_009_009, DbTxException.CODE);
		assertEquals(50_009_009,
				new DbTxException(DbTxErrorCode.DBTX_VALIDATION_FAILED, "事务校验失败").getCode());
	}

	@Test
	void javaEntityCompilationExceptionCarriesScriptCode() {
		assertEquals(50_029_001, JavaEntityCompilationException.CODE);

		JavaEntityDiagnostic diagnostic = new JavaEntityDiagnostic("order", 1, 1, "compiler.test", "无法解析类型 X");
		JavaEntityCompilationException exception = new JavaEntityCompilationException(
				Collections.singletonList(diagnostic));
		assertEquals(50_029_001, exception.getCode());
		assertEquals("无法解析类型 X", exception.getMessage());
	}
}

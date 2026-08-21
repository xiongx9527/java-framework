package cn.afterturn.easypoi.excel.export;

import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FwPlusExcelTransServiceTest {

	private final FwPlusExcelTransService service = new FwPlusExcelTransService() {
	};

	@Test
	public void transExportCollectionReturnsSameCollectionWhenParamsAreNull() {
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transExportCollection(null, rows);

		assertSame(rows, result);
	}

	@Test
	public void transExportCollectionReturnsSameCollectionWhenNeedTransIsFalse() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(false);
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transExportCollection(params, rows);

		assertSame(rows, result);
	}

	@Test
	public void transExportCollectionReturnsNullCollectionWhenNeedTransIsTrue() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(true);

		Collection<?> result = service.transExportCollection(params, null);

		assertSame(null, result);
	}

	@Test
	public void transExportCollectionReturnsSameEmptyCollectionWhenNeedTransIsTrue() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(true);
		List<String> rows = new ArrayList<>();

		Collection<?> result = service.transExportCollection(params, rows);

		assertSame(rows, result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void shouldTransReturnsFalseForEmptyCollection() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(true);

		assertTrue(!service.shouldTrans(params, Collections.emptyList()));
	}

	@Test
	public void shouldTransReturnsFalseWhenParamsAreNull() {
		assertTrue(!service.shouldTrans(null, Collections.singletonList("row")));
	}

	@Test
	public void transImportCollectionReturnsSameCollectionWhenParamsAreNull() {
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transImportCollection(null, rows);

		assertSame(rows, result);
	}

	@Test
	public void transImportCollectionReturnsSameCollectionWhenNeedTransIsFalse() {
		FwPlusImportParams params = new FwPlusImportParams();
		params.setNeedTrans(false);
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transImportCollection(params, rows);

		assertSame(rows, result);
	}

	@Test
	public void transImportCollectionReturnsNullCollectionWhenNeedTransIsTrue() {
		FwPlusImportParams params = new FwPlusImportParams();
		params.setNeedTrans(true);

		Collection<?> result = service.transImportCollection(params, null);

		assertSame(null, result);
	}

	@Test
	public void transImportCollectionReturnsSameEmptyCollectionWhenNeedTransIsTrue() {
		FwPlusImportParams params = new FwPlusImportParams();
		params.setNeedTrans(true);
		List<String> rows = new ArrayList<>();

		Collection<?> result = service.transImportCollection(params, rows);

		assertSame(rows, result);
		assertTrue(result.isEmpty());
	}
}

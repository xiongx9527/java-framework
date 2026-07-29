package cn.afterturn.easypoi.excel;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusTemplateExportParams;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.export.FwPlusExcelBatchExportService;
import cn.afterturn.easypoi.excel.export.FwPlusExcelExportService;
import cn.afterturn.easypoi.handler.inter.IWriter;
import cn.afterturn.easypoi.util.TransUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExcelExportUtilFwPlusBoundaryTest {

	@Test
	public void exportExcelAcceptsStandardExportParams() throws Exception {
		ExportParams params = new ExportParams("用户", "用户列表", ExcelType.HSSF);
		List<SimpleRow> rows = new ArrayList<>(Collections.singletonList(new SimpleRow("张三")));

		Workbook workbook = ExcelExportUtil.exportExcel(params, SimpleRow.class, rows);

		assertNotNull(workbook);
		assertEquals(1, workbook.getNumberOfSheets());
		workbook.close();
	}

	@Test
	public void exportExcelAcceptsEmptyCollectionWithStandardExportParams() throws Exception {
		ExportParams params = new ExportParams("用户", "用户列表", ExcelType.HSSF);
		Collection<SimpleRow> rows = new ArrayList<>();

		Workbook workbook = ExcelExportUtil.exportExcel(params, SimpleRow.class, rows);

		assertNotNull(workbook);
		assertEquals(1, workbook.getNumberOfSheets());
		workbook.close();
	}

	@Test
	public void exportBigExcelAcceptsStandardExportParams() {
		ExportParams params = new ExportParams("用户", "用户列表", ExcelType.HSSF);

		IWriter<Workbook> writer = ExcelExportUtil.exportBigExcel(params, SimpleRow.class);

		assertNotNull(writer);
	}

	@Test
	public void templateExportAcceptsStandardTemplateExportParams() {
		TemplateExportParams params = new TemplateExportParams("missing-template.xlsx");
		Map<String, Object> map = new HashMap<>();

		assertDoesNotThrow(() ->
			ExcelExportUtil.exportExcel(params, SimpleRow.class, Collections.<SimpleRow>emptyList(), map)
		);
	}

	@Test
	public void templateExportUsesTranslatedFwPlusCollection() throws Exception {
		FwPlusTemplateExportParams params = new FwPlusTemplateExportParams(templateInputStream()) {
			@Override
			public boolean isNeedTrans() {
				return true;
			}
		};
		params.setHeadingStartRow(0);
		Collection<SimpleRow> rows = new TranslatedThenOriginalRows();
		Map<String, Object> map = new HashMap<>();

		TransUtilsState state = setTransUtilsState(null, 1);
		try {
			Workbook workbook = ExcelExportUtil.exportExcel(params, SimpleRow.class, rows, map);

			assertNotNull(workbook);
			assertEquals("译值", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
			workbook.close();
		} finally {
			restoreTransUtilsState(state);
		}
	}

	@Test
	public void exportServiceClearsFwPlusParamsWhenReinitializedWithStandardExportParams() throws Exception {
		FwPlusExcelExportService service = new FwPlusExcelExportService();
		service.init(new FwPlusExportParams());

		service.init(new ExportParams("用户", "用户列表", ExcelType.HSSF));

		assertEquals(null, readFwPlusExportParams(service));
	}

	@Test
	public void batchExportServiceClearsFwPlusParamsWhenReinitializedWithStandardExportParams() throws Exception {
		FwPlusExcelBatchExportService service = new FwPlusExcelBatchExportService();
		service.init(new FwPlusExportParams(), SimpleRow.class);

		service.init(new ExportParams("用户", "用户列表", ExcelType.HSSF), SimpleRow.class);

		assertEquals(null, readFwPlusExportParams(service));
	}

	@Test
	public void batchExportServiceListInitTracksFwPlusParamsAndClearsStandardParams() throws Exception {
		FwPlusExcelBatchExportService service = new FwPlusExcelBatchExportService();
		FwPlusExportParams fwPlusParams = new FwPlusExportParams();
		List<ExcelExportEntity> excelParams = new ArrayList<>();

		service.init(fwPlusParams, excelParams);
		assertEquals(fwPlusParams, readFwPlusExportParams(service));

		service.init(new ExportParams("用户", "用户列表", ExcelType.HSSF), excelParams);
		assertEquals(null, readFwPlusExportParams(service));
	}

	private Object readFwPlusExportParams(Object service) throws Exception {
		Field field = service.getClass().getDeclaredField("fwPlusExportParams");
		field.setAccessible(true);
		return field.get(service);
	}

	private ByteArrayInputStream templateInputStream() throws Exception {
		Workbook template = new HSSFWorkbook();
		template.createSheet().createRow(0).createCell(0).setCellValue("姓名");
		template.getSheetAt(0).createRow(1);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		template.write(output);
		template.close();
		return new ByteArrayInputStream(output.toByteArray());
	}

	private TransUtilsState setTransUtilsState(Object transService, int initFlag) throws Exception {
		Field transServiceField = TransUtils.class.getDeclaredField("transService");
		transServiceField.setAccessible(true);
		Field initFlagField = TransUtils.class.getDeclaredField("initFlag");
		initFlagField.setAccessible(true);
		TransUtilsState state = new TransUtilsState(transServiceField.get(null), ((AtomicInteger) initFlagField.get(null)).get());
		transServiceField.set(null, transService);
		((AtomicInteger) initFlagField.get(null)).set(initFlag);
		return state;
	}

	private void restoreTransUtilsState(TransUtilsState state) throws Exception {
		Field transServiceField = TransUtils.class.getDeclaredField("transService");
		transServiceField.setAccessible(true);
		transServiceField.set(null, state.transService);
		Field initFlagField = TransUtils.class.getDeclaredField("initFlag");
		initFlagField.setAccessible(true);
		((AtomicInteger) initFlagField.get(null)).set(state.initFlag);
	}

	public static class SimpleRow {
		@Excel(name = "姓名")
		private String name;

		public SimpleRow() {
		}

		public SimpleRow(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private static class TranslatedThenOriginalRows extends AbstractCollection<SimpleRow> {
		private int iteratorCount;

		@Override
		public Iterator<SimpleRow> iterator() {
			iteratorCount++;
			return Collections.singletonList(new SimpleRow(iteratorCount == 1 ? "译值" : "原值")).iterator();
		}

		@Override
		public int size() {
			return 1;
		}
	}

	private static class TransUtilsState {
		private final Object transService;
		private final int initFlag;

		private TransUtilsState(Object transService, int initFlag) {
			this.transService = transService;
			this.initFlag = initFlag;
		}
	}
}

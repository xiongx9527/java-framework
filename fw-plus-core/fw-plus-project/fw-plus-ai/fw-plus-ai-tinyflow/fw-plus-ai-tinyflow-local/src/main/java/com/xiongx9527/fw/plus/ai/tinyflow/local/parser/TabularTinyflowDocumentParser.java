package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TabularTinyflowDocumentParser implements TinyflowDocumentParser {

	@Override
	public boolean supports(String mediaType, String fileName) {
		String type = lower(mediaType);
		String name = lower(fileName);
		return type.contains("text/csv") || type.contains("spreadsheetml.sheet")
				|| name.endsWith(".csv") || name.endsWith(".xlsx");
	}

	@Override
	public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
		String name = lower(context.getFileName());
		return name.endsWith(".csv") || lower(context.getMediaType()).contains("text/csv")
				? parseCsv(input, context) : parseWorkbook(input, context);
	}

	private TinyflowParsedDocument parseCsv(InputStream input, ParseContext context) throws IOException {
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			int row = 0;
			while ((line = reader.readLine()) != null) {
				row++;
				String value = join(parseCsvRow(line));
				if (!value.trim().isEmpty()) {
					addRow(sections, content, "csv", row, value);
				}
			}
		}
		return result(context, sections, content);
	}

	private TinyflowParsedDocument parseWorkbook(InputStream input, ParseContext context) throws IOException {
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		DataFormatter formatter = new DataFormatter(Locale.ROOT);
		try (Workbook workbook = WorkbookFactory.create(input)) {
			for (Sheet sheet : workbook) {
				for (Row row : sheet) {
					List<String> values = new ArrayList<String>();
					int lastCell = Math.max(row.getLastCellNum(), 0);
					for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
						Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
						values.add(cell == null ? "" : formatter.formatCellValue(cell));
					}
					String value = join(values);
					if (!value.trim().isEmpty()) {
						addRow(sections, content, sheet.getSheetName(), row.getRowNum() + 1, value);
					}
				}
			}
		} catch (RuntimeException exception) {
			throw new IOException("Unable to parse XLSX document", exception);
		}
		return result(context, sections, content);
	}

	private TinyflowParsedDocument result(ParseContext context,
			List<TinyflowParsedDocument.Section> sections, StringBuilder content) throws IOException {
		if (content.length() == 0) {
			throw new IOException("Tabular document contains no rows");
		}
		return new TinyflowParsedDocument(context.getFileName(), content.toString(), sections,
				TextMarkdownHtmlParser.parserMetadata("tabular"));
	}

	private void addRow(List<TinyflowParsedDocument.Section> sections, StringBuilder content,
			String sheet, int row, String value) {
		if (content.length() > 0) {
			content.append('\n');
		}
		content.append(value);
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("sheet", sheet);
		metadata.put("row", row);
		sections.add(new TinyflowParsedDocument.Section(value,
				"sheet:" + sheet + ",row:" + row, metadata));
	}

	private List<String> parseCsvRow(String line) {
		List<String> values = new ArrayList<String>();
		StringBuilder value = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char current = line.charAt(index);
			if (current == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					value.append('"');
					index++;
				} else {
					quoted = !quoted;
				}
			} else if (current == ',' && !quoted) {
				values.add(value.toString());
				value.setLength(0);
			} else {
				value.append(current);
			}
		}
		values.add(value.toString());
		return values;
	}

	private String join(List<String> values) {
		StringBuilder result = new StringBuilder();
		for (String value : values) {
			if (result.length() > 0) {
				result.append('\t');
			}
			result.append(value);
		}
		return result.toString();
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}

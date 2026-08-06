package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
		TinyflowParserLimits limits = TinyflowParserLimits.from(context);
		String source = new String(limits.read(input), StandardCharsets.UTF_8);
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		List<List<String>> rows = parseCsvRows(source, limits);
		for (int index = 0; index < rows.size(); index++) {
			String value = join(rows.get(index));
			if (!value.trim().isEmpty()) {
				addRow(sections, content, "csv", index + 1, value);
			}
		}
		limits.checkText(content.toString());
		return result(context, sections, content);
	}

	private TinyflowParsedDocument parseWorkbook(InputStream input, ParseContext context) throws IOException {
		TinyflowParserLimits limits = TinyflowParserLimits.from(context);
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		DataFormatter formatter = new DataFormatter(Locale.ROOT);
		int rowCount = 0;
		int cellCount = 0;
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(limits.read(input)))) {
			for (Sheet sheet : workbook) {
				for (Row row : sheet) {
					rowCount++;
					limits.checkTabularRows(rowCount);
					List<String> values = new ArrayList<String>();
					int lastCell = Math.max(row.getLastCellNum(), 0);
					for (int cellIndex = 0; cellIndex < lastCell; cellIndex++) {
						cellCount++;
						limits.checkTabularCells(cellCount);
						Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
						String value = cell == null ? "" : formatter.formatCellValue(cell);
						limits.checkField(value.length());
						values.add(value);
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
		limits.checkText(content.toString());
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

	private List<List<String>> parseCsvRows(String source, TinyflowParserLimits limits) throws IOException {
		List<List<String>> rows = new ArrayList<List<String>>();
		List<String> values = new ArrayList<String>();
		StringBuilder value = new StringBuilder();
		boolean quoted = false;
		boolean afterQuote = false;
		boolean fieldStarted = false;
		boolean recordStarted = false;
		int cells = 0;
		for (int index = 0; index < source.length(); index++) {
			char current = source.charAt(index);
			boolean lineBreak = current == '\n' || current == '\r';
			if (quoted) {
				if (current == '"') {
					if (index + 1 < source.length() && source.charAt(index + 1) == '"') {
						value.append('"');
						limits.checkField(value.length());
						index++;
					} else {
						quoted = false;
						afterQuote = true;
					}
				} else {
					if (lineBreak) {
						value.append('\n');
						if (current == '\r' && index + 1 < source.length() && source.charAt(index + 1) == '\n') {
							index++;
						}
					} else {
						value.append(current);
					}
					limits.checkField(value.length());
				}
				continue;
			}
			if (afterQuote) {
				if (current == ',') {
					cells = addCsvField(values, value, limits, cells);
					afterQuote = false;
					fieldStarted = false;
					recordStarted = true;
					continue;
				}
				if (lineBreak) {
					cells = addCsvField(values, value, limits, cells);
					addCsvRow(rows, values, limits);
					values = new ArrayList<String>();
					afterQuote = false;
					fieldStarted = false;
					recordStarted = false;
					if (current == '\r' && index + 1 < source.length() && source.charAt(index + 1) == '\n') {
						index++;
					}
					continue;
				}
				throw new IOException("Malformed CSV: invalid character after closing quote");
			}
			if (current == '"') {
				if (fieldStarted) {
					throw new IOException("Malformed CSV: quote must start a field");
				}
				quoted = true;
				fieldStarted = true;
				recordStarted = true;
			} else if (current == ',') {
				cells = addCsvField(values, value, limits, cells);
				fieldStarted = false;
				recordStarted = true;
			} else if (lineBreak) {
				cells = addCsvField(values, value, limits, cells);
				addCsvRow(rows, values, limits);
				values = new ArrayList<String>();
				fieldStarted = false;
				recordStarted = false;
				if (current == '\r' && index + 1 < source.length() && source.charAt(index + 1) == '\n') {
					index++;
				}
			} else {
				value.append(current);
				limits.checkField(value.length());
				fieldStarted = true;
				recordStarted = true;
			}
		}
		if (quoted) {
			throw new IOException("Malformed CSV: unclosed quoted field");
		}
		if (recordStarted || afterQuote || !values.isEmpty() || value.length() > 0) {
			addCsvField(values, value, limits, cells);
			addCsvRow(rows, values, limits);
		}
		return rows;
	}

	private int addCsvField(List<String> values, StringBuilder value,
			TinyflowParserLimits limits, int cells) throws IOException {
		limits.checkField(value.length());
		limits.checkTabularCells(++cells);
		values.add(value.toString());
		value.setLength(0);
		return cells;
	}

	private void addCsvRow(List<List<String>> rows, List<String> values,
			TinyflowParserLimits limits) throws IOException {
		limits.checkTabularRows(rows.size() + 1);
		rows.add(values);
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

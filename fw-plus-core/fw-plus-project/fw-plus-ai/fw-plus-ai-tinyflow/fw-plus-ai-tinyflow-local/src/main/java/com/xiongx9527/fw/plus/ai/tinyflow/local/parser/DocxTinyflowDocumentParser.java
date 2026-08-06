package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DocxTinyflowDocumentParser implements TinyflowDocumentParser {

	@Override
	public boolean supports(String mediaType, String fileName) {
		String type = lower(mediaType);
		String name = lower(fileName);
		return type.contains("wordprocessingml.document") || name.endsWith(".docx");
	}

	@Override
	public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
		TinyflowParserLimits limits = TinyflowParserLimits.from(context);
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(limits.read(input)))) {
			limits.checkDocxParagraphs(document.getParagraphs().size());
			int paragraph = 0;
			for (XWPFParagraph value : document.getParagraphs()) {
				paragraph++;
				String text = value.getText().trim();
				if (!text.isEmpty()) {
					Map<String, Object> metadata = new LinkedHashMap<String, Object>();
					metadata.put("paragraph", paragraph);
					add(sections, content, text, "paragraph:" + paragraph, metadata);
				}
			}
			int tableNumber = 0;
			int totalRows = 0;
			int totalCells = 0;
			for (XWPFTable table : document.getTables()) {
				tableNumber++;
				limits.checkDocxTables(tableNumber);
				int rowNumber = 0;
				for (XWPFTableRow row : table.getRows()) {
					rowNumber++;
					totalRows++;
					limits.checkDocxRows(totalRows);
					StringBuilder rowText = new StringBuilder();
					for (XWPFTableCell cell : row.getTableCells()) {
						totalCells++;
						limits.checkDocxCells(totalCells);
						limits.checkField(cell.getText().length());
						if (rowText.length() > 0) {
							rowText.append('\t');
						}
						rowText.append(cell.getText());
					}
					if (rowText.toString().trim().isEmpty()) {
						continue;
					}
					Map<String, Object> metadata = new LinkedHashMap<String, Object>();
					metadata.put("table", tableNumber);
					metadata.put("row", rowNumber);
					add(sections, content, rowText.toString(),
							"table:" + tableNumber + ",row:" + rowNumber, metadata);
				}
			}
		}
		if (content.length() == 0) {
			throw new IOException("DOCX contains no extractable text");
		}
		limits.checkText(content.toString());
		return new TinyflowParsedDocument(context.getFileName(), content.toString(), sections,
				TextMarkdownHtmlParser.parserMetadata("docx"));
	}

	private void add(List<TinyflowParsedDocument.Section> sections, StringBuilder content,
			String text, String location, Map<String, Object> metadata) {
		if (content.length() > 0) {
			content.append('\n');
		}
		content.append(text);
		sections.add(new TinyflowParsedDocument.Section(text, location, metadata));
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}

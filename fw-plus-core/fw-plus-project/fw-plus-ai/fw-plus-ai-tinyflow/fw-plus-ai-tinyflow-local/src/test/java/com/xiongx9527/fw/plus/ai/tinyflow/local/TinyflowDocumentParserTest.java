package com.xiongx9527.fw.plus.ai.tinyflow.local;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.local.parser.CompositeTinyflowDocumentParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowDocumentParserTest {

	private final CompositeTinyflowDocumentParser parser = CompositeTinyflowDocumentParser.defaults();

	@Test
	public void shouldParseTextMarkdownAndHtmlFixturesWithLineLocations() throws Exception {
		assertTextFixture("notes.txt", "text/plain", "first line\nsecond line", "first line", 1);
		assertTextFixture("guide.md", "text/markdown", "# Guide\nMarkdown body", "# Guide", 1);
		assertTextFixture("page.html", "text/html", "<h1>Title</h1><p>HTML body</p>", "Title", 1);
	}

	@Test
	public void shouldParseRealPdfFixtureByPage() throws Exception {
		TinyflowParsedDocument parsed = parse("manual.pdf", "application/pdf", pdfFixture());

		assertTrue(parsed.getContent().contains("PDF page one"));
		assertEquals("pdf", parsed.getMetadata().get("parserKey"));
		assertEquals("1", parsed.getMetadata().get("parserVersion"));
		assertEquals(1, parsed.getSections().get(0).getMetadata().get("page"));
		assertEquals("page:1", parsed.getSections().get(0).getLocation());
	}

	@Test
	public void shouldParseRealDocxFixtureByParagraph() throws Exception {
		TinyflowParsedDocument parsed = parse("manual.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxFixture());

		assertTrue(parsed.getContent().contains("DOCX paragraph"));
		assertEquals("docx", parsed.getMetadata().get("parserKey"));
		assertEquals(1, parsed.getSections().get(0).getMetadata().get("paragraph"));
	}

	@Test
	public void shouldParseCsvAndRealXlsxFixturesBySheetAndRow() throws Exception {
		TinyflowParsedDocument csv = parse("people.csv", "text/csv",
				"name,city\nAlice,Shanghai".getBytes(StandardCharsets.UTF_8));
		assertEquals("tabular", csv.getMetadata().get("parserKey"));
		assertEquals("csv", csv.getSections().get(1).getMetadata().get("sheet"));
		assertEquals(2, csv.getSections().get(1).getMetadata().get("row"));

		TinyflowParsedDocument xlsx = parse("people.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxFixture());
		assertTrue(xlsx.getContent().contains("Alice"));
		assertEquals("People", xlsx.getSections().get(1).getMetadata().get("sheet"));
		assertEquals(2, xlsx.getSections().get(1).getMetadata().get("row"));
	}

	@Test
	public void shouldSelectByExtensionAndAllowCustomParserExtension() throws Exception {
		assertTrue(parser.supports(null, "UPPER.PDF"));
		assertFalse(parser.supports("application/octet-stream", "archive.zip"));

		TinyflowDocumentParser custom = new TinyflowDocumentParser() {
			@Override
			public boolean supports(String mediaType, String fileName) {
				return fileName != null && fileName.endsWith(".custom");
			}

			@Override
			public TinyflowParsedDocument parse(java.io.InputStream input, ParseContext context) {
				return new TinyflowParsedDocument("custom", "custom content", Collections.emptyList(),
						Collections.<String, Object>singletonMap("parserKey", "custom"));
			}
		};
		CompositeTinyflowDocumentParser extensible = new CompositeTinyflowDocumentParser(Arrays.asList(custom));
		TinyflowParsedDocument parsed = extensible.parse(new ByteArrayInputStream(new byte[0]),
				context("data.custom", "application/octet-stream"));
		assertEquals("custom", parsed.getMetadata().get("parserKey"));
	}

	@Test
	public void shouldPreferBinaryExtensionOverMisleadingTextMediaType() throws Exception {
		TinyflowParsedDocument parsed = parse("manual.pdf", "text/plain", pdfFixture());

		assertEquals("pdf", parsed.getMetadata().get("parserKey"));
		assertTrue(parsed.getContent().contains("PDF page one"));
	}

	@Test
	public void shouldEnforceBoundedInputAndFormatSpecificLimits() throws Exception {
		assertLimit("notes.txt", "text/plain", "12345".getBytes(StandardCharsets.UTF_8), "maxInputBytes", 4);
		assertLimit("notes.txt", "text/plain", "12345".getBytes(StandardCharsets.UTF_8), "maxTextChars", 4);
		assertLimit("notes.txt", "text/plain", "a\rb\rc".getBytes(StandardCharsets.UTF_8), "maxTextLines", 2);
		assertLimit("manual.pdf", "application/pdf", pdfFixture(2), "maxPdfPages", 1);
		assertLimit("manual.pdf", "application/pdf", pdfFixture(2), "maxTextChars", 5);
		assertLimit("manual.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				docxFixture(2), "maxDocxParagraphs", 1);
		assertLimit("people.csv", "text/csv", "a\nb".getBytes(StandardCharsets.UTF_8), "maxTabularRows", 1);
		assertLimit("people.csv", "text/csv", "long".getBytes(StandardCharsets.UTF_8), "maxFieldChars", 3);
		assertLimit("people.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				xlsxFixture(1, 2), "maxTabularCells", 1);
	}

	@Test
	public void shouldPreflightOfficeZipExpansionBeforePoiLoadsDocument() throws Exception {
		assertLimit("bomb.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				zipFixture(2, 1), "maxArchiveEntries", 1);
		assertLimit("bomb.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				zipFixture(1, 5), "maxArchiveEntryBytes", 4);
		assertLimit("bomb.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				zipFixture(2, 3), "maxArchiveInflatedBytes", 5);
	}

	@Test
	public void shouldParseMultilineCsvAndRejectMalformedQuotes() throws Exception {
		TinyflowParsedDocument parsed = parse("notes.csv", "text/csv",
				("name,note\r\nAlice,\"first line\nsecond \"\"quoted\"\" line\"")
						.getBytes(StandardCharsets.UTF_8));

		assertEquals(2, parsed.getSections().size());
		assertTrue(parsed.getSections().get(1).getContent().contains("first line\nsecond \"quoted\" line"));
		assertEquals(2, parsed.getSections().get(1).getMetadata().get("row"));
		assertThrows(IOException.class, () -> parse("bad.csv", "text/csv",
				"a,\"unclosed".getBytes(StandardCharsets.UTF_8)));
		assertThrows(IOException.class, () -> parse("bad.csv", "text/csv",
				"a,b\"illegal".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void shouldPreserveHtmlNestedBlockListAndTableSeparators() throws Exception {
		String html = "<div>Intro<ul><li>One</li><li><span>Two</span></li></ul>"
				+ "<table><tr><th>Name</th><th>City</th></tr><tr><td>Alice</td><td>Shanghai</td></tr></table></div>";

		TinyflowParsedDocument parsed = parse("nested.html", "text/html", html.getBytes(StandardCharsets.UTF_8));

		assertTrue(parsed.getContent().contains("Intro\n"));
		assertTrue(parsed.getContent().contains("One\nTwo\n"));
		assertTrue(parsed.getContent().contains("Name\tCity"));
		assertTrue(parsed.getContent().contains("Alice\tShanghai"));
	}

	private void assertTextFixture(String name, String mediaType, String value,
			String expectedContent, int expectedLine) throws Exception {
		TinyflowParsedDocument parsed = parse(name, mediaType, value.getBytes(StandardCharsets.UTF_8));
		assertTrue(parsed.getContent().contains(expectedContent));
		assertEquals("text-markdown-html", parsed.getMetadata().get("parserKey"));
		assertEquals("1", parsed.getMetadata().get("parserVersion"));
		assertEquals(expectedLine, parsed.getSections().get(0).getMetadata().get("line"));
	}

	private TinyflowParsedDocument parse(String name, String mediaType, byte[] fixture) throws Exception {
		return parser.parse(new ByteArrayInputStream(fixture), context(name, mediaType));
	}

	private TinyflowDocumentParser.ParseContext context(String name, String mediaType) {
		return context(name, mediaType, Collections.<String, Object>emptyMap());
	}

	private TinyflowDocumentParser.ParseContext context(String name, String mediaType, Map<String, Object> options) {
		return new TinyflowDocumentParser.ParseContext("tenant-a", "kb-1", "source-1", "file",
				"doc-1", mediaType, name, options);
	}

	private void assertLimit(String name, String mediaType, byte[] fixture, String key, int limit) {
		Map<String, Object> options = new LinkedHashMap<String, Object>();
		options.put(key, limit);
		IOException exception = assertThrows(IOException.class, () -> parser.parse(
				new ByteArrayInputStream(fixture), context(name, mediaType, options)));
		assertTrue(exception.getMessage().toLowerCase().contains("limit"));
	}

	private byte[] pdfFixture() throws IOException {
		return pdfFixture(1);
	}

	private byte[] pdfFixture(int pages) throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			for (int index = 0; index < pages; index++) {
				PDPage page = new PDPage();
				document.addPage(page);
				try (PDPageContentStream content = new PDPageContentStream(document, page)) {
					content.beginText();
					content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
					content.newLineAtOffset(72, 700);
					content.showText(index == 0 ? "PDF page one" : "PDF page " + (index + 1));
					content.endText();
				}
			}
			document.save(output);
			return output.toByteArray();
		}
	}

	private byte[] docxFixture() throws IOException {
		return docxFixture(1);
	}

	private byte[] docxFixture(int paragraphs) throws IOException {
		try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			for (int index = 0; index < paragraphs; index++) {
				document.createParagraph().createRun().setText("DOCX paragraph " + (index + 1));
			}
			document.write(output);
			return output.toByteArray();
		}
	}

	private byte[] xlsxFixture() throws IOException {
		return xlsxFixture(2, 2);
	}

	private byte[] xlsxFixture(int rows, int cells) throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("People");
			for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
				Row row = sheet.createRow(rowIndex);
				for (int cellIndex = 0; cellIndex < cells; cellIndex++) {
					String value = rowIndex == 1 && cellIndex == 0 ? "Alice"
							: rowIndex == 1 && cellIndex == 1 ? "Shanghai"
							: rowIndex == 0 && cellIndex == 0 ? "name" : "city";
					row.createCell(cellIndex).setCellValue(value);
				}
			}
			workbook.write(output);
			return output.toByteArray();
		}
	}

	private byte[] zipFixture(int entries, int bytesPerEntry) throws IOException {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(output)) {
			for (int index = 0; index < entries; index++) {
				zip.putNextEntry(new ZipEntry("entry-" + index));
				zip.write(new byte[bytesPerEntry]);
				zip.closeEntry();
			}
			zip.finish();
			return output.toByteArray();
		}
	}
}

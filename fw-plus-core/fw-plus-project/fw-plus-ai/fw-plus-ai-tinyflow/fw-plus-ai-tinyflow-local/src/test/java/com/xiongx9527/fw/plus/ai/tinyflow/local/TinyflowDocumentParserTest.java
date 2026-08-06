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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
		return new TinyflowDocumentParser.ParseContext("tenant-a", "kb-1", "source-1", "file",
				"doc-1", mediaType, name, Collections.<String, Object>emptyMap());
	}

	private byte[] pdfFixture() throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(72, 700);
				content.showText("PDF page one");
				content.endText();
			}
			document.save(output);
			return output.toByteArray();
		}
	}

	private byte[] docxFixture() throws IOException {
		try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			document.createParagraph().createRun().setText("DOCX paragraph");
			document.write(output);
			return output.toByteArray();
		}
	}

	private byte[] xlsxFixture() throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("People");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("name");
			header.createCell(1).setCellValue("city");
			Row data = sheet.createRow(1);
			data.createCell(0).setCellValue("Alice");
			data.createCell(1).setCellValue("Shanghai");
			workbook.write(output);
			return output.toByteArray();
		}
	}
}

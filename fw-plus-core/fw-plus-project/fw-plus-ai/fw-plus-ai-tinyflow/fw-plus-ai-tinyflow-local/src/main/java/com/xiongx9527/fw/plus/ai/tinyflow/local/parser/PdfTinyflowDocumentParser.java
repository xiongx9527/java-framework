package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PdfTinyflowDocumentParser implements TinyflowDocumentParser {

	@Override
	public boolean supports(String mediaType, String fileName) {
		String type = lower(mediaType);
		String name = lower(fileName);
		return type.equals("application/pdf") || name.endsWith(".pdf");
	}

	@Override
	public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		try (PDDocument document = Loader.loadPDF(TextMarkdownHtmlParser.readAll(input))) {
			PDFTextStripper stripper = new PDFTextStripper();
			for (int page = 1; page <= document.getNumberOfPages(); page++) {
				stripper.setStartPage(page);
				stripper.setEndPage(page);
				String pageText = stripper.getText(document).trim();
				if (!pageText.isEmpty()) {
					if (content.length() > 0) {
						content.append('\n');
					}
					content.append(pageText);
					Map<String, Object> metadata = new LinkedHashMap<String, Object>();
					metadata.put("page", page);
					sections.add(new TinyflowParsedDocument.Section(pageText, "page:" + page, metadata));
				}
			}
		}
		if (content.length() == 0) {
			throw new IOException("PDF contains no extractable text");
		}
		return new TinyflowParsedDocument(context.getFileName(), content.toString(), sections,
				TextMarkdownHtmlParser.parserMetadata("pdf"));
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}

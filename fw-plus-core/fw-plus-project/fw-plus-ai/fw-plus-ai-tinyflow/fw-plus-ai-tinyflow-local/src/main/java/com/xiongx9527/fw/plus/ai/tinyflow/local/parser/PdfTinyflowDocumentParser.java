package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
		TinyflowParserLimits limits = TinyflowParserLimits.from(context);
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		StringBuilder content = new StringBuilder();
		Path path = limits.copyToTempFile(input, ".pdf");
		try {
			try (PDDocument document = Loader.loadPDF(path.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
				limits.checkPdfPages(document.getNumberOfPages());
				PDFTextStripper stripper = new PDFTextStripper();
				for (int page = 1; page <= document.getNumberOfPages(); page++) {
					stripper.setStartPage(page);
					stripper.setEndPage(page);
					String pageText = stripper.getText(document).trim();
					if (!pageText.isEmpty()) {
						int separator = content.length() > 0 ? 1 : 0;
						limits.checkTextChars(content.length() + separator + pageText.length());
						if (separator > 0) {
							content.append('\n');
						}
						content.append(pageText);
						Map<String, Object> metadata = new LinkedHashMap<String, Object>();
						metadata.put("page", page);
						sections.add(new TinyflowParsedDocument.Section(pageText, "page:" + page, metadata));
					}
				}
			}
		} finally {
			Files.deleteIfExists(path);
		}
		if (content.length() == 0) {
			throw new IOException("PDF contains no extractable text");
		}
		limits.checkText(content.toString());
		return new TinyflowParsedDocument(context.getFileName(), content.toString(), sections,
				TextMarkdownHtmlParser.parserMetadata("pdf"));
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}

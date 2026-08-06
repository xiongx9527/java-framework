package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CompositeTinyflowDocumentParser implements TinyflowDocumentParser {

	private final List<TinyflowDocumentParser> delegates;

	public CompositeTinyflowDocumentParser(List<TinyflowDocumentParser> delegates) {
		if (delegates == null || delegates.isEmpty()) {
			throw new IllegalArgumentException("delegates must not be empty");
		}
		this.delegates = Collections.unmodifiableList(new ArrayList<TinyflowDocumentParser>(delegates));
	}

	public static CompositeTinyflowDocumentParser defaults() {
		return new CompositeTinyflowDocumentParser(Arrays.<TinyflowDocumentParser>asList(
				new PdfTinyflowDocumentParser(), new DocxTinyflowDocumentParser(),
				new TabularTinyflowDocumentParser(), new TextMarkdownHtmlParser()));
	}

	@Override
	public boolean supports(String mediaType, String fileName) {
		return select(mediaType, fileName) != null;
	}

	@Override
	public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		TinyflowDocumentParser parser = select(context.getMediaType(), context.getFileName());
		if (parser == null) {
			throw new IOException("Unsupported document format");
		}
		return parser.parse(input, context);
	}

	private TinyflowDocumentParser select(String mediaType, String fileName) {
		for (TinyflowDocumentParser delegate : delegates) {
			if (delegate != null && delegate.supports(mediaType, fileName)) {
				return delegate;
			}
		}
		return null;
	}
}

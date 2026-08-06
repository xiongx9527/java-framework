package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TextMarkdownHtmlParser implements TinyflowDocumentParser {

	private static final String KEY = "text-markdown-html";
	private static final String VERSION = "1";

	@Override
	public boolean supports(String mediaType, String fileName) {
		String type = lower(mediaType);
		String name = lower(fileName);
		return type.startsWith("text/plain") || type.contains("markdown") || type.contains("html")
				|| name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")
				|| name.endsWith(".html") || name.endsWith(".htm");
	}

	@Override
	public TinyflowParsedDocument parse(InputStream input, ParseContext context) throws IOException {
		String raw = new String(readAll(input), StandardCharsets.UTF_8);
		if (raw.startsWith("\uFEFF")) {
			raw = raw.substring(1);
		}
		String content = isHtml(context) ? htmlText(raw) : raw;
		if (content.trim().isEmpty()) {
			throw new IOException("Document content is empty");
		}
		List<TinyflowParsedDocument.Section> sections = new ArrayList<TinyflowParsedDocument.Section>();
		String[] lines = content.split("\\r?\\n", -1);
		for (int index = 0; index < lines.length; index++) {
			String line = lines[index].trim();
			if (!line.isEmpty()) {
				Map<String, Object> metadata = new LinkedHashMap<String, Object>();
				metadata.put("line", index + 1);
				sections.add(new TinyflowParsedDocument.Section(line, "line:" + (index + 1), metadata));
			}
		}
		return new TinyflowParsedDocument(context.getFileName(), content.trim(), sections, parserMetadata(KEY));
	}

	static Map<String, Object> parserMetadata(String key) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("parserKey", key);
		metadata.put("parserVersion", VERSION);
		return metadata;
	}

	static byte[] readAll(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = input.read(buffer)) != -1) {
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}

	private boolean isHtml(ParseContext context) {
		String type = lower(context.getMediaType());
		String name = lower(context.getFileName());
		return type.contains("html") || name.endsWith(".html") || name.endsWith(".htm");
	}

	private String htmlText(String html) throws IOException {
		final StringBuilder content = new StringBuilder();
		new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
			@Override
			public void handleText(char[] data, int position) {
				content.append(data);
			}

			@Override
			public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
				if (HTML.Tag.BR.equals(tag)) {
					content.append('\n');
				}
			}

			@Override
			public void handleEndTag(HTML.Tag tag, int position) {
				if (HTML.Tag.P.equals(tag) || HTML.Tag.DIV.equals(tag) || HTML.Tag.LI.equals(tag)
						|| HTML.Tag.H1.equals(tag) || HTML.Tag.H2.equals(tag) || HTML.Tag.H3.equals(tag)
						|| HTML.Tag.H4.equals(tag) || HTML.Tag.H5.equals(tag) || HTML.Tag.H6.equals(tag)) {
					content.append('\n');
				}
			}
		}, true);
		return content.toString();
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}

package com.xiongx9527.fw.plus.ai.tinyflow.local.parser;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDocumentParser;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class TinyflowParserLimits {

	private static final int DEFAULT_MAX_INPUT_BYTES = 20 * 1024 * 1024;
	private static final int DEFAULT_MAX_TEXT_CHARS = 5 * 1024 * 1024;
	private static final int DEFAULT_MAX_TEXT_LINES = 100000;
	private static final int DEFAULT_MAX_PDF_PAGES = 1000;
	private static final int DEFAULT_MAX_DOCX_PARAGRAPHS = 100000;
	private static final int DEFAULT_MAX_DOCX_TABLES = 1000;
	private static final int DEFAULT_MAX_DOCX_ROWS = 100000;
	private static final int DEFAULT_MAX_DOCX_CELLS = 1000000;
	private static final int DEFAULT_MAX_TABULAR_ROWS = 100000;
	private static final int DEFAULT_MAX_TABULAR_CELLS = 1000000;
	private static final int DEFAULT_MAX_FIELD_CHARS = 100000;
	private static final int DEFAULT_MAX_ARCHIVE_ENTRIES = 10000;
	private static final int DEFAULT_MAX_ARCHIVE_ENTRY_BYTES = 16 * 1024 * 1024;
	private static final int DEFAULT_MAX_ARCHIVE_INFLATED_BYTES = 64 * 1024 * 1024;

	private final int maxInputBytes;
	private final int maxTextChars;
	private final int maxTextLines;
	private final int maxPdfPages;
	private final int maxDocxParagraphs;
	private final int maxDocxTables;
	private final int maxDocxRows;
	private final int maxDocxCells;
	private final int maxTabularRows;
	private final int maxTabularCells;
	private final int maxFieldChars;
	private final int maxArchiveEntries;
	private final int maxArchiveEntryBytes;
	private final int maxArchiveInflatedBytes;

	private TinyflowParserLimits(Map<String, Object> options) throws IOException {
		maxInputBytes = limit(options, "maxInputBytes", DEFAULT_MAX_INPUT_BYTES);
		maxTextChars = limit(options, "maxTextChars", DEFAULT_MAX_TEXT_CHARS);
		maxTextLines = limit(options, "maxTextLines", DEFAULT_MAX_TEXT_LINES);
		maxPdfPages = limit(options, "maxPdfPages", DEFAULT_MAX_PDF_PAGES);
		maxDocxParagraphs = limit(options, "maxDocxParagraphs", DEFAULT_MAX_DOCX_PARAGRAPHS);
		maxDocxTables = limit(options, "maxDocxTables", DEFAULT_MAX_DOCX_TABLES);
		maxDocxRows = limit(options, "maxDocxRows", DEFAULT_MAX_DOCX_ROWS);
		maxDocxCells = limit(options, "maxDocxCells", DEFAULT_MAX_DOCX_CELLS);
		maxTabularRows = limit(options, "maxTabularRows", DEFAULT_MAX_TABULAR_ROWS);
		maxTabularCells = limit(options, "maxTabularCells", DEFAULT_MAX_TABULAR_CELLS);
		maxFieldChars = limit(options, "maxFieldChars", DEFAULT_MAX_FIELD_CHARS);
		maxArchiveEntries = limit(options, "maxArchiveEntries", DEFAULT_MAX_ARCHIVE_ENTRIES);
		maxArchiveEntryBytes = limit(options, "maxArchiveEntryBytes", DEFAULT_MAX_ARCHIVE_ENTRY_BYTES);
		maxArchiveInflatedBytes = limit(options, "maxArchiveInflatedBytes", DEFAULT_MAX_ARCHIVE_INFLATED_BYTES);
	}

	static TinyflowParserLimits from(TinyflowDocumentParser.ParseContext context) throws IOException {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		return new TinyflowParserLimits(context.getOptions());
	}

	byte[] read(InputStream input) throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxInputBytes, 8192));
		byte[] buffer = new byte[4096];
		int total = 0;
		int read;
		while ((read = input.read(buffer)) != -1) {
			total += read;
			check(total, maxInputBytes, "input byte");
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}

	Path copyToTempFile(InputStream input, String suffix) throws IOException {
		if (input == null) {
			throw new IllegalArgumentException("input must not be null");
		}
		Path path = Files.createTempFile("tinyflow-parser-", suffix);
		try (OutputStream output = Files.newOutputStream(path)) {
			byte[] buffer = new byte[4096];
			int total = 0;
			int read;
			while ((read = input.read(buffer)) != -1) {
				total += read;
				check(total, maxInputBytes, "input byte");
				output.write(buffer, 0, read);
			}
			return path;
		} catch (IOException exception) {
			Files.deleteIfExists(path);
			throw exception;
		}
	}

	byte[] readArchive(InputStream input) throws IOException {
		byte[] bytes = read(input);
		int entries = 0;
		long totalInflated = 0L;
		byte[] buffer = new byte[4096];
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				entries++;
				check(entries, maxArchiveEntries, "archive entry count");
				long entryInflated = 0L;
				int read;
				while ((read = zip.read(buffer)) != -1) {
					entryInflated += read;
					totalInflated += read;
					check(entryInflated, maxArchiveEntryBytes, "archive entry byte");
					check(totalInflated, maxArchiveInflatedBytes, "archive inflated byte");
				}
				zip.closeEntry();
			}
		}
		return bytes;
	}

	void checkText(String value) throws IOException {
		check(value.length(), maxTextChars, "text character");
		int lines = value.isEmpty() ? 0 : 1;
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (current == '\r' || current == '\n' && (index == 0 || value.charAt(index - 1) != '\r')) {
				lines++;
				check(lines, maxTextLines, "text line");
			}
		}
	}

	void checkField(int length) throws IOException { check(length, maxFieldChars, "field character"); }
	void checkTextChars(int count) throws IOException { check(count, maxTextChars, "text character"); }
	void checkPdfPages(int count) throws IOException { check(count, maxPdfPages, "PDF page"); }
	void checkDocxParagraphs(int count) throws IOException { check(count, maxDocxParagraphs, "DOCX paragraph"); }
	void checkDocxTables(int count) throws IOException { check(count, maxDocxTables, "DOCX table"); }
	void checkDocxRows(int count) throws IOException { check(count, maxDocxRows, "DOCX row"); }
	void checkDocxCells(int count) throws IOException { check(count, maxDocxCells, "DOCX cell"); }
	void checkTabularRows(int count) throws IOException { check(count, maxTabularRows, "tabular row"); }
	void checkTabularCells(int count) throws IOException { check(count, maxTabularCells, "tabular cell"); }

	private static int limit(Map<String, Object> options, String key, int hardLimit) throws IOException {
		Object configured = options == null ? null : options.get(key);
		if (configured == null) {
			return hardLimit;
		}
		if (!(configured instanceof Number)) {
			throw new IOException(key + " limit must be numeric");
		}
		long value = ((Number) configured).longValue();
		if (value <= 0L) {
			throw new IOException(key + " limit must be positive");
		}
		return (int) Math.min(value, (long) hardLimit);
	}

	private static void check(int actual, int maximum, String subject) throws IOException {
		if (actual > maximum) {
			throw new IOException(subject + " limit exceeded");
		}
	}

	private static void check(long actual, long maximum, String subject) throws IOException {
		if (actual > maximum) {
			throw new IOException(subject + " limit exceeded");
		}
	}
}

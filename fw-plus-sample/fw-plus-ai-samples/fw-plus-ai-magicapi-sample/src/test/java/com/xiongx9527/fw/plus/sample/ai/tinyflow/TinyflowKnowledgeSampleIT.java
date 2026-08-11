package com.xiongx9527.fw.plus.sample.ai.tinyflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.FwPlusAISampleApplication;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeOptionProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowObjectStorage;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorDocument;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStore;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.vector.TinyflowVectorStoreProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = FwPlusAISampleApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TinyflowKnowledgeSampleIT.FixtureConfiguration.class)
public class TinyflowKnowledgeSampleIT {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String TOKEN = "sample-editor-token";
	private static final String KNOWLEDGE_ID = "sample-knowledge";

	@Autowired private TinyflowKnowledgeProviderRegistry providers;
	@Autowired private TestRestTemplate rest;

	@Test
	public void shouldImportSevenSourcesAndRetrieveThroughDifyExternalHttpContract() throws Exception {
		assertNotNull(providers.get("fw-plus-local"));
		assertNotNull(providers.get("dify"));
		assertFalse(providers.get("dify").capabilities().supportsManagement());

		Map<String, Object> create = new LinkedHashMap<String, Object>();
		create.put("knowledgeId", KNOWLEDGE_ID);
		create.put("name", "Sample knowledge");
		create.put("description", "End-to-end sample fixture");
		create.put("enabled", Boolean.TRUE);
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("embeddingModelKey", "embedding-main");
		metadata.put("vectorStoreKey", "vector-main");
		metadata.put("s3Key", "s3-main");
		create.put("metadata", metadata);
		post("/magic-api/tinyflow/knowledge/create", create);

		List<FileFixture> fixtures = Arrays.asList(
				new FileFixture("source-txt", "source.txt", "text/plain", textBytes("TXT shared retrieval source")),
				new FileFixture("source-pdf", "source.pdf", "application/pdf", pdfBytes()),
				new FileFixture("source-docx", "source.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes()),
				new FileFixture("source-html", "source.html", "text/html", textBytes("<html><body><h1>HTML</h1><p>shared retrieval source</p></body></html>")),
				new FileFixture("source-csv", "source.csv", "text/csv", textBytes("type,value\ncsv,shared retrieval source\n")),
				new FileFixture("source-xlsx", "source.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes()));
		List<String> documents = new ArrayList<String>();
		for (FileFixture fixture : fixtures) {
			Map<String, Object> body = importBody(fixture.sourceId, fixture.fileName);
			body.put("filename", fixture.fileName);
			body.put("mediaType", fixture.mediaType);
			body.put("content", fixture.content);
			JsonNode task = post("/magic-api/tinyflow/knowledge/import/file", body);
			JsonNode completed = waitForTask(task.path("taskId").asText());
			documents.add(completed.path("documentId").asText());
		}
		Map<String, Object> text = importBody("source-text", "Inline text");
		text.put("content", "Inline shared retrieval source");
		JsonNode textTask = post("/magic-api/tinyflow/knowledge/import/text", text);
		documents.add(waitForTask(textTask.path("taskId").asText()).path("documentId").asText());
		assertEquals(7, documents.size());

		JsonNode chunks = get("/magic-api/tinyflow/knowledge/chunk/list?knowledgeId=" + KNOWLEDGE_ID
				+ "&documentId=" + documents.get(0) + "&page=1&size=20").path("items");
		assertTrue(chunks.size() > 0);
		JsonNode firstChunk = chunks.get(0);
		assertEquals("source-txt", firstChunk.path("sourceId").asText());
		assertEquals("file", firstChunk.path("sourceType").asText());
		assertEquals(documents.get(0), firstChunk.path("documentId").asText());
		assertFalse(firstChunk.path("chunkId").asText().isEmpty());

		Map<String, Object> keyRequest = Collections.<String, Object>singletonMap("knowledgeId", KNOWLEDGE_ID);
		String accessKey = post("/magic-api/tinyflow/knowledge/key/create", keyRequest).path("token").asText();
		Map<String, Object> retrieval = new LinkedHashMap<String, Object>();
		retrieval.put("knowledge_id", KNOWLEDGE_ID);
		retrieval.put("query", "shared retrieval source");
		Map<String, Object> setting = new LinkedHashMap<String, Object>();
		setting.put("top_k", Integer.valueOf(20));
		setting.put("score_threshold", Double.valueOf(0D));
		retrieval.put("retrieval_setting", setting);
		HttpHeaders externalHeaders = new HttpHeaders();
		externalHeaders.setContentType(MediaType.APPLICATION_JSON);
		externalHeaders.setBearerAuth(accessKey);
		ResponseEntity<String> external = rest.exchange("/external-knowledge/retrieval", HttpMethod.POST,
				new HttpEntity<Object>(retrieval, externalHeaders), String.class);
		assertEquals(HttpStatus.OK, external.getStatusCode());
		JsonNode record = JSON.readTree(external.getBody()).path("records").get(0);
		assertNotNull(record);
		assertEquals("file", record.path("metadata").path("source_type").asText());
		assertFalse(record.path("metadata").path("source_id").asText().isEmpty());
		assertFalse(record.path("metadata").path("document_id").asText().isEmpty());
		assertFalse(record.path("metadata").path("chunk_id").asText().isEmpty());
	}

	private JsonNode waitForTask(String taskId) throws Exception {
		for (int attempt = 0; attempt < 100; attempt++) {
			JsonNode items = get("/magic-api/tinyflow/knowledge/task/list?knowledgeId=" + KNOWLEDGE_ID + "&page=1&size=50").path("items");
			for (JsonNode item : items) {
				if (!taskId.equals(item.path("taskId").asText())) continue;
				String status = item.path("status").asText();
				if ("COMPLETED".equals(status)) return item;
				if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
					throw new AssertionError("Task " + taskId + " ended as " + status + ": " + item.path("message").asText());
				}
			}
			Thread.sleep(50L);
		}
		throw new AssertionError("Task did not complete: " + taskId);
	}

	private Map<String, Object> importBody(String sourceId, String name) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("knowledgeId", KNOWLEDGE_ID);
		body.put("sourceId", sourceId);
		body.put("name", name);
		Map<String, Object> options = new LinkedHashMap<String, Object>();
		options.put("chunkStrategy", "normal");
		options.put("chunkSize", Integer.valueOf(128));
		options.put("overlap", Integer.valueOf(16));
		body.put("options", options);
		return body;
	}

	private JsonNode get(String path) throws Exception {
		return exchange(path, HttpMethod.GET, null);
	}

	private JsonNode post(String path, Object body) throws Exception {
		return exchange(path, HttpMethod.POST, body);
	}

	private JsonNode exchange(String path, HttpMethod method, Object body) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(Constants.MAGIC_TOKEN_HEADER, TOKEN);
		ResponseEntity<String> response = rest.exchange(path, method, new HttpEntity<Object>(body, headers), String.class);
		assertEquals(HttpStatus.OK, response.getStatusCode(), path + " response body=" + response.getBody());
		JsonNode envelope = JSON.readTree(response.getBody());
		assertTrue(envelope.path("code").asInt() == 1 || envelope.path("code").asInt() == 200,
				path + " response body=" + response.getBody());
		return envelope.path("data");
	}

	private static byte[] textBytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

	private static byte[] pdfBytes() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PDDocument document = new PDDocument();
		PDPage page = new PDPage();
		document.addPage(page);
		PDPageContentStream content = new PDPageContentStream(document, page);
		content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12F);
		content.newLineAtOffset(40F, 700F); content.showText("PDF shared retrieval source"); content.endText(); content.close();
		document.save(output); document.close();
		return output.toByteArray();
	}

	private static byte[] docxBytes() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		XWPFDocument document = new XWPFDocument();
		document.createParagraph().createRun().setText("DOCX shared retrieval source");
		document.write(output); document.close();
		return output.toByteArray();
	}

	private static byte[] xlsxBytes() throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		XSSFWorkbook workbook = new XSSFWorkbook();
		workbook.createSheet("sources").createRow(0).createCell(0).setCellValue("XLSX shared retrieval source");
		workbook.write(output); workbook.close();
		return output.toByteArray();
	}

	private static final class FileFixture {
		private final String sourceId; private final String fileName; private final String mediaType; private final byte[] content;
		private FileFixture(String sourceId, String fileName, String mediaType, byte[] content) {
			this.sourceId = sourceId; this.fileName = fileName; this.mediaType = mediaType; this.content = content;
		}
	}

	@TestConfiguration
	public static class FixtureConfiguration {
		@Bean @Primary
		public AuthorizationInterceptor sampleAuthorization() {
			return new AuthorizationInterceptor() {
				@Override public boolean requireLogin() { return true; }
				@Override public MagicUser getUserByToken(String token) { return TOKEN.equals(token) ? new MagicUser("sample-tenant", "sample", token) : null; }
				@Override public boolean allowVisit(MagicUser user, MagicHttpServletRequest request, Authorization authorization) { return user != null; }
			};
		}

		@Bean @Primary
		public TinyflowModelProvider sampleTinyflowModelProvider() {
			return new TinyflowModelProvider() {
				@Override public List<TinyflowModelDescriptor> listChatModels() { return Collections.emptyList(); }
				@Override public List<TinyflowModelDescriptor> listEmbeddingModels() { return Collections.singletonList(new TinyflowModelDescriptor("embedding-main", "Sample embedding", "EMBEDDING", "sample")); }
				@Override public ChatLanguageModel getChatModel(String key) { return null; }
				@Override public TinyflowEmbeddingModel getEmbeddingModel(String key) {
					return "embedding-main".equals(key) ? text -> new float[] { 1F, (text == null ? 0 : text.length() % 17) / 17F, .5F } : null;
				}
			};
		}

		@Bean public InMemoryVectorStore sampleVectorStore() { return new InMemoryVectorStore(); }

		@Bean @Primary
		public TinyflowVectorStoreProvider sampleTinyflowVectorStoreProvider(InMemoryVectorStore store) {
			return new TinyflowVectorStoreProvider() {
				@Override public List<TinyflowVectorStoreDescriptor> list() { return Collections.singletonList(new TinyflowVectorStoreDescriptor("vector-main", "Sample vectors", "MEMORY", "sample", true)); }
				@Override public TinyflowVectorStore get(String key) { return "vector-main".equals(key) ? store : null; }
			};
		}

		@Bean @Primary public TinyflowObjectStorage sampleObjectStorage() { return new InMemoryObjectStorage(); }

		@Bean @Primary
		public MagicTinyflowKnowledgeOptionProvider sampleKnowledgeOptions() {
			return new MagicTinyflowKnowledgeOptionProvider() {
				@Override public List<Option> listModels(int limit) { return Collections.singletonList(new Option("embedding-main", "Sample embedding", "EMBEDDING", "sample")); }
				@Override public List<Option> listVectors(int limit) { return Collections.singletonList(new Option("vector-main", "Sample vectors", "VECTOR", "sample")); }
				@Override public List<Option> listS3(int limit) { return Collections.singletonList(new Option("s3-main", "Sample object storage", "OBJECT_STORAGE", "sample")); }
				@Override public boolean hasEmbeddingModel(String key) { return "embedding-main".equals(key); }
				@Override public boolean hasVectorStore(String key) { return "vector-main".equals(key); }
				@Override public boolean hasS3Storage(String key) { return "s3-main".equals(key); }
			};
		}

		@Bean public TinyflowCredentialResolver sampleCredentialResolver() {
			return (tenantId, credentialRef) -> new TinyflowCredentialResolver.ResolvedCredential("fixture-value".toCharArray(), Long.MAX_VALUE);
		}

		@Bean public TinyflowKnowledgeProvider sampleDifyProvider() {
			return new TinyflowKnowledgeProvider() {
				@Override public String getKey() { return "dify"; }
				@Override public TinyflowKnowledgeCapabilities capabilities() { return TinyflowKnowledgeCapabilities.retrievalOnly(); }
				@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) { return TinyflowKnowledgeResult.empty(); }
			};
		}
	}

	static final class InMemoryVectorStore implements TinyflowVectorStore {
		private final Map<String, TinyflowVectorDocument> documents = new LinkedHashMap<String, TinyflowVectorDocument>();
		@Override public synchronized List<String> addAll(List<TinyflowVectorDocument> values) {
			List<String> ids = new ArrayList<String>();
			for (TinyflowVectorDocument value : values) { documents.put(value.getId(), value); ids.add(value.getId()); }
			return ids;
		}
		@Override public synchronized void removeAll(List<String> ids) { for (String id : ids) documents.remove(id); }
		@Override public synchronized List<TinyflowVectorMatch> search(float[] vector, int maxResults, double minScore) {
			List<TinyflowVectorMatch> matches = new ArrayList<TinyflowVectorMatch>();
			for (TinyflowVectorDocument document : documents.values()) {
				if (matches.size() >= maxResults) break;
				if (.99D >= minScore) matches.add(new TinyflowVectorMatch(document.getId(), document.getContent(), .99D, document.getMetadata()));
			}
			return matches;
		}
	}

	static final class InMemoryObjectStorage implements TinyflowObjectStorage {
		private final Map<String, byte[]> objects = new LinkedHashMap<String, byte[]>();
		@Override public synchronized StoredObject put(String tenantId, String objectKey, InputStream input, String mediaType, Map<String, Object> metadata) throws IOException {
			byte[] bytes = read(input); objects.put(tenantId + "/" + objectKey, bytes);
			return new StoredObject(tenantId, objectKey, mediaType, bytes.length, digest(bytes), metadata);
		}
		@Override public synchronized InputStream get(String tenantId, String objectKey) throws IOException {
			byte[] bytes = objects.get(tenantId + "/" + objectKey); if (bytes == null) throw new IOException("Object not found"); return new ByteArrayInputStream(bytes);
		}
		@Override public synchronized boolean exists(String tenantId, String objectKey) { return objects.containsKey(tenantId + "/" + objectKey); }
		@Override public synchronized void delete(String tenantId, String objectKey) { objects.remove(tenantId + "/" + objectKey); }
		private static byte[] read(InputStream input) throws IOException { ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count; while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count); return output.toByteArray(); }
		private static String digest(byte[] value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value); StringBuilder result = new StringBuilder(); for (byte item : bytes) result.append(String.format("%02x", item & 0xff)); return result.toString(); } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); } }
	}
}

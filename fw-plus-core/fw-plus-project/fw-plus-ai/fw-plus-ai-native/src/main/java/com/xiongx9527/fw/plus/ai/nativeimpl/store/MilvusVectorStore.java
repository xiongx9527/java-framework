package com.xiongx9527.fw.plus.ai.nativeimpl.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实原子能力：Milvus 向量存储后端（REST v1 API）。实现 base 的 VectorStorePort。
 * 覆盖 ensureCollection / upsert / search / delete / dropCollection / health；
 * 元数据过滤与候选 id 收敛在客户端完成（base 的 {@link Filter} 不暴露内部结构，无法翻译为 Milvus expr）。
 */
public class MilvusVectorStore implements VectorStorePort {
    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String token;
    private final String defaultCollection;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    public MilvusVectorStore(String baseUrl) { this(baseUrl, null, "default", 5000, 10000); }
    public MilvusVectorStore(String baseUrl, String token, String defaultCollection) { this(baseUrl, token, defaultCollection, 5000, 10000); }
    public MilvusVectorStore(String baseUrl, String token, String defaultCollection, int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new IllegalArgumentException("milvus baseUrl is required");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        this.defaultCollection = defaultCollection;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }
    @Override
    public String name() { return "milvus"; }
    @Override
    public StoreType type() { return StoreType.VECTOR; }
    @Override
    public boolean configured() { return baseUrl != null && !baseUrl.isEmpty(); }
    @Override
    public boolean health() {
        try { int code = request("GET", baseUrl + "/v1/vector/collections", null, null).code; return code == 200; }
        catch (IOException ex) { return false; }
    }
    @Override
    public String defaultCollection() { return defaultCollection; }
    @Override
    public void ensureCollection(String collection, int dimension) {
        Integer existing = collectionDimension(collection);
        if (existing != null) {
            if (existing != dimension) throw new VectorStorePort.DimensionMismatchException("collection " + collection + " expects dim " + existing + " but got " + dimension);
            return;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        body.put("dimension", dimension);
        body.put("metricType", "COSINE");
        body.put("primaryField", "id");
        body.put("vectorField", "vector");
        body.put("idType", "VarChar");
        body.put("autoID", false);
        body.put("enableDynamicField", true);
        try { request("POST", baseUrl + "/v1/vector/collections", body, null); }
        catch (IOException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("exist")) return;
            throw new IllegalStateException("milvus ensureCollection failed: " + ex.getMessage(), ex);
        }
    }
    @Override
    public Integer collectionDimension(String collection) {
        try {
            Response r = request("GET", baseUrl + "/v1/vector/collections/" + collection + "/details", null, null);
            if (r.code != 200) return null;
            JsonNode dims = mapper.readTree(r.body).path("data").path("fields");
            if (dims.isArray()) {
                for (JsonNode f : dims) {
                    if ("vector".equals(f.path("name").asText()) && f.path("dimension").isInt()) return f.get("dimension").asInt();
                }
            }
            return null;
        } catch (IOException ex) { return null; }
    }
    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) return;
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", id);
        item.put("vector", vector);
        if (metadata != null) item.putAll(metadata);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        body.put("data", Collections.singletonList(item));
        try { checkDim(collection, vector.length); request("POST", baseUrl + "/v1/vector/insert", body, null); }
        catch (IOException ex) { throw new IllegalStateException("milvus upsert failed: " + ex.getMessage(), ex); }
    }
    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) return;
        deleteByIds(collection, Collections.singletonList(id));
    }
    @Override
    public void deleteBatch(String collection, Collection<String> ids) {
        if (collection == null || ids == null) return;
        List<String> list = new ArrayList<String>();
        for (String id : ids) if (id != null) list.add(id);
        if (list.isEmpty()) return;
        deleteByIds(collection, list);
    }
    private void deleteByIds(String collection, List<String> ids) {
        StringBuilder expr = new StringBuilder("id in [");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) expr.append(",");
            expr.append("\"").append(ids.get(i)).append("\"");
        }
        expr.append("]");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        body.put("filter", expr.toString());
        try { request("POST", baseUrl + "/v1/vector/delete", body, null); }
        catch (IOException ex) { log.warn("milvus delete failed: {}", ex.getMessage()); }
    }
    @Override
    public void dropCollection(String collection) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", collection);
        try { request("POST", baseUrl + "/v1/vector/collections/drop", body, null); }
        catch (IOException ex) { log.warn("milvus dropCollection failed: {}", ex.getMessage()); }
    }
    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        if (request == null || request.getVector() == null) return VectorSearchResult.empty();
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;
        int fetch = (request.getFilter() == Filter.NO_OP && (request.getCandidateIds() == null || request.getCandidateIds().isEmpty())) ? topK : Math.max(topK * 10, 200);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collectionName", request.getCollection());
        body.put("data", Collections.singletonList(request.getVector()));
        body.put("limit", fetch);
        body.put("outputFields", Collections.singletonList("*"));
        try {
            Response r = request("POST", baseUrl + "/v1/vector/search", body, null);
            JsonNode arr = mapper.readTree(r.body).get("data");
            if (arr == null || !arr.isArray()) return VectorSearchResult.empty();
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            for (JsonNode n : arr) {
                double score = n.has("distance") ? n.get("distance").asDouble() : 0.0;
                String pk = n.has("id") ? n.get("id").asText() : null;
                Map<String, Object> meta = new LinkedHashMap<String, Object>();
                for (java.util.Iterator<Map.Entry<String, JsonNode>> it = n.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if ("id".equals(e.getKey()) || "distance".equals(e.getKey()) || "vector".equals(e.getKey())) continue;
                    meta.put(e.getKey(), nodeValue(e.getValue()));
                }
                Document doc = Document.builder().id(pk).content(metaText(meta)).metadata(meta).build();
                hits.add(new ScoredDocument(doc, score));
            }
            List<ScoredDocument> filtered = applyFilter(hits, request);
            if (filtered.size() > topK) filtered = filtered.subList(0, topK);
            return VectorSearchResult.of(filtered);
        } catch (IOException ex) {
            log.warn("milvus search failed: {}", ex.getMessage());
            return VectorSearchResult.empty();
        }
    }
    private List<ScoredDocument> applyFilter(List<ScoredDocument> hits, VectorSearchRequest request) {
        Filter filter = request.getFilter();
        Collection<String> candidates = request.getCandidateIds();
        List<ScoredDocument> out = new ArrayList<ScoredDocument>();
        for (ScoredDocument h : hits) {
            if (candidates != null && !candidates.isEmpty() && !candidates.contains(h.getDocument().getId())) continue;
            if (filter != Filter.NO_OP && !filter.matches(h.getDocument().getMetadata())) continue;
            out.add(h);
        }
        return out;
    }
    private void checkDim(String collection, int dim) {
        Integer existing = collectionDimension(collection);
        if (existing != null && existing != dim) throw new VectorStorePort.DimensionMismatchException("collection " + collection + " expects dim " + existing + " but got " + dim);
    }
    private static String metaText(Map<String, Object> meta) {
        Object c = meta == null ? null : meta.get("content");
        return c == null ? null : String.valueOf(c);
    }
    private static Object nodeValue(JsonNode n) {
        if (n == null) return null;
        if (n.isNumber()) return n.numberValue();
        if (n.isBoolean()) return n.booleanValue();
        return n.asText();
    }
    private Response request(String method, String url, Object body, Map<String, String> headers) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeoutMillis);
        conn.setReadTimeout(readTimeoutMillis);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (token != null && !token.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            conn.setDoOutput(true);
            byte[] data = mapper.writeValueAsBytes(body);
            try (OutputStream os = conn.getOutputStream()) { os.write(data); }
        }
        int code = conn.getResponseCode();
        String resp = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        if (code >= 400) throw new IOException("http " + code + ": " + resp);
        return new Response(code, resp);
    }
    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) sb.append((char) c);
        return sb.toString();
    }
    private static final class Response {
        final int code; final String body;
        Response(int code, String body) { this.code = code; this.body = body; }
    }
}

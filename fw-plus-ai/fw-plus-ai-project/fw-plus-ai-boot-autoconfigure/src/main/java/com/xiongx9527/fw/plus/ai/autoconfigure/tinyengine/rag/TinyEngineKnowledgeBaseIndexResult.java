package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TinyEngineKnowledgeBaseIndexResult {

	private final int loaded;
	private final int skipped;
	private final int failed;
	private final int chunks;
	private final List<String> failures;

	public TinyEngineKnowledgeBaseIndexResult(int loaded, int skipped, int failed, int chunks, List<String> failures) {
		this.loaded = loaded;
		this.skipped = skipped;
		this.failed = failed;
		this.chunks = chunks;
		this.failures = failures == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(failures));
	}

	public int getLoaded() { return loaded; }
	public int getSkipped() { return skipped; }
	public int getFailed() { return failed; }
	public int getChunks() { return chunks; }
	public List<String> getFailures() { return failures; }
}

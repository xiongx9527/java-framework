package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TinyEngineRagSearchResult {

	private final List<TinyEngineRagMatch> matches;

	public TinyEngineRagSearchResult(List<TinyEngineRagMatch> matches) {
		this.matches = matches == null ? Collections.<TinyEngineRagMatch>emptyList()
				: Collections.unmodifiableList(new ArrayList<TinyEngineRagMatch>(matches));
	}

	public List<TinyEngineRagMatch> getMatches() { return matches; }
}

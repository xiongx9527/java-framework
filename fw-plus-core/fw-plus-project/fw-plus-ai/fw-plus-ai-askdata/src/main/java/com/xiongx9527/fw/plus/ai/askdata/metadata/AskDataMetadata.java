package com.xiongx9527.fw.plus.ai.askdata.metadata;

import java.util.ArrayList; import java.util.Collections; import java.util.List;
/** 数据源元数据快照。 */
public final class AskDataMetadata { private final String datasourceKey; private final List<String> resources;
	public AskDataMetadata(String datasourceKey, List<String> resources) { if (datasourceKey == null || datasourceKey.trim().length() == 0) throw new IllegalArgumentException("datasourceKey 不能为空"); this.datasourceKey = datasourceKey.trim(); this.resources = Collections.unmodifiableList(new ArrayList<String>(resources == null ? Collections.<String>emptyList() : resources)); }
	public String getDatasourceKey() { return datasourceKey; } public List<String> getResources() { return resources; } }

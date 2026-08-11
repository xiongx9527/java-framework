package com.xiongx9527.fw.plus.ai.tinyflow.workflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.chain.Edge;
import dev.tinyflow.core.chain.Node;
import dev.tinyflow.core.node.EndNode;
import dev.tinyflow.core.node.StartNode;
import dev.tinyflow.core.parser.ChainParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class TinyflowWorkflowValidator {

	private final ChainParser chainParser;

	public TinyflowWorkflowValidator(ChainParser chainParser) {
		if (chainParser == null) {
			throw new IllegalArgumentException("chainParser must not be null");
		}
		this.chainParser = chainParser;
	}

	public ChainDefinition validate(String resourceId, String json) {
		JSONObject root = parseRoot(resourceId, json);
		JSONArray nodes = requireArray(root, "nodes", resourceId);
		requireArray(root, "edges", resourceId);
		if (nodes.isEmpty()) {
			throw invalid(resourceId, "nodes must not be empty");
		}
		validateNodeTypes(resourceId, nodes);

		ChainDefinition definition;
		try {
			definition = chainParser.parse(json);
		} catch (RuntimeException error) {
			throw new TinyflowException("Invalid Tinyflow workflow " + resource(resourceId)
					+ ": parser rejected the definition", error);
		}
		if (definition == null) {
			throw invalid(resourceId, "no path from startNode to endNode");
		}
		definition.setId(resourceId);
		validateStartToEndPath(resourceId, definition);
		return definition;
	}

	private JSONObject parseRoot(String resourceId, String json) {
		try {
			JSONObject root = JSON.parseObject(json);
			if (root == null) {
				throw invalid(resourceId, "JSON root must be an object");
			}
			return root;
		} catch (TinyflowException error) {
			throw error;
		} catch (RuntimeException error) {
			throw new TinyflowException("Invalid Tinyflow workflow " + resource(resourceId)
					+ ": malformed JSON", error);
		}
	}

	private JSONArray requireArray(JSONObject root, String field, String resourceId) {
		Object value = root.get(field);
		if (!(value instanceof JSONArray)) {
			throw invalid(resourceId, field + " must be an array");
		}
		return (JSONArray) value;
	}

	private void validateNodeTypes(String resourceId, JSONArray nodes) {
		for (int index = 0; index < nodes.size(); index++) {
			Object value = nodes.get(index);
			if (!(value instanceof JSONObject)) {
				throw invalid(resourceId, "node at index " + index + " must be an object");
			}
			JSONObject node = (JSONObject) value;
			String nodeId = node.getString("id");
			String type = node.getString("type");
			if (nodeId == null || nodeId.trim().length() == 0 || type == null || type.trim().length() == 0) {
				throw invalid(resourceId, "node at index " + index + " requires non-blank id and type");
			}
			if (!chainParser.getNodeParserMap().containsKey(type)) {
				throw invalid(resourceId, "unknown node id " + nodeId + " with type " + type);
			}
		}
	}

	private void validateStartToEndPath(String resourceId, ChainDefinition definition) {
		Set<String> nodeIds = new HashSet<String>();
		Set<String> endIds = new HashSet<String>();
		Queue<String> pending = new ArrayDeque<String>();
		for (Node node : safe(definition.getNodes())) {
			String nodeId = node.getId();
			if (!nodeIds.add(nodeId)) {
				throw invalid(resourceId, "duplicate node id " + nodeId);
			}
			if (node instanceof StartNode && node.getId() != null) {
				pending.add(nodeId);
			}
			if (node instanceof EndNode && node.getId() != null) {
				endIds.add(nodeId);
			}
		}

		Map<String, List<String>> targetsBySource = new HashMap<String, List<String>>();
		for (Edge edge : safe(definition.getEdges())) {
			if (!nodeIds.contains(edge.getSource())) {
				throw invalid(resourceId, "edge " + edge.getId() + " source node does not exist: "
						+ edge.getSource());
			}
			if (!nodeIds.contains(edge.getTarget())) {
				throw invalid(resourceId, "edge " + edge.getId() + " target node does not exist: "
						+ edge.getTarget());
			}
			List<String> targets = targetsBySource.get(edge.getSource());
			if (targets == null) {
				targets = new ArrayList<String>();
				targetsBySource.put(edge.getSource(), targets);
			}
			targets.add(edge.getTarget());
		}

		Set<String> visited = new HashSet<String>();
		while (!pending.isEmpty()) {
			String nodeId = pending.remove();
			if (!visited.add(nodeId)) {
				continue;
			}
			if (endIds.contains(nodeId)) {
				return;
			}
			List<String> targets = targetsBySource.get(nodeId);
			if (targets != null) {
				pending.addAll(targets);
			}
		}
		throw invalid(resourceId, "no path from startNode to endNode");
	}

	private <T> Collection<T> safe(Collection<T> values) {
		return values == null ? Collections.<T>emptyList() : values;
	}

	private TinyflowException invalid(String resourceId, String detail) {
		return new TinyflowException("Invalid Tinyflow workflow " + resource(resourceId) + ": " + detail);
	}

	private String resource(String resourceId) {
		return resourceId == null ? "<unknown>" : resourceId;
	}
}

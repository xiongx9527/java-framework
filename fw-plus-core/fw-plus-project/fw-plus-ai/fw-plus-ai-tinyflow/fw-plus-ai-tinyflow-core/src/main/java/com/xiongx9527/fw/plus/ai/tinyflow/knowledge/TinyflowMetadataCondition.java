package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

public final class TinyflowMetadataCondition {

	private final String field;
	private final String operator;
	private final Object value;

	private TinyflowMetadataCondition(Builder builder) {
		this.field = TinyflowKnowledgeReference.requireText(builder.field, "field");
		this.operator = TinyflowKnowledgeReference.requireText(builder.operator, "operator");
		this.value = builder.value;
	}

	public static Builder builder() { return new Builder(); }
	public String getField() { return field; }
	public String getOperator() { return operator; }
	public Object getValue() { return value; }

	public static final class Builder {
		private String field;
		private String operator;
		private Object value;
		public Builder field(String value) { this.field = value; return this; }
		public Builder operator(String value) { this.operator = value; return this; }
		public Builder value(Object value) { this.value = value; return this; }
		public TinyflowMetadataCondition build() { return new TinyflowMetadataCondition(this); }
	}
}

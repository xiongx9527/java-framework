package com.vesoft.nebula.jdbc.sql.trigger;

public class TriggerDdlResult {
	private final boolean handled;
	private final TriggerDefinition definition;
	private final TriggerDropResult dropResult;

	private TriggerDdlResult(boolean handled, TriggerDefinition definition, TriggerDropResult dropResult) {
		this.handled = handled;
		this.definition = definition;
		this.dropResult = dropResult;
	}

	public static TriggerDdlResult created(TriggerDefinition definition) {
		return new TriggerDdlResult(true, definition, null);
	}

	public static TriggerDdlResult dropped(TriggerDropResult dropResult) {
		return new TriggerDdlResult(true, null, dropResult);
	}

	public boolean isHandled() {
		return handled;
	}

	public TriggerDefinition getDefinition() {
		return definition;
	}

	public TriggerDropResult getDropResult() {
		return dropResult;
	}
}

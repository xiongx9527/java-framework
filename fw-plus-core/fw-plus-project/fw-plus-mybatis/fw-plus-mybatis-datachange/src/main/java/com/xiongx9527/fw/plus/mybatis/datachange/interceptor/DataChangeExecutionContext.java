package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DataChangeExecutionContext {
	private final MappedStatement mappedStatement;
	private final Object parameterObject;
	private final DataChangeFrameworkAdapter frameworkAdapter;
	private final List<StatementChangePlan> plans = new ArrayList<StatementChangePlan>();
	private final List<Integer> updateCounts = new ArrayList<Integer>();
	private List<StatementChangePlan> currentPlans = Collections.emptyList();
	private Connection connection;
	private ConsistencyLevel consistencyLevel;
	private boolean postProcessingFailed;

	public DataChangeExecutionContext(MappedStatement mappedStatement, Object parameterObject,
			DataChangeFrameworkAdapter frameworkAdapter) {
		this.mappedStatement = mappedStatement;
		this.parameterObject = parameterObject;
		this.frameworkAdapter = frameworkAdapter;
	}

	public MappedStatement getMappedStatement() { return mappedStatement; }
	public Object getParameterObject() { return parameterObject; }
	public DataChangeFrameworkAdapter getFrameworkAdapter() { return frameworkAdapter; }
	public Connection getConnection() { return connection; }
	public ConsistencyLevel getConsistencyLevel() { return consistencyLevel; }
	public boolean isPostProcessingFailed() { return postProcessingFailed; }
	public List<StatementChangePlan> getPlans() {
		return Collections.unmodifiableList(new ArrayList<StatementChangePlan>(plans));
	}
	public List<Integer> getUpdateCounts() {
		return Collections.unmodifiableList(new ArrayList<Integer>(updateCounts));
	}
	public List<StatementChangePlan> getCurrentPlans() { return currentPlans; }

	void initializeConnection(Connection actualConnection, ConsistencyLevel level) {
		if (connection == null) {
			connection = actualConnection;
			consistencyLevel = level;
		}
	}

	void registerPlans(List<StatementChangePlan> statementPlans) {
		currentPlans = Collections.unmodifiableList(new ArrayList<StatementChangePlan>(statementPlans));
		plans.addAll(statementPlans);
	}

	void addUpdateCount(int count) { updateCounts.add(Integer.valueOf(count)); }
	void markPostProcessingFailed() { postProcessingFailed = true; }
}

package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface AskDataMetadataProvider {

    AskDataMetadata load(AskDataRequest request);

    /**
     * 加载仅包含当前授权对象的元数据。
     *
     * <p>保留旧 {@link #load(AskDataRequest)} SPI 的实现兼容性；未覆盖此方法的提供方会在
     * 返回元数据后执行默认收窄。</p>
     */
    default AskDataMetadata load(AskDataRequest request, AskDataAuthorizationSummary authorizationSummary) {
        AskDataMetadata metadata = load(request);
        if (metadata == null || authorizationSummary == null || !authorizationSummary.isAllowed()) {
            return metadata == null ? null : new AskDataMetadata(metadata.getDialect(),
                    Collections.<AskDataTable>emptyList(), metadata.getTerms(), metadata.getExamples(),
                    metadata.getPromptFragments());
        }
        List<AskDataTable> tables = new ArrayList<AskDataTable>();
        for (AskDataTable table : metadata.getTables()) {
            if (table == null || !isAuthorizedTable(request, authorizationSummary, table)) {
                continue;
            }
            tables.add(new AskDataTable(table.getName(), authorizedColumns(request, authorizationSummary, table)));
        }
        return new AskDataMetadata(metadata.getDialect(), tables, metadata.getTerms(), metadata.getExamples(),
                metadata.getPromptFragments());
    }

    default boolean isAuthorizedTable(AskDataRequest request, AskDataAuthorizationSummary authorizationSummary,
            AskDataTable table) {
        for (AskDataAuthorizedObject authorizedObject : authorizationSummary.getAuthorizedObjects()) {
            if (authorizedObject != null && request != null
                    && equals(request.getDatasourceKey(), authorizedObject.getDatasourceKey())
                    && equals(table.getName(), authorizedObject.getTableName())) {
                return true;
            }
        }
        return false;
    }

    default List<AskDataColumn> authorizedColumns(AskDataRequest request,
            AskDataAuthorizationSummary authorizationSummary, AskDataTable table) {
        List<AskDataColumn> columns = new ArrayList<AskDataColumn>();
        for (AskDataColumn column : table.getColumns()) {
            if (isAuthorizedColumn(request, authorizationSummary, table, column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    default boolean isAuthorizedColumn(AskDataRequest request, AskDataAuthorizationSummary authorizationSummary,
            AskDataTable table, AskDataColumn column) {
        if (column == null) {
            return false;
        }
        for (AskDataAuthorizedObject authorizedObject : authorizationSummary.getAuthorizedObjects()) {
            if (authorizedObject != null && request != null
                    && equals(request.getDatasourceKey(), authorizedObject.getDatasourceKey())
                    && equals(table.getName(), authorizedObject.getTableName())
                    && ("*".equals(authorizedObject.getColumnName())
                            || equals(column.getName(), authorizedObject.getColumnName()))) {
                return true;
            }
        }
        return false;
    }

    default boolean equals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}

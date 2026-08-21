package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserNamespace;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserObjectDetail;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserObjectInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewResult;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserRoutineInfo;

import java.util.List;

public interface DbBrowserService {
	List<DbBrowserDataSourceInfo> listDataSources();
	List<DbBrowserNamespace> listNamespaces(String key);
	List<DbBrowserObjectInfo> listObjects(String key, String catalog, String schema, String type);
	List<DbBrowserRoutineInfo> listRoutines(String key, String catalog, String schema, String type);
	DbBrowserObjectDetail getObjectDetail(String key, String catalog, String schema, String objectName, String objectType);
	DbBrowserPreviewResult preview(DbBrowserPreviewRequest request);
}

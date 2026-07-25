package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserNamespace;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserObjectDetail;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserObjectInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewResult;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserRoutineInfo;

import java.util.List;

public interface DbBrowserService {
	List<DbBrowserDataSourceInfo> listDataSources();
	List<DbBrowserNamespace> listNamespaces(String key);
	List<DbBrowserObjectInfo> listObjects(String key, String catalog, String schema, String type);
	List<DbBrowserRoutineInfo> listRoutines(String key, String catalog, String schema, String type);
	DbBrowserObjectDetail getObjectDetail(String key, String catalog, String schema, String objectName, String objectType);
	DbBrowserPreviewResult preview(DbBrowserPreviewRequest request);
}

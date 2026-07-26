package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class VectorDataSourceInfo extends MagicEntity {

    private String name;

    private String key;

    private String provider;

    private boolean enabled = true;

    private Integer dimension;

    private List<String> metadataKeys = new ArrayList<String>();

    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    @Override
    public MagicEntity simple() {
        VectorDataSourceInfo dataSourceInfo = new VectorDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setName(this.name);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setProvider(this.provider);
        dataSourceInfo.setEnabled(this.enabled);
        dataSourceInfo.setDimension(this.dimension);
        dataSourceInfo.setMetadataKeys(new ArrayList<String>(this.metadataKeys));
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        VectorDataSourceInfo dataSourceInfo = new VectorDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setName(this.name);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setProvider(this.provider);
        dataSourceInfo.setEnabled(this.enabled);
        dataSourceInfo.setDimension(this.dimension);
        dataSourceInfo.setMetadataKeys(new ArrayList<String>(this.metadataKeys));
        dataSourceInfo.setProperties(new LinkedHashMap<String, Object>(this.properties));
        return dataSourceInfo;
    }
}

package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.ComponentLibraryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ComponentMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BundleDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BundleResultDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.Child;
import com.xiongx9527.fw.plus.tinyengine.model.dto.CustComponentDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.FileResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.Snippet;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Component;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ComponentLibrary;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialComponent;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialHistoryComponent;
import com.xiongx9527.fw.plus.tinyengine.service.ComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ComponentServiceImpl extends ServiceImpl<ComponentMapper, Component> implements ComponentService {
	@Autowired
	private ComponentLibraryMapper componentLibraryMapper;

	@Override
	public List<Component> findAllComponent() {
		return baseMapper.queryAllComponent();
	}

	@Override
	public Component findComponentById(Integer id) {
		return baseMapper.queryComponentById(id);
	}

	@Override
	public List<Component> findComponentByCondition(Component component) {
		return baseMapper.queryComponentByCondition(component);
	}

	@Override
	public Integer deleteComponentById(Integer id) {
		return baseMapper.deleteComponentById(id);
	}

	@Override
	public Integer updateComponentById(Component component) {
		return baseMapper.updateComponentById(component);
	}

	@Override
	public Integer createComponent(Component component) {
		return baseMapper.createComponent(component);
	}

	@Override
	public BundleResultDto parseBundle(BundleDto bundleDto) {
		if (bundleDto == null || bundleDto.getMaterials() == null
				|| bundleDto.getMaterials().getComponents() == null
				|| bundleDto.getMaterials().getComponents().isEmpty()) {
			throw failure(ExceptionEnum.CM009);
		}
		BundleResultDto result = new BundleResultDto();
		result.setComponentList(buildComponentList(bundleDto));
		List<Map<String, Object>> packages = bundleDto.getMaterials().getPackages();
		if (packages != null && !packages.isEmpty()) {
			List<ComponentLibrary> libraries = new ArrayList<ComponentLibrary>();
			for (Map<String, Object> source : packages) {
				ComponentLibrary library = JSON.parseObject(JSON.toJSONString(source), ComponentLibrary.class);
				library.setPackageName(String.valueOf(source.get("package")));
				library.setFramework("Vue");
				libraries.add(library);
			}
			result.setPackageList(libraries);
		}
		return result;
	}

	@Override
	public FileResult custComponentBatchCreate(CustComponentDto request) {
		if (request == null || request.getComponentLibraryId() == null
				|| request.getComponents() == null || request.getComponents().isEmpty()) {
			throw failure(ExceptionEnum.CM002);
		}
		int inserted = 0;
		for (Component component : request.getComponents()) {
			component.setLibraryId(request.getComponentLibraryId());
			inserted += createComponent(component);
		}
		return fileResult(inserted, 0);
	}

	@Override
	public FileResult bulkCreate(List<Component> componentList) {
		int inserted = 0;
		int updated = 0;
		if (componentList == null) {
			return fileResult(0, 0);
		}
		for (Component component : componentList) {
			Component condition = new Component();
			condition.setComponent(component.getComponent());
			condition.setName(component.getName());
			condition.setVersion(component.getVersion());
			List<Component> existing = findComponentByCondition(condition);
			assignLibrary(component);
			if (existing == null || existing.isEmpty()) {
				if (createComponent(component) == 1) {
					MaterialComponent material = new MaterialComponent();
					material.setMaterialId(1);
					material.setComponentId(component.getId());
					baseMapper.createMaterialComponent(material);
					MaterialHistoryComponent history = new MaterialHistoryComponent();
					history.setMaterialHistoryId(1);
					history.setComponentId(component.getId());
					baseMapper.createMaterialHistoryComponent(history);
					inserted++;
				}
			} else {
				component.setId(existing.get(0).getId());
				updated += updateComponentById(component);
			}
		}
		return fileResult(inserted, updated);
	}

	private void assignLibrary(Component component) {
		if (component.getNpm() == null || component.getNpm().get("package") == null) {
			return;
		}
		ComponentLibrary condition = new ComponentLibrary();
		condition.setPackageName(String.valueOf(component.getNpm().get("package")));
		condition.setVersion(component.getVersion());
		List<ComponentLibrary> libraries = componentLibraryMapper.queryComponentLibraryByCondition(condition);
		if (libraries != null && !libraries.isEmpty()) {
			component.setLibraryId(libraries.get(0).getId());
		}
	}

	private List<Component> buildComponentList(BundleDto bundleDto) {
		List<Component> result = new ArrayList<Component>();
		List<Child> snippets = bundleDto.getMaterials().getSnippets();
		for (Map<String, Object> source : bundleDto.getMaterials().getComponents()) {
			Component component = JSON.parseObject(JSON.toJSONString(source), Component.class);
			component.setId(null);
			component.setIsDefault(true);
			component.setIsOfficial(true);
			component.setDevMode("proCode");
			component.setFramework(bundleDto.getFramework());
			component.setPublicStatus(1);
			component.setIsTinyReserved(false);
			if (source.get("schema") instanceof Map) {
				component.setSchemaFragment((Map<String, Object>) source.get("schema"));
			}
			applySnippet(component, source, snippets);
			result.add(component);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private void applySnippet(Component component, Map<String, Object> source, List<Child> groups) {
		if (groups == null) {
			return;
		}
		for (Child group : groups) {
			if (group.getChildren() == null) {
				continue;
			}
			for (Snippet snippet : group.getChildren()) {
				if (pascal(String.valueOf(source.get("component"))).equals(pascal(snippet.getSnippetName()))) {
					Map<String, Object> value = JSON.parseObject(JSON.toJSONString(snippet), Map.class);
					component.setSnippets(Arrays.asList(value));
					if (component.getCategory() == null) {
						component.setCategory(group.getGroup());
					}
					return;
				}
			}
		}
	}

	private String pascal(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
	}

	private FileResult fileResult(int inserted, int updated) {
		FileResult result = new FileResult();
		result.setInsertNum(inserted);
		result.setUpdateNum(updated);
		return result;
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}

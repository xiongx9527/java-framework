package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.MagicDataSourceDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.datasource.service.DataSourceMagicDynamicRegistry;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MyDataSourceMagicDynamicRegistry extends DataSourceMagicDynamicRegistry {

    /**
     * 数据源构建器映射表，用于存储不同数据源名称对应的构建器实例
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String,DataSourceBuilder> dataSourceBuilderMap = new ConcurrentHashMap<>();

    /**
     * 默认数据源构建器，当没有找到特定数据源构建器时使用
     */
    private DefaultDataSourceBuilder defaultDataSourceBuilder = new DefaultDataSourceBuilder();

    /**
     * 魔法动态数据源实例，用于实现动态数据源切换功能
     */
    private final MagicDynamicDataSource magicDynamicDataSource;

	private final List<MagicDataSourceDecorator> dataSourceDecorators;

	private final Object lifecycleMonitor = new Object();

	private final Map<MagicDynamicDataSource.DataSourceNode, List<DecorationStage>> registeredDecorationStages =
			new IdentityHashMap<MagicDynamicDataSource.DataSourceNode, List<DecorationStage>>();

    /**
     * 当前类的类加载器实例
     */
    public static final ClassLoader CLASSLOADER = MyDataSourceMagicDynamicRegistry.class.getClassLoader();

    /**
     * 支持的数据源类型名称数组
     * 从DataSourceBuilder复制而来，包含常用的数据库连接池实现类
     * HikariCP、Druid、Tomcat JDBC Pool、Commons DBCP2
     */
    // copy from DataSourceBuilder
    public static final String[] DATA_SOURCE_TYPE_NAMES = new String[]{
            "com.zaxxer.hikari.HikariDataSource",
            "com.alibaba.druid.pool.DruidDataSource",
            "org.apache.tomcat.jdbc.pool.DataSource",
            "org.apache.commons.dbcp2.BasicDataSource"};


    /**
     * 构造方法，初始化MyDataSourceMagicDynamicRegistry实例。
     *
     * @param magicResourceStorage      资源存储对象，用于管理数据源信息资源
     * @param magicDynamicDataSource    动态数据源对象，用于实际的数据源注册与切换
     * @param dataSourceBuilderMap      数据源构建器映射表，根据类型获取对应的数据源构建方式
     */
    public MyDataSourceMagicDynamicRegistry(MagicResourceStorage<DataSourceInfo> magicResourceStorage, MagicDynamicDataSource magicDynamicDataSource
            ,Map<String,DataSourceBuilder> dataSourceBuilderMap) {
        this(magicResourceStorage, magicDynamicDataSource, dataSourceBuilderMap,
                Collections.<MagicDataSourceDecorator>emptyList());
    }

    public MyDataSourceMagicDynamicRegistry(MagicResourceStorage<DataSourceInfo> magicResourceStorage,
            MagicDynamicDataSource magicDynamicDataSource, Map<String,DataSourceBuilder> dataSourceBuilderMap,
            List<MagicDataSourceDecorator> dataSourceDecorators) {
        super(magicResourceStorage,magicDynamicDataSource);
        this.magicDynamicDataSource = magicDynamicDataSource;
        this.dataSourceDecorators = dataSourceDecorators == null
                ? Collections.<MagicDataSourceDecorator>emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<>(dataSourceDecorators));
        if (MapUtils.isNotEmpty(dataSourceBuilderMap)) {
            this.dataSourceBuilderMap.putAll(dataSourceBuilderMap);
        }
    }

    /**
     * 监听文件事件，当事件类型为'datasource'时处理该事件。
     * 处理过程中若发生异常将记录错误日志。
     *
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process Magic-API data source event", e);
        }
    }

    /**
     * 注册数据源信息
     * 重写父类的register方法，使用父类的注册逻辑来注册数据源信息
     *
     * @param entity 数据源信息实体对象，包含需要注册的数据源相关信息
     * @return boolean 返回注册结果，true表示注册成功，false表示注册失败
     */
    @Override
    public boolean register(DataSourceInfo entity) {
        try {
            return super.register(entity);
        } catch (Exception e) {
            log.error("Failed to register Magic-API data source: {}",
                    entity == null ? null : entity.getKey(), e);
            return false;
        }
    }


    /**
     * 注册指定的数据源节点到动态数据源中。
     * 根据数据源信息构造属性集，并使用对应的构建器创建数据源实例后进行注册。
     *
     * @param mappingNode 包含数据源信息的映射节点
     * @return 注册是否成功（始终返回true）
     */
    @Override
    protected boolean register(MappingNode<DataSourceInfo> mappingNode) {
        DataSourceInfo info = mappingNode.getEntity();
        Map<String, Object> properties = new HashMap<>(info.getProperties());
        String other = MagicAPIConfigUtils.getDbConfig(info.getKey(), "other", "");
        if (StringUtils.isNotBlank(other)) {
            JSONValidator jsonValidator = JSONValidator.from(other);
            if (jsonValidator.validate() && jsonValidator.getType() == JSONValidator.Type.Object) {
                JSONObject jsonObject = JSON.parseObject(other);
                if (jsonObject != null && !jsonObject.isEmpty()) {
                    properties = jsonObject;
                }
            }
        }
        String url = MagicAPIConfigUtils.getDbConfig(info.getKey(), "url", info.getUrl());
        String username = MagicAPIConfigUtils.getDbConfig(info.getKey(), "username", info.getUsername());
        String password = MagicAPIConfigUtils.getDbConfig(info.getKey(), "password", info.getPassword());
        String type = MagicAPIConfigUtils.getDbConfig(info.getKey(), "type", info.getType());
        String driverClassName = MagicAPIConfigUtils.getDbConfig(info.getKey(), "driverClassName", info.getDriverClassName());
        // 特殊处理Elasticsearch驱动，设置只读模式
        if(Constants.ES_DRIVER.equals(driverClassName)){
            properties.put("defaultReadOnly", true);
        }
        // 设置基本连接属性
        properties.put("name", info.getName());
        properties.put("url", url);
        properties.put("username", username);
        properties.put("password", password);
        // 若未提供驱动类名，则尝试从URL推断
        if (StringUtils.isBlank(driverClassName)) {
            String driverClass = DatabaseDriver.fromJdbcUrl(url).getDriverClassName();
            properties.put("driverClassName", driverClass);
        } else {
            properties.put("driverClassName", driverClassName);
        }
        // 获取并使用合适的构建器来创建数据源
        DataSourceBuilder dataSourceBuilder = null;
        if (StringUtils.isNotBlank(type)) {
            dataSourceBuilder = dataSourceBuilderMap.get(type);
        }
        if (Objects.isNull(dataSourceBuilder)) {
            dataSourceBuilder = defaultDataSourceBuilder;
        }
        DataSource rawDataSource = dataSourceBuilder.buildDataSource(type, properties);
        DataSource datasource = rawDataSource;
        List<DecorationStage> decorationStages = new ArrayList<>();
        try {
            for (MagicDataSourceDecorator decorator : dataSourceDecorators) {
                DataSource decorated = decorator.decorate(info.getKey(), datasource);
                if (decorated == null) {
                    throw new IllegalStateException("DataSource decorator returned null");
                }
                decorationStages.add(new DecorationStage(decorator, decorated));
                datasource = decorated;
            }
        } catch (RuntimeException e) {
            rollbackDecoration(info.getKey(), rawDataSource, decorationStages, e);
            throw e;
        } catch (Error e) {
            rollbackDecoration(info.getKey(), rawDataSource, decorationStages, e);
            throw e;
        }
		List<RemovalClaim> removalClaims = Collections.emptyList();
		RegistrationCleanupDisposition cleanupDisposition =
				RegistrationCleanupDisposition.REGISTRY_OWNED;
		try {
			synchronized (lifecycleMonitor) {
				List<MagicDynamicDataSource.DataSourceNode> replacedNodes =
						findDataSources(info.getId(), info.getKey());
				removalClaims = prepareRemove(replacedNodes);
				try {
					magicDynamicDataSource.put(info.getId(), info.getKey(), info.getName(),
							datasource, info.getMaxRows());
				} catch (RuntimeException e) {
					afterRemoveFailure(removalClaims, e);
					throw e;
				} catch (Error e) {
					afterRemoveFailure(removalClaims, e);
					throw e;
				}
				cleanupDisposition = RegistrationCleanupDisposition.DYNAMIC_DATA_SOURCE_OWNED;
				forgetDecorationStages(replacedNodes);
				MagicDynamicDataSource.DataSourceNode publishedNode =
						findPublishedNode(info.getId(), info.getKey(), datasource);
				registeredDecorationStages.put(publishedNode,
						Collections.unmodifiableList(new ArrayList<DecorationStage>(decorationStages)));
			}
		} catch (RuntimeException e) {
			if (cleanupDisposition == RegistrationCleanupDisposition.REGISTRY_OWNED) {
				rollbackDecoration(info.getKey(), rawDataSource, decorationStages, e);
			}
			throw e;
		} catch (Error e) {
			if (cleanupDisposition == RegistrationCleanupDisposition.REGISTRY_OWNED) {
				rollbackDecoration(info.getKey(), rawDataSource, decorationStages, e);
			}
			throw e;
		}
		return true;
    }

    /**
     * 取消注册指定的数据源节点。
     *
     * @param mappingNode 需要取消注册的数据源映射节点
	 */
    @Override
	protected void unregister(MappingNode<DataSourceInfo> mappingNode) {
		synchronized (lifecycleMonitor) {
			List<MagicDynamicDataSource.DataSourceNode> nodes =
					findDataSources(null, mappingNode.getMappingKey());
			List<RemovalClaim> removalClaims = prepareRemove(nodes);
			try {
				magicDynamicDataSource.delete(mappingNode.getMappingKey());
			} catch (RuntimeException e) {
				afterRemoveFailure(removalClaims, e);
				throw e;
			} catch (Error e) {
				afterRemoveFailure(removalClaims, e);
				throw e;
			}
			forgetDecorationStages(nodes);
		}
    }

    private List<MagicDynamicDataSource.DataSourceNode> findDataSources(String id, String key) {
        List<MagicDynamicDataSource.DataSourceNode> nodes = new ArrayList<>();
        for (MagicDynamicDataSource.DataSourceNode node : magicDynamicDataSource.datasourceNodes()) {
            if (Objects.equals(key, node.getKey()) || (id != null && Objects.equals(id, node.getId()))) {
                nodes.add(node);
            }
        }
        return nodes;
    }

	private MagicDynamicDataSource.DataSourceNode findPublishedNode(String id, String key,
			DataSource dataSource) {
		MagicDynamicDataSource.DataSourceNode publishedNode;
		try {
			publishedNode = magicDynamicDataSource.getDataSource(key);
		} catch (RuntimeException e) {
			throw new IllegalStateException(
					"Unable to identify the published DataSource node: " + key, e);
		}
		if (publishedNode.getDataSource() != dataSource
				|| !Objects.equals(key, publishedNode.getKey())
				|| !Objects.equals(id, publishedNode.getId())) {
			throw new IllegalStateException(
					"Unable to identify the published DataSource node: " + key);
		}
		return publishedNode;
	}

	private List<RemovalClaim> prepareRemove(List<MagicDynamicDataSource.DataSourceNode> nodes) {
		List<RemovalClaim> removalClaims = new ArrayList<RemovalClaim>();
		RuntimeException failure = null;
		try {
			for (MagicDynamicDataSource.DataSourceNode node : nodes) {
				for (DecorationStage stage : decorationStages(node)) {
					try {
						stage.decorator.startRemove(node.getKey(), stage.dataSource);
						removalClaims.add(new RemovalClaim(node.getKey(), stage));
					} catch (RuntimeException e) {
						failure = append(failure, e);
					}
				}
			}
			for (MagicDynamicDataSource.DataSourceNode node : nodes) {
				for (DecorationStage stage : decorationStages(node)) {
					try {
						stage.decorator.beforeRemove(node.getKey(), stage.dataSource);
					} catch (RuntimeException e) {
						failure = append(failure, e);
					}
				}
			}
		} catch (Error e) {
			afterRemoveFailure(removalClaims, e);
			throw e;
		}
		if (failure != null) {
			afterRemoveFailure(removalClaims, failure);
			throw failure;
		}
		return removalClaims;
	}

	private RuntimeException append(RuntimeException failure, RuntimeException next) {
		if (failure == null) {
			return next;
		}
		failure.addSuppressed(next);
		return failure;
	}

	private void afterRemoveFailure(List<RemovalClaim> removalClaims, Throwable failure) {
		for (int index = removalClaims.size() - 1; index >= 0; index--) {
			try {
				RemovalClaim claim = removalClaims.get(index);
				claim.stage.decorator.afterRemoveFailure(claim.key, claim.stage.dataSource);
			} catch (Throwable cleanupFailure) {
				addSuppressed(failure, cleanupFailure);
			}
		}
	}

	private List<DecorationStage> decorationStages(MagicDynamicDataSource.DataSourceNode node) {
		List<DecorationStage> stages = registeredDecorationStages.get(node);
		if (stages != null) {
			return stages;
		}
		List<DecorationStage> fallback = new ArrayList<DecorationStage>(dataSourceDecorators.size());
		for (MagicDataSourceDecorator decorator : dataSourceDecorators) {
			fallback.add(new DecorationStage(decorator, node.getDataSource()));
		}
		return fallback;
	}

	private void forgetDecorationStages(List<MagicDynamicDataSource.DataSourceNode> nodes) {
		for (MagicDynamicDataSource.DataSourceNode node : nodes) {
			registeredDecorationStages.remove(node);
		}
	}

    private void rollbackDecoration(String key, DataSource rawDataSource,
            List<DecorationStage> decorationStages, Throwable failure) {
        for (int i = decorationStages.size() - 1; i >= 0; i--) {
            DecorationStage stage = decorationStages.get(i);
            try {
                stage.decorator.cleanupAfterDecorationFailure(key, stage.dataSource);
            } catch (Throwable cleanupFailure) {
                addSuppressed(failure, cleanupFailure);
            }
        }
        closeRawDataSource(rawDataSource, failure);
    }

    private void closeRawDataSource(DataSource dataSource, Throwable failure) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Throwable cleanupFailure) {
                addSuppressed(failure, cleanupFailure);
            }
        }
    }

    private void addSuppressed(Throwable failure, Throwable cleanupFailure) {
        if (cleanupFailure != failure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private static final class DecorationStage {

        private final MagicDataSourceDecorator decorator;
        private final DataSource dataSource;

        private DecorationStage(MagicDataSourceDecorator decorator, DataSource dataSource) {
            this.decorator = decorator;
            this.dataSource = dataSource;
        }
    }

	private static final class RemovalClaim {

		private final String key;
		private final DecorationStage stage;

		private RemovalClaim(String key, DecorationStage stage) {
			this.key = key;
			this.stage = stage;
		}
	}

	private enum RegistrationCleanupDisposition {
		REGISTRY_OWNED,
		DYNAMIC_DATA_SOURCE_OWNED
	}

    /**
     * 获取默认的数据源映射列表。
     * 这些是系统内置或预定义但尚未分配ID的数据源节点。
     *
     * @return 默认数据源信息列表
     */
    @Override
    public List<DataSourceInfo> defaultMappings() {
        return magicDynamicDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            DataSourceInfo dataSourceInfo = new DataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }


}

package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.model.MybatisFlexXmlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service.MybatisFlexXmlMapperRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.model.MybatisPlusXmlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service.MybatisPlusXmlMapperRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.event.MagicEvent;
import org.ssssssss.magicapi.core.exception.InvalidArgumentException;
import org.ssssssss.magicapi.core.model.*;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.resource.ZipResource;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.DefaultMagicResourceService;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.utils.IoUtils;
import org.ssssssss.magicapi.utils.JsonUtils;
import org.ssssssss.magicapi.utils.WebUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class MyDefaultMagicResourceService extends DefaultMagicResourceService {

    // 添加常量定义
    private static final long WRITE_LOCK_TIMEOUT_SECONDS = 60L;

    /**
     * 存储锁对象的字段引用
     */
    private static Field lockField;

    /**
     * 存储文件缓存的字段引用
     */
    private static Field fileCacheField;

    /**
     * 存储组缓存的字段引用
     */
    private static Field groupCacheField;

    /**
     * 存储组映射关系的字段引用
     */
    private static Field groupMappingsField;

    /**
     * 存储文件映射关系的字段引用
     */
    private static Field fileMappingsField;

    /**
     * 存储路径缓存的字段引用
     */
    private static Field pathCacheField;

    /**
     * 存储发布者信息的字段引用
     */
    private static Field publisherField;

    /**
     * 存储根目录信息的字段引用
     */
    private static Field rootField;

    /**
     * 存储存储设备信息的字段引用
     */
    private static Field storagesField;


    /**
     * 静态初始化块，通过反射获取DefaultMagicResourceService类中所有私有字段的访问权限
     * 包括lock、fileCache、groupCache、groupMappings、fileMappings、pathCache、publisher、root、storages等字段
     * 如果获取字段失败，则记录错误日志
     */
    static {
        Class clazz = DefaultMagicResourceService.class;

        try {
            lockField = clazz.getDeclaredField("lock");
            lockField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取lock字段失败", e);
        }

        try {
            fileCacheField = clazz.getDeclaredField("fileCache");
            fileCacheField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取fileCache字段失败", e);
        }

        try {
            groupCacheField = clazz.getDeclaredField("groupCache");
            groupCacheField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取groupCache字段失败", e);
        }

        try {
            groupMappingsField = clazz.getDeclaredField("groupMappings");
            groupMappingsField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取groupMappings字段失败", e);
        }

        try {
            fileMappingsField = clazz.getDeclaredField("fileMappings");
            fileMappingsField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取fileMappings字段失败", e);
        }

        try {
            pathCacheField = clazz.getDeclaredField("pathCache");
            pathCacheField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取pathCache字段失败", e);
        }

        try {
            publisherField = clazz.getDeclaredField("publisher");
            publisherField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取publisher字段失败", e);
        }

        try {
            rootField = clazz.getDeclaredField("root");
            rootField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取root字段失败", e);
        }

        try {
            storagesField = clazz.getDeclaredField("storages");
            storagesField.setAccessible(true);
        } catch (Exception e) {
            log.error("获取storages字段失败", e);
        }
    }

    /**
     * 构造函数，初始化默认魔法资源服务
     *
     * @param resource 资源对象，用于提供基础资源配置
     * @param storages 魔法资源存储列表，包含所有可用的魔法实体存储
     * @param publisher 应用事件发布器，用于发布相关事件通知
     */
    public MyDefaultMagicResourceService(Resource resource, List<MagicResourceStorage<? extends MagicEntity>> storages, ApplicationEventPublisher publisher) {
        super(resource, storages, publisher);
    }

    /**
     * 获取当前对象的读写锁实例
     * 通过反射访问lockField字段来获取ReadWriteLock对象
     *
     * @return ReadWriteLock类型的锁对象
     * @throws RuntimeException 当无法访问lockField字段时抛出运行时异常
     */
    private ReadWriteLock getLock() {
        try {
            return (ReadWriteLock) lockField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行读锁操作的通用方法
     * 该方法会尝试获取读锁，执行提供的操作，然后释放读锁
     * 注意：当前实现中的读锁逻辑被注释掉了，直接执行supplier操作
     *
     * @param supplier 需要执行的操作，该操作将在读锁保护下执行
     * @param <R> 返回值类型
     * @return supplier执行后的返回值
     */
    private <R> R readLock(Supplier<R> supplier) {
        /**
        ReadWriteLock lock = getLock();
        try {
            if (!lock.readLock().tryLock(WRITE_LOCK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire read lock within 30 seconds");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for read lock", e);
        } finally {
            lock.readLock().unlock();
        }**/
        return supplier.get();
    }


    /**
     * 在写锁保护下执行给定的操作
     * 获取写锁后执行supplier中的操作，确保操作的原子性
     *
     * @param supplier 需要在写锁保护下执行的操作
     * @param <R> 返回值类型
     * @return supplier执行后的返回值
     * @throws RuntimeException 当无法在指定时间内获取写锁或线程被中断时抛出
     */
    private <R> R writeLock(Supplier<R> supplier) {
        ReadWriteLock lock = getLock();
        boolean acquired = false;
        try {
            if (!lock.writeLock().tryLock(WRITE_LOCK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to acquire write lock within 30 seconds");
            }
            acquired = true;
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for write lock", e);
        } finally {
            if (acquired) {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * 获取组缓存映射
     * 通过反射访问groupCacheField字段获取组缓存数据
     *
     * @return Map<String, Group> 返回组缓存映射，键为组名，值为Group对象
     * @throws RuntimeException 当字段访问被拒绝时抛出运行时异常
     */
    private Map<String, Group> getGroupCache() {
        try {
            return (Map<String, Group>) groupCacheField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取文件缓存映射
     * 通过反射访问fileCacheField字段获取文件缓存数据
     *
     * @return Map<String, MagicEntity> 返回文件缓存映射，键为文件标识，值为MagicEntity对象
     * @throws RuntimeException 当字段访问被拒绝时抛出运行时异常
     */
    private Map<String, MagicEntity> getFileCache() {
        try {
            return (Map<String, MagicEntity>) fileCacheField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取文件映射关系
     * 通过反射访问fileMappingsField字段获取文件映射数据
     *
     * @return Map<String, Resource> 返回文件映射关系，键为文件路径，值为Resource对象
     * @throws RuntimeException 当字段访问被拒绝时抛出运行时异常
     */
    private Map<String, Resource> getFileMappings() {
        try {
            return (Map<String, Resource>) fileMappingsField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取组映射信息
     *
     * @return 返回一个Map，键为字符串类型的组标识，值为Resource类型的资源对象
     * @throws RuntimeException 当无法访问groupMappingsField字段时抛出运行时异常
     */
    private Map<String, Resource> getGroupMappings() {
        try {
            return (Map<String, Resource>) groupMappingsField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取路径缓存信息
     *
     * @return 返回一个嵌套Map，外层键为字符串类型的路径标识，
     *         内层Map的键值对均为字符串类型，用于存储路径相关的缓存数据
     * @throws RuntimeException 当无法访问pathCacheField字段时抛出运行时异常
     */
    private Map<String, Map<String, String>> getPathCache() {
        try {
            return (Map<String, Map<String, String>>) pathCacheField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取应用事件发布器
     *
     * @return 返回ApplicationEventPublisher实例，用于发布应用事件
     * @throws RuntimeException 当无法访问publisherField字段时抛出运行时异常
     */
    private ApplicationEventPublisher getPublisher() {
        try {
            return (ApplicationEventPublisher) publisherField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取根资源对象
     *
     * @return 根资源对象
     */
    private Resource getRoot() {
        try {
            return (Resource) rootField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取存储映射表
     *
     * @return 存储映射表，键为字符串类型，值为MagicResourceStorage的泛型实例
     */
    private Map<String, MagicResourceStorage<? extends MagicEntity>> getStorages() {
        try {
            return (Map<String, MagicResourceStorage<? extends MagicEntity>>) storagesField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将分组列表转换为树形结构
     *
     * @param remains 待处理的分组列表
     * @param current 当前节点
     */
    private void convertToTree(List<Group> remains, TreeNode<Group> current) {
        Group temp;
        // 创建子节点列表
        List<TreeNode<Group>> childNodes = new LinkedList<>();
        Iterator<Group> iterator = remains.iterator();
        while (iterator.hasNext()) {
            temp = iterator.next();
            if (current.getNode().getId().equals(temp.getParentId())) {
                childNodes.add(new TreeNode<>(temp));
                iterator.remove();
            }
        }
        current.setChildren(childNodes);
        childNodes.forEach(it -> convertToTree(remains, it));
    }

    /**
     * 将分组列表转换为树形结构的入口方法
     *
     * @param groups 分组列表
     * @return 树形结构的根节点
     */
    private TreeNode<Group> convertToTree(List<Group> groups) {
        TreeNode<Group> root = new TreeNode<>();
        root.setNode(new Group("0", "root"));
        convertToTree(groups, root);
        return root;
    }

    /**
     * 读取所有数据并初始化
     */
    private void readAll() {
        writeLock(() -> {
            this.init();
            this.getRoot().readAll();
            // 遍历存储映射表，刷新每个分组
            getStorages().forEach((key, registry) -> refreshGroup(getRoot().getDirectory(key), registry));
            return null;
        });
    }

    /**
     * 读取数据并触发事件
     *
     * @param triggerEvent 是否触发事件
     */
    private void read(boolean triggerEvent) {
        writeLock(() -> {
            if (triggerEvent) {
                getPublisher().publishEvent(new MagicEvent("clear", EventAction.CLEAR));
            }
            this.readAll();
            // 遍历文件缓存，为每个实体发布加载事件
            getFileCache().values().forEach(entity -> {
                Group group = getGroupCache().get(entity.getGroupId());
                getPublisher().publishEvent(new FileEvent(group.getType(), EventAction.LOAD, entity));
            });
            return null;
        });
    }

    /**
     * 保存分组信息
     * 该方法负责将分组信息保存到存储系统中，包括校验、创建目录、写入元数据等操作
     *
     * @param group 待保存的分组对象，包含分组的基本信息和类型
     * @return 保存成功返回true，失败返回false
     * @throws InvalidArgumentException 当参数校验失败时抛出异常
     */
    @Override
    public boolean saveGroup(Group group) {
        isTrue(!this.getRoot().readonly(), IS_READ_ONLY);
        // 类型校验
        isTrue(getStorages().containsKey(group.getType()), NOT_SUPPORTED_GROUP_TYPE);
        // 名字校验
        notNull(group.getName(), NAME_REQUIRED);
        notNull(IoUtils.validateFileName(group.getName()), NAME_INVALID);
        // 需要填写parentId
        notNull(group.getParentId(), GROUP_ID_REQUIRED);
        MagicResourceStorage<? extends MagicEntity> storage = getStorages().get(group.getType());
        return writeLock(() -> {
            Resource resource;
            // 判断是否要保存到根节点下
            if (Constants.ROOT_ID.equals(group.getParentId())) {
                resource = getRoot().getDirectory(group.getType());
            } else {
                // 找到上级分组
                resource = getGroupResource(group.getParentId());
                // 上级分组需要存在
                isTrue(resource != null && resource.exists(), GROUP_NOT_FOUND);
            }
            Resource groupResource;
            GroupEvent event = new GroupEvent(group.getType(), group.getId() == null ? EventAction.CREATE : EventAction.SAVE, group);
            if (group.getId() == null || !getGroupCache().containsKey(group.getId())) {
                // 添加分组
                if (group.getId() == null) {
                    group.setId(UUID.randomUUID().toString().replace("-", ""));
                }
                group.setCreateTime(System.currentTimeMillis());
                group.setCreateBy(WebUtils.currentUserName());
                groupResource = resource.getDirectory(group.getName());
                // 判断分组文件夹需要不存在
                isTrue(!groupResource.exists(), FILE_SAVE_FAILURE);
                // 创建文件夹
                groupResource.mkdir();
            } else {
                Group oldGroup = getGroupCache().get(group.getId());
                if (storage.requirePath() && !Objects.equals(oldGroup.getPath(), group.getPath())) {
                    TreeNode<Group> treeNode = tree(group.getType());
                    String oldPath = oldGroup.getPath();
                    oldGroup.setPath(group.getPath());
                    // 递归找出该组下的文件
                    List<MagicEntity> entities = treeNode.findTreeNode(it -> it.getId().equals(group.getId()))
                            .flat()
                            .stream()
                            .flatMap(it -> getFileCache().values().stream().filter(f -> f.getGroupId().equals(it.getId())))
                            .collect(Collectors.toList());
                    for (MagicEntity entity : entities) {
                        String newMappingKey = storage.buildKey(entity);
                        if (getPathCache().get(group.getType()).entrySet().stream().anyMatch(entry -> entry.getValue().equals(newMappingKey) && !entry.getKey().equals(entity.getId()))) {
                            // 还原path
                            oldGroup.setPath(oldPath);
                            throw new InvalidArgumentException(SAVE_GROUP_PATH_CONFLICT);
                        }
                    }
                }
                Resource oldResource = getGroupResource(group.getId());
                groupResource = resource.getDirectory(group.getName());
                isTrue(oldResource != null && oldResource.exists(), GROUP_NOT_FOUND);
                // 设置修改时间，修改人
                group.setUpdateBy(WebUtils.currentUserName());
                group.setUpdateTime(System.currentTimeMillis());
                // 名字不一样时重命名
                if (!Objects.equals(oldGroup.getName(), group.getName())) {
                    // 判断分组文件夹需要不存在
                    isTrue(!groupResource.exists(), FILE_SAVE_FAILURE);
                    isTrue(oldResource.renameTo(groupResource), FILE_SAVE_FAILURE);
                }
            }
            // 写入分组信息
            if (groupResource.getResource(Constants.GROUP_METABASE).write(JsonUtils.toJsonString(group))) {
                putGroup(group, groupResource);
                TreeNode<Group> treeNode = tree(group.getType()).findTreeNode(it -> it.getId().equals(group.getId()));
                // 刷新分组缓存
                refreshGroup(groupResource, storage);
                if (event.getAction() != EventAction.CREATE) {
                    event.setEntities(treeNode
                            .flat()
                            .stream()
                            .flatMap(g -> listFiles(g.getId()).stream())
                            .collect(Collectors.toList()));
                }
                getPublisher().publishEvent(event);
                return true;
            }
            return false;
        });
    }

    /**
     * 移动分组到目标位置
     *
     * @param src 源分组对象，需要被移动的分组
     * @param target 目标位置标识符，分组将被移动到此位置
     * @return boolean 移动操作是否成功，成功返回true，失败返回false
     */
    private boolean moveGroup(Group src, String target) {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        MagicResourceStorage<?> storage = getStorages().get(src.getType());
        Resource targetResource = Constants.ROOT_ID.equals(target) ? this.getRoot().getDirectory(storage.folder()) : getGroupMappings().get(target);

        // 校验分组名称是否有冲突
        isTrue(!targetResource.getDirectory(src.getName()).exists(), MOVE_NAME_CONFLICT);
        targetResource = targetResource.getDirectory(src.getName());
        TreeNode<Group> treeNode = tree(storage.folder());
        String oldParentId = src.getParentId();
        src.setParentId(target);

        // 递归找出要移动的文件
        List<MagicEntity> entities = treeNode.findTreeNode(it -> it.getId().equals(src.getId()))
                .flat()
                .stream()
                .flatMap(it -> getFileCache().values().stream().filter(f -> f.getGroupId().equals(it.getId())))
                .collect(Collectors.toList());

        // 检查路径冲突
        if (storage.requirePath()) {
            for (MagicEntity entity : entities) {
                String newMappingKey = storage.buildKey(entity);
                if (getPathCache().get(src.getType()).entrySet().stream().anyMatch(entry -> entry.getValue().equals(newMappingKey) && !entry.getKey().equals(entity.getId()))) {
                    // 还原parentId
                    src.setParentId(oldParentId);
                    throw new InvalidArgumentException(MOVE_PATH_CONFLICT);
                }
            }
        }

        // 设置修改时间，修改人
        src.setUpdateBy(WebUtils.currentUserName());
        src.setUpdateTime(System.currentTimeMillis());
        Resource oldResource = getGroupMappings().get(src.getId());
        if (oldResource.renameTo(targetResource)) {
            Resource resource = targetResource.getResource(Constants.GROUP_METABASE);
            if (resource.write(JsonUtils.toJsonString(src))) {
                // 更新group缓存
                putGroup(src, targetResource);

                // 更新mapping缓存
                if (storage.requirePath()) {
                    Map<String, String> selfPathCache = getPathCache().get(storage.folder());
                    entities.forEach(entity -> selfPathCache.put(entity.getId(), storage.buildKey(entity)));
                }

                // 刷新缓存
                refreshGroup(targetResource, storage);
                getPublisher().publishEvent(new GroupEvent(src.getType(), EventAction.MOVE, src, entities));
                return true;
            }
        }
        return false;
    }

    /**
     * 移动文件到指定分组
     *
     * @param entity 要移动的实体对象，必须继承自MagicEntity
     * @param group 目标分组对象
     * @param <T> 实体类型，必须继承自MagicEntity
     * @return 移动成功返回true，失败返回false
     */
    private <T extends MagicEntity> boolean moveFile(T entity, Group group) {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        // 判断是否被锁定
        isTrue(!Constants.LOCK.equals(entity.getLock()), RESOURCE_LOCKED);
        // 设置新的分组ID
        entity.setGroupId(group.getId());
        MagicResourceStorage<?> storage = getStorages().get(group.getType());
        // 计算mappingKey
        String newMappingKey = storage.buildKey(entity);
        Resource resource = getGroupMappings().get(group.getId());
        // 判断名字和路径是否有冲突
        Resource newResource = resource.getResource(entity.getName() + storage.suffix());
        isTrue(!newResource.exists(), MOVE_NAME_CONFLICT);
        isTrue(!storage.requirePath() || getPathCache().get(storage.folder()).entrySet().stream().noneMatch(entry -> entry.getValue().equals(newMappingKey) && !entry.getKey().equals(entity.getId())), MOVE_PATH_CONFLICT);
        // 设置修改时间，修改人
        entity.setUpdateBy(WebUtils.currentUserName());
        entity.setUpdateTime(System.currentTimeMillis());
        // 写入新文件
        if (newResource.write(storage.write(entity))) {
            // 删除旧文件
            getFileMappings().remove(entity.getId()).delete();
            // 写入缓存
            putFile(storage, entity, newResource);
            getPublisher().publishEvent(new FileEvent(group.getType(), EventAction.MOVE, entity));
            return true;
        }
        return false;
    }

    /**
     * 移动文件或分组到指定的目标分组
     *
     * @param src 源文件或分组的ID
     * @param groupId 目标分组的ID，"0"表示根分组
     * @return 移动操作是否成功
     */
    @Override
    public boolean move(String src, String groupId) {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        Group group = getGroupCache().get(groupId);
        isTrue("0".equals(groupId) || group != null, GROUP_NOT_FOUND);
        isTrue(!Objects.equals(src, groupId), MOVE_NAME_CONFLICT);
        return writeLock(() -> {
            Group srcGroup = getGroupCache().get(src);
            if (srcGroup != null) {
                // 移动分组
                return moveGroup(srcGroup, groupId);
            } else {
                // 不能将文件移动至根节点下
                notNull(group, GROUP_NOT_FOUND);
                MagicEntity entity = getFileCache().get(src);
                notNull(entity, FILE_NOT_FOUND);
                // 移动文件
                return moveFile(entity.copy(), group);
            }
        });
    }

    /**
     * 复制指定的组到目标组下
     *
     * @param src 源组ID，表示要被复制的组
     * @param groupId 目标组ID，表示复制到哪个组下，"0"表示根组
     * @return 返回新创建的组的ID
     */
    @Override
    public String copyGroup(String src, String groupId) {
        // 检查根目录是否为只读模式
        isTrue(!getRoot().readonly(), IS_READ_ONLY);

        // 获取目标组信息
        Group group = getGroupCache().get(groupId);
        isTrue("0".equals(groupId) || group != null, GROUP_NOT_FOUND);
        isTrue(!Objects.equals(src, groupId), SRC_GROUP_CONFLICT);

        // 获取源组信息
        Group srcGroup = getGroupCache().get(src);
        notNull(srcGroup, GROUP_NOT_FOUND);

        // 创建新的组对象并复制源组的属性
        Group newGroup = new Group();
        newGroup.setType(srcGroup.getType());
        newGroup.setParentId(groupId);
        newGroup.setName(srcGroup.getName() + "(Copy)");
        newGroup.setPath(Objects.toString(srcGroup.getPath(), "") + "_copy");
        newGroup.setOptions(srcGroup.getOptions());
        newGroup.setPaths(srcGroup.getPaths());
        newGroup.setProperties(srcGroup.getProperties());
        saveGroup(newGroup);

        // 复制源组下的所有文件到新组中
        listFiles(src).stream()
                .map(MagicEntity::copy)
                .peek(it -> it.setGroupId(newGroup.getId()))
                .peek(it -> it.setId(null))
                .forEach(this::saveFile);
        return newGroup.getId();
    }

    /**
     * 刷新数据
     * 重写父类的refresh方法，通过调用read方法并传入true参数来实现数据刷新功能
     */
    @Override
    public void refresh() {
        this.read(true);
    }


    /**
     * 根据类型获取分组树形结构
     * 该方法通过读锁保护，从分组缓存中筛选指定类型的分组，然后转换为树形结构返回
     *
     * @param type 分组类型，用于筛选特定类型的分组数据
     * @return TreeNode<Group> 返回指定类型的分组树形结构，根节点为Group类型的TreeNode对象
     */
    @Override
    public TreeNode<Group> tree(String type) {
        // 使用读锁保护缓存访问，筛选指定类型分组并转换为树形结构
        return readLock(() -> getGroupCache().values().stream().filter(it -> type.equals(it.getType())).collect(Collectors.collectingAndThen(Collectors.toList(), this::convertToTree)));
    }


    /**
     * 构建并返回群组类型的树形结构映射
     * 该方法通过读锁保护，从群组缓存中获取所有群组数据，
     * 按照群组类型进行分组，并将每个分组转换为树形结构
     *
     * @return Map<String, TreeNode<Group>> 返回一个映射，键为群组类型，值为对应的树形结构节点
     */
    @Override
    public Map<String, TreeNode<Group>> tree() {
        // 使用读锁保护并发访问，按群组类型分组后转换为树形结构
        return readLock(() -> getGroupCache().values().stream().collect(Collectors.groupingBy(Group::getType, Collectors.collectingAndThen(Collectors.toList(), this::convertToTree))));
    }


    /**
     * 根据文件ID获取所属的组层级列表
     * 该方法会返回从当前组开始，向上追溯到根组的所有父组
     *
     * @param id 文件的唯一标识符
     * @return 包含所有父组的列表，按从当前组到根组的顺序排列；如果文件不存在或无关联组，则返回空列表
     */
    @Override
    public List<Group> getGroupsByFileId(String id) {
        return readLock(() -> {
            List<Group> groups = new ArrayList<>();
            MagicEntity entity = getFileCache().get(id);
            if (entity != null) {
                // 从文件关联的组开始，向上遍历所有父组
                Group group = getGroupCache().get(entity.getGroupId());
                while (group != null) {
                    groups.add(group);
                    group = getGroupCache().get(group.getParentId());
                }
            }
            return groups;
        });
    }


    /**
     * 根据ID获取组资源
     *
     * @param id 组资源的唯一标识符
     * @return 对应ID的Resource对象，如果不存在则返回null
     */
    @Override
    public Resource getGroupResource(String id) {
        return getGroupMappings().get(id);
    }


    /**
     * 保存文件实体
     * 该方法负责将MagicEntity类型的实体保存到文件系统中，支持创建和更新操作
     *
     * @param entity 要保存的实体对象，必须继承自MagicEntity
     * @param <T> 实体类型，必须是MagicEntity的子类
     * @return 保存成功返回true，失败返回false
     */
    @Override
    public <T extends MagicEntity> boolean saveFile(T entity) {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        // 校验必填信息
        notNull(entity.getGroupId(), GROUP_ID_REQUIRED);
        // 校验名字
        notBlank(entity.getName(), NAME_REQUIRED);
        isTrue(IoUtils.validateFileName(entity.getName()), NAME_INVALID);
        return writeLock(() -> {
            // 确定操作类型：创建或保存
            EventAction action = entity.getId() == null || !getFileCache().containsKey(entity.getId()) ? EventAction.CREATE : EventAction.SAVE;
            // 获取所在分组
            Resource groupResource = getGroupResource(entity.getGroupId());
            // 分组需要存在
            notNull(groupResource, GROUP_NOT_FOUND);
            MagicResourceStorage<T> storage;
            if (entity.getGroupId().contains(":")) {
                storage = (MagicResourceStorage<T>) this.getStorages().get(entity.getGroupId().split(":")[0]);
            } else {
                // 读取分组信息
                Group group = getGroupCache().get(entity.getGroupId());
                storage = (MagicResourceStorage<T>) this.getStorages().get(group.getType());
            }

            // 检查脚本
            if (storage.requiredScript()) {
                notBlank(entity.getScript(), SCRIPT_REQUIRED);
            }
            // 路径检查
            if (storage.requirePath()) {
                notBlank(((PathMagicEntity) entity).getPath(), PATH_REQUIRED);
                String newMappingKey = storage.buildKey(entity);
                isTrue(getPathCache().get(storage.folder()).entrySet().stream().noneMatch(entry -> entry.getValue().equals(newMappingKey) && !entry.getKey().equals(entity.getId())), PATH_CONFLICT);
            }
            storage.validate(entity);
            // 拼接文件名
            String filename = entity.getName() + storage.suffix();
            // 获取修改前的信息
            Resource fileResource = groupResource.getResource(filename);
            if (action == EventAction.CREATE) {
                if (entity.getId() == null) {
                    isTrue(!fileResource.exists(), FILE_SAVE_FAILURE);
                    // 新增操作赋值
                    entity.setId(UUID.randomUUID().toString().replace("-", ""));
                }
                entity.setCreateTime(System.currentTimeMillis());
                entity.setCreateBy(WebUtils.currentUserName());
            } else {
                MagicEntity oldEntity = getFileCache().get(entity.getId());
                // 防止创建信息被修改
                entity.setCreateBy(oldEntity.getCreateBy());
                entity.setCreateTime(oldEntity.getCreateTime());
                entity.setUpdateTime(oldEntity.getUpdateTime());
                entity.setUpdateBy(oldEntity.getUpdateBy());
                boolean sameFlag = Arrays.equals(storage.write(oldEntity), storage.write(entity));
                if (sameFlag) {
                    return true;
                }
                // 修改操作赋值
                entity.setUpdateTime(System.currentTimeMillis());
                entity.setUpdateBy(WebUtils.currentUserName());
                isTrue(!Constants.LOCK.equals(getFileCache().get(entity.getId()).getLock()), RESOURCE_LOCKED);
                Resource oldFileResource = getFileMappings().get(entity.getId());
                if (!oldFileResource.name().equals(fileResource.name())) {
                    // 重命名
                    isTrue(oldFileResource.renameTo(fileResource), FILE_SAVE_FAILURE);
                }
            }
            boolean flag = fileResource.write(storage.write(entity));
            if (flag) {
                // 发布文件事件
                getPublisher().publishEvent(new FileEvent(storage.folder(), action, entity));
                putFile(storage, entity, fileResource);
            }
            return flag;
        });
    }

    private List<MagicEntity> deleteGroup(String id) {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        Group group = getGroupCache().get(id);
        List<MagicEntity> entities = new ArrayList<>();
        // 递归删除分组和文件
        tree(group.getType())
                .findTreeNode(it -> it.getId().equals(id))
                .flat()
                .forEach(g -> {
                    getGroupCache().remove(g.getId());
                    getGroupMappings().remove(g.getId());
                    getFileCache().values().stream()
                            .filter(f -> Objects.nonNull(f.getGroupId()) && Objects.nonNull(g.getId()) && f.getGroupId().equals(g.getId())).peek(entities::add)
                            .collect(Collectors.toList())
                            .forEach(file -> {
                                getFileCache().remove(file.getId());
                                getFileCache().remove(file.getId());
                                Map<String, String> map = getPathCache().get(g.getType());
                                if (map != null) {
                                    map.remove(file.getId());
                                }
                            });
                });
        getGroupMappings().remove(id);
        getGroupCache().remove(id);
        return entities;
    }

    @Override
    public boolean delete(String id) {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        return writeLock(() -> {
            Resource resource = getGroupResource(id);
            if (resource != null) {
                // 删除分组
                Group group = getGroupCache().get(id);
                validateDeleteGroup(group);
                if (resource.exists() && resource.delete()) {
                    GroupEvent event = new GroupEvent(getGroupCache().get(group.getId()).getType(), EventAction.DELETE, group);
                    event.setEntities(deleteGroup(id));
                    getPublisher().publishEvent(event);
                    return true;
                }
            }
            resource = getFileMappings().get(id);
            // 删除文件
            if (resource != null && resource.exists()) {
                MagicEntity entity = getFileCache().get(id);
                validateDeleteEntity(entity);
                if (!resource.delete()) {
                    return true;
                }
                entity = getFileCache().remove(id);
                String type = getGroupCache().get(entity.getGroupId()).getType();
                getPublisher().publishEvent(new FileEvent(type, EventAction.DELETE, entity));
                getFileMappings().remove(id);
                getFileCache().remove(id);
                Map<String, String> map = getPathCache().get(type);
                if (map != null) {
                    map.remove(id);
                }

            }
            return true;
        });
    }

    private void validateDeleteGroup(Group group) {
        if (group == null) {
            return;
        }
        tree(group.getType())
                .findTreeNode(it -> it.getId().equals(group.getId()))
                .flat()
                .forEach(g -> getFileCache().values().stream()
                        .filter(f -> Objects.nonNull(f.getGroupId()) && Objects.nonNull(g.getId()) && f.getGroupId().equals(g.getId()))
                        .forEach(this::validateDeleteEntity));
    }

    private void validateDeleteEntity(MagicEntity entity) {
        if (!(entity instanceof DataSourceInfo)) {
            return;
        }
        DataSourceInfo dataSourceInfo = (DataSourceInfo) entity;
        String dbKey = normalizeDatasourceKey(dataSourceInfo.getKey());
        for (MagicEntity item : getFileCache().values()) {
            if (item instanceof MybatisPlusXmlInfo) {
                MybatisPlusXmlInfo info = (MybatisPlusXmlInfo) item;
                if (dbKey.equals(MybatisPlusXmlMapperRegistry.normalizeDbKey(info.getDbKey()))) {
                    throw new IllegalArgumentException("数据源[" + dbKey + "]已被 MyBatis-Plus XML[" + info.getName() + "]引用，不能删除");
                }
            }
            if (item instanceof MybatisFlexXmlInfo) {
                MybatisFlexXmlInfo info = (MybatisFlexXmlInfo) item;
                if (dbKey.equals(MybatisFlexXmlMapperRegistry.normalizeDbKey(info.getDbKey()))) {
                    throw new IllegalArgumentException("数据源[" + dbKey + "]已被 MyBatis-Flex XML[" + info.getName() + "]引用，不能删除");
                }
            }
        }
    }

    private String normalizeDatasourceKey(String key) {
        return key == null || key.trim().isEmpty() ? "default" : key.trim();
    }

    @Override
    public <T extends MagicEntity> List<T> listFiles(String groupId) {
        return readLock(() -> {
            Group group = getGroupCache().get(groupId);
            notNull(group, GROUP_NOT_FOUND);
            return getFileCache().values().stream()
                    .filter(it -> groupId.equals(it.getGroupId()))
                    .map(it -> (T) it)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 锁定资源文件
     *
     * @param id 资源文件的唯一标识符
     * @param lockState 锁定状态值
     * @return 操作是否成功，成功返回true，失败返回false
     */
    private boolean doLockResource(String id, String lockState) {
        // 检查系统是否为只读模式
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        return writeLock(() -> {
            // 获取文件实体和资源映射
            MagicEntity entity = getFileCache().get(id);
            Resource resource = getFileMappings().get(id);
            // 验证文件实体和资源是否存在
            notNull(entity, FILE_NOT_FOUND);
            notNull(resource, FILE_NOT_FOUND);
            // 获取文件所属的组信息
            Group group = getGroupCache().get(entity.getGroupId());
            notNull(group, GROUP_NOT_FOUND);
            // 更新实体的锁定状态和更新信息
            entity.setLock(lockState);
            entity.setUpdateTime(System.currentTimeMillis());
            entity.setUpdateBy(WebUtils.currentUserName());
            // 获取对应的存储器并执行写入操作
            MagicResourceStorage<? extends MagicEntity> storage = getStorages().get(group.getType());
            boolean flag = resource.write(storage.write(entity));
            if (flag) {
                // 写入成功后更新文件缓存
                putFile(storage, entity, resource);
            }
            return flag;
        });
    }


    /**
     * 获取指定资源的锁
     *
     * @param id 资源标识符，用于唯一标识需要加锁的资源
     * @return true表示成功获取锁，false表示获取锁失败
     */
    @Override
    public boolean lock(String id) {
        return doLockResource(id, Constants.LOCK);
    }


    /**
     * 解锁指定资源
     *
     * @param id 资源标识符，用于唯一识别需要解锁的资源
     * @return boolean 返回解锁操作是否成功，true表示解锁成功，false表示解锁失败
     */
    @Override
    public boolean unlock(String id) {
        return doLockResource(id, Constants.UNLOCK);
    }


    /**
     * 读取所有资源信息，包括组信息和实体信息
     *
     * @param root 根资源对象，用于获取基础资源
     * @param groups 用于存储解析出的组信息的集合
     * @param entities 用于存储解析出的魔法实体信息的集合
     * @param checked 检查标志，用于控制是否进行额外验证
     */
    private void readAllResource(Resource root, Set<Group> groups, Set<MagicEntity> entities, boolean checked) {
        // 获取组元数据资源
        Resource resource = root.getResource(Constants.GROUP_METABASE);
        MagicResourceStorage<? extends MagicEntity> storage = null;
        if (resource.exists()) {
            // 从资源中读取并解析组信息
            Group group = JsonUtils.readValue(resource.read(), Group.class);
            group.setType(mappingV1Type(group.getType()));
            storage = getStorages().get(group.getType());
            notNull(storage, NOT_SUPPORTED_GROUP_TYPE);
        }
        // 递归读取所有资源
        readAllResource(root, storage, groups, entities, null, "/", checked);
    }


    /**
     * 递归读取所有资源文件并存储到指定的存储器中
     *
     * @param root 根资源对象，表示要读取的资源目录
     * @param storage 魔法资源存储器，用于存储和读取魔实体对象，如果为null则根据根资源名称获取默认存储器
     * @param groups 分组集合，用于存储读取到的分组信息
     * @param entities 魔实体集合，用于存储读取到的魔实体对象
     * @param mappingKeys 映射键集合，用于检查映射键冲突
     * @param path 当前路径字符串，用于构建映射键
     * @param checked 是否进行冲突检查的布尔值
     */
    private void readAllResource(Resource root, MagicResourceStorage<? extends MagicEntity> storage, Set<Group> groups, Set<MagicEntity> entities, Set<String> mappingKeys, String path, boolean checked) {
        storage = storage == null ? getStorages().get(root.name()) : storage;
        if (storage != null) {
            mappingKeys = mappingKeys == null ? new HashSet<>() : mappingKeys;
            if (!storage.allowRoot()) {
                Resource resource = root.getResource(Constants.GROUP_METABASE);
                // 分组信息不存在时，直接返回不处理
                if (resource.exists()) {
                    // 读取分组信息
                    Group group = JsonUtils.readValue(resource.read(), Group.class);
                    group.setType(mappingV1Type(group.getType()));
                    groups.add(group);
                    if (storage.requirePath()) {
                        path = path + Objects.toString(group.getPath(), "") + "/";
                    }
                }

            }
            for (Resource file : root.files(storage.suffix())) {
                MagicEntity entity = storage.read(file.read());
                if (storage.allowRoot()) {
                    entity.setGroupId(storage.folder() + ":0");
                }
                String mappingKey;
                if (storage instanceof AbstractPathMagicResourceStorage) {
                    mappingKey = ((AbstractPathMagicResourceStorage) storage).buildMappingKey((PathMagicEntity) entity, path);
                } else {
                    mappingKey = storage.buildKey(entity);
                }
                if (checked) {
                    String groupId = entity.getGroupId();
                    // 名字和路径冲突检查
                    getFileCache().values().stream()
                            // 同一分组
                            .filter(it -> Objects.equals(it.getGroupId(), groupId))
                            // 非自身
                            .filter(it -> !it.getId().equals(entity.getId()))
                            .forEach(it -> isTrue(!Objects.equals(it.getName(), entity.getName()), RESOURCE_PATH_CONFLICT.format(entity.getName())));

                }
                // 自检
                isTrue(mappingKeys.add(mappingKey), RESOURCE_PATH_CONFLICT.format(mappingKey));
                entities.add(entity);
            }

        }
        for (Resource directory : root.dirs()) {
            readAllResource(directory, storage, groups, entities, mappingKeys, path, checked);
        }
    }


    /**
     * 兼容1.x版本
     */
    private String mappingV1Type(String type) {
        if ("1".equals(type)) {
            return "api";
        } else if ("2".equals(type)) {
            return "function";
        }
        return type;
    }

    /**
     * 上传资源文件
     *
     * @param inputStream 输入流，包含要上传的资源数据
     * @param full 是否全量上传模式，true表示全量替换，false表示增量更新
     * @return 上传操作是否成功
     * @throws IOException 读写操作异常
     */
    @Override
    public boolean upload(InputStream inputStream, boolean full) throws IOException {
        isTrue(!getRoot().readonly(), IS_READ_ONLY);
        try {
            ZipResource zipResource = new ZipResource(inputStream);
            Set<Group> groups = new LinkedHashSet<>();
            Set<MagicEntity> entities = new LinkedHashSet<>();
            return writeLock(() -> {
                readAllResource(zipResource, groups, entities, !full);

                // 全量模式下先删除现有数据
                if (full) {
                    // 全量模式先删除处理。
                    getRoot().delete();
                    this.init();
                    getPublisher().publishEvent(new MagicEvent("clear", EventAction.CLEAR));
                }

                // 保存分组信息
                for (Group group : groups) {
                    saveGroup(group);
                }

                // 保存实体文件
                for (MagicEntity entity : entities) {
                    saveFile(entity);
                }
                return true;
            });
        } finally {
            IoUtils.close(inputStream);
        }
    }


    /**
     * 初始化方法，用于清空各种缓存和映射，并根据存储配置初始化路径缓存、根组和目录结构
     * 该方法会清空文件映射、组缓存、组映射、文件缓存和路径缓存，然后遍历所有存储配置，
     * 根据配置要求创建相应的路径缓存、根组和目录结构
     */
    private void init() {
        getFileMappings().clear();
        getGroupCache().clear();
        getGroupMappings().clear();
        getFileCache().clear();
        getPathCache().clear();

        // 遍历所有存储配置，根据配置要求初始化路径缓存、根组和目录结构
        getStorages().forEach((key, registry) -> {
            if (registry.requirePath()) {
                getPathCache().put(registry.folder(), new HashMap<>(32));
            }
            Resource folder = getRoot().getDirectory(key);

            // 如果允许根目录访问，则创建根组
            if (registry.allowRoot()) {
                String rootId = key + ":0";
                Group group = new Group();
                group.setId(rootId);
                group.setType(key);
                group.setParentId("0");
                putGroup(group, folder);
            }

            // 如果目录不存在则创建目录
            if (!folder.exists()) {
                folder.mkdir();
            }
        });
    }

    /**
     * 初始化资源，清理相关缓存并重新建立资源映射关系
     *
     * @param resource 需要初始化的资源路径或标识符
     */
    private void initResource(String resource) {
        // 清理资源相关的各种缓存映射
        getFileMappings().remove(resource);
        getGroupCache().remove(resource);
        getGroupMappings().remove(resource);
        getFileCache().remove(resource);
        getPathCache().remove(resource);

        // 遍历所有存储配置，重新建立资源映射关系
        getStorages().forEach((key, registry) -> {
            if (resource.equals(registry.folder())) {
                if (registry.requirePath()) {
                    getPathCache().put(registry.folder(), new HashMap<>(32));
                }
                Resource folder = getRoot().getDirectory(key);
                if (registry.allowRoot()) {
                    String rootId = key + ":0";
                    Group group = new Group();
                    group.setId(rootId);
                    group.setType(key);
                    group.setParentId("0");
                    putGroup(group, folder);
                }
                if (!folder.exists()) {
                    folder.mkdir();
                }
            }
        });
    }


    /**
     * 将文件存储到指定的存储系统中，并更新相关的缓存映射
     *
     * @param storage 存储系统实例，用于处理资源的存储和路径构建
     * @param entity 要存储的实体对象，包含文件的相关信息
     * @param resource 要存储的资源对象
     */
    private void putFile(MagicResourceStorage<?> storage, MagicEntity entity, Resource resource) {
        // 更新文件映射缓存，将实体ID与资源进行关联
        getFileMappings().put(entity.getId(), resource);
        // 更新文件缓存，将实体ID与实体对象进行关联
        getFileCache().put(entity.getId(), entity);
        // 如果存储系统需要路径信息，则更新路径缓存
        if (storage.requirePath()) {
            getPathCache().get(storage.folder()).put(entity.getId(), storage.buildKey(entity));
        }
    }

    /**
     * 将群组和资源映射关系存储到系统中
     * 该方法会同时更新群组映射表和群组缓存
     *
     * @param group 群组对象，包含群组的基本信息
     * @param resource 与群组关联的资源对象
     */
    private void putGroup(Group group, Resource resource) {
        getGroupMappings().put(group.getId(), resource);
        getGroupCache().put(group.getId(), group);
    }


    /**
     * 刷新资源组，根据存储配置决定是直接处理根目录还是按子目录分组处理
     *
     * @param folder 资源文件夹对象，包含要处理的文件和子目录
     * @param storage 魔法资源存储对象，用于读取和管理资源，泛型参数为魔法实体的子类型
     */
    private void refreshGroup(Resource folder, MagicResourceStorage<? extends MagicEntity> storage) {
        if (storage.allowRoot()) {
            // 直接处理根目录下的所有指定后缀的文件
            folder.files(storage.suffix()).forEach(file -> putFile(storage, storage.readResource(file), file));
        } else {
            // 按子目录处理，每个子目录需要有元数据文件才能作为资源组处理
            folder.dirs().forEach(dir -> {
                Resource meta = dir.getResource(Constants.GROUP_METABASE);
                if (meta.exists()) {
                    // 读取并解析组元数据，然后处理该组及其文件
                    putGroup(JsonUtils.readValue(meta.read(), Group.class), dir);
                    dir.files(storage.suffix()).forEach(file -> putFile(storage, storage.readResource(file), file));
                }
            });
        }
    }


    /**
     * 读取指定资源并根据需要触发相关事件
     *
     * @param resource 要读取的资源名称
     */
    private void readResource(String resource) {
        this.writeLock(() -> {
            // 在写锁保护下初始化资源并刷新相关存储
            writeLock(() -> {
                this.initResource(resource);
                this.getRoot().readAll();
                getStorages().forEach((key, registry) -> {
                    if (resource.equals(registry.folder())) {
                        refreshGroup(getRoot().getDirectory(key), registry);
                    }
                });
                return null;
            });

            //this.readAll();
            getFileCache().values().forEach((entity) -> {
                Group group = (Group)getGroupCache().get(entity.getGroupId());
                if (group.getType().equalsIgnoreCase(resource)) {
                    getPublisher().publishEvent(new FileEvent(group.getType(), EventAction.LOAD, entity,resource));
                }
            });
            return null;
        });

        writeLock(() -> {

            // 遍历文件缓存并为匹配的组发布文件事件
            getFileCache().values().forEach(entity -> {
                Group group = getGroupCache().get(entity.getGroupId());
                if (group.getType().equalsIgnoreCase(resource)) {
                    getPublisher().publishEvent(new FileEvent(group.getType(), EventAction.LOAD, entity,resource));
                }
            });
            return null;
        });
    }

    /**
     * 刷新指定的资源
     * 该方法通过调用readResource方法并设置强制刷新参数为true来实现资源的刷新功能
     *
     * @param resource 需要刷新的资源名称或路径
     */
    public void refreshResource(String resource) {
        this.readResource(resource);
    }

    public boolean refreshResourceById(String type, String id) {
        return writeLock(() -> {
            MagicEntity entity = getFileCache().get(id);
            MagicResourceStorage<?> storage = getStorages().get(type);
            Resource file = getFileMappings().get(id);
            if (entity == null || storage == null || file == null || !file.exists()) {
                return false;
            }
            MagicEntity fresh = storage.readResource(file);
            fresh.setId(id);
            putFile(storage, fresh, file);
            getPublisher().publishEvent(new FileEvent(type, EventAction.LOAD, fresh, type));
            return true;
        });
    }

}

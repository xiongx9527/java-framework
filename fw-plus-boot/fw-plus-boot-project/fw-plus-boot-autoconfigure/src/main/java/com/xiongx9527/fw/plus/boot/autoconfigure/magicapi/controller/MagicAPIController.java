package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.model.*;
import org.ssssssss.magicapi.core.service.MagicDynamicRegistry;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.utils.JdbcUtils;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@RestController
public class MagicAPIController extends MagicController implements MagicExceptionHandler {


    /**
     * 魔法配置对象，用于存储和管理魔法相关的配置信息
     * 该配置对象在类实例化时初始化，后续不可更改
     */
    private final MagicConfiguration configuration ;


    /**
     * 构造函数，初始化MagicAPIController
     * @param configuration 魔法配置对象
     */
    public MagicAPIController(MagicConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    /**
     * 检查用户是否有权限访问指定资源
     * @param request HTTP请求对象
     * @param authorization 授权信息
     * @return 如果允许访问返回true，否则返回false
     */
    boolean allowVisit(MagicHttpServletRequest request, Authorization authorization) {
        if (authorization == null) {
            return true;
        }
        MagicUser magicUser = (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
        return this.configuration.getAuthorizationInterceptor().allowVisit(magicUser, request, authorization);
    }

    /**
     * 检查用户是否有权限访问指定资源（带用户组）
     * @param request HTTP请求对象
     * @param authorization 授权信息
     * @param group 用户组信息
     * @return 如果允许访问返回true，否则返回false
     */
    boolean allowVisit(MagicHttpServletRequest request, Authorization authorization, Group group) {
        if (authorization == null) {
            return true;
        }
        MagicUser magicUser = (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
        return this.configuration.getAuthorizationInterceptor().allowVisit(magicUser, request, authorization, group);
    }

    /**
     * 检查用户是否有权限访问指定实体资源
     * @param request HTTP请求对象
     * @param authorization 授权信息
     * @param entity 魔法实体对象
     * @return 如果允许访问返回true，否则返回false
     */
    boolean allowVisit(MagicHttpServletRequest request, Authorization authorization, MagicEntity entity) {
        if (authorization == null) {
            return true;
        }
        MagicUser magicUser = (MagicUser) request.getAttribute(Constants.ATTRIBUTE_MAGIC_USER);
        return this.configuration.getAuthorizationInterceptor().allowVisit(magicUser, request, authorization, entity);
    }


    /**
     * 获取选项列表
     *
     * @return 包含选项信息的JsonBean对象，数据为List<List<String>>格式，
     *         每个内部List包含三个元素：[value, name, defaultValue]
     */
    @RequestMapping("${magic-api.web:/magic-api/web}/optionList")
    @ResponseBody
    @Valid(requireLogin = false)
    public JsonBean<List<List<String>>> options() {
        // 将Options枚举转换为包含value、name、defaultValue的列表集合
        return new JsonBean<>(Stream.of(Options.values()).map(item -> Arrays.asList(item.getValue(), item.getName(), item.getDefaultValue())).collect(Collectors.toList()));
    }

    /**
     * 重新加载指定资源
     * 
     * 该方法用于重新加载指定名称的资源，需要用户具有RELOAD权限。
     * 方法会首先检查当前用户是否具有重载资源的权限，然后获取Magic资源配置服务并刷新指定资源。
     *
     * @param resource 资源名称，通过路径变量传入，标识需要重新加载的资源
     * @param request HTTP请求对象，包含用户认证信息用于权限验证
     * @return JsonBean<Boolean> 返回封装了布尔值的JsonBean对象，
     *         true表示资源重新加载成功，false表示失败（权限验证不通过时会抛出异常）
     */
    @GetMapping("${magic-api.web:/magic-api/web}/{resource}/reloadResource")
    @ResponseBody
    public JsonBean<Boolean> reloadResource(@PathVariable("resource") String resource, MagicHttpServletRequest request) {
        isTrue(allowVisit(request, Authorization.RELOAD), PERMISSION_INVALID);
        MyDefaultMagicResourceService service = (MyDefaultMagicResourceService)MagicConfiguration.getMagicResourceService();
        service.refreshResource(resource);
        return new JsonBean<>(true);
    }

    @GetMapping("${magic-api.web:/magic-api/web}/{type}/reload/{id}")
    @ResponseBody
    public JsonBean<Boolean> reloadSingleResource(@PathVariable("type") String type,
                                                   @PathVariable("id") String id,
                                                   MagicHttpServletRequest request) {
        isTrue(allowVisit(request, Authorization.RELOAD), PERMISSION_INVALID);
        MyDefaultMagicResourceService service = (MyDefaultMagicResourceService)MagicConfiguration.getMagicResourceService();
        return new JsonBean<>(service.refreshResourceById(type, id));
    }


    /**
     * 处理组节点并构建包含属性的树结构
     *
     * 该方法递归地处理组节点，根据用户权限筛选可访问的子节点和文件资源，
     * 构建一个新的树结构，其中包含经过权限验证的节点。
     *
     * 处理过程分为两个部分：
     * 1. 处理子组节点：遍历当前组节点的所有子节点，过滤出用户有查看权限的节点，
     *    然后递归调用本方法处理每个有权限的子节点
     * 2. 处理文件资源：如果当前节点不是根节点，则获取该节点下的所有文件资源，
     *    过滤出用户有查看权限的文件，按名称排序后添加到结果树中
     *
     * @param groupNode 需要处理的组节点，包含组的基本信息和子节点
     * @param request HTTP请求对象，包含用户认证信息用于权限验证
     * @return TreeNode<Attributes<Object>> 返回处理后的树节点，包含经过权限验证的
     *         子组节点和文件资源，每个节点都带有相应的属性
     */
    private TreeNode<Attributes<Object>> process(TreeNode<Group> groupNode, MagicHttpServletRequest request) {
        MyDefaultMagicResourceService service = (MyDefaultMagicResourceService)MagicConfiguration.getMagicResourceService();
        TreeNode<Attributes<Object>> value = new TreeNode<>();
        value.setNode(groupNode.getNode());
        // 处理子组节点：筛选有查看权限的子节点并递归处理
        groupNode.getChildren().stream()
                .filter(it -> allowVisit(request, Authorization.VIEW, it.getNode()))
                .map(it -> process(it, request))
                .forEach(value::addChild);
        // 处理文件资源：非根节点才处理其下的文件资源
        if (!Constants.ROOT_ID.equals(groupNode.getNode().getId())) {
            service
                    .listFiles(groupNode.getNode().getId())
                    .stream()
                    .filter(it -> allowVisit(request, Authorization.VIEW, it))
                    .map(MagicEntity::simple)
                    .sorted(Comparator.comparing(MagicEntity::getName))
                    .map((Function<MagicEntity, TreeNode>) TreeNode::new)
                    .forEach(value::addChild);
        }
        return value;
    }

    /**
     * 获取指定资源的树形结构数据
     *
     * 该方法处理前端请求，返回指定资源的完整树形结构，包含经过权限验证的组节点和文件资源。
     * 方法会从资源服务中获取完整的树结构，然后根据请求中的资源标识符进行筛选，
     * 对匹配的资源节点进行权限处理，并添加默认映射的实体资源。
     *
     * 处理流程：
     * 1. 获取资源服务中的完整树结构
     * 2. 遍历树结构，找到与请求参数匹配的资源
     * 3. 使用process方法处理该资源节点，进行权限过滤
     * 4. 如果存在子节点，向第一个子节点组中添加默认映射的实体资源
     * 5. 将处理结果放入返回映射中
     *
     * @param resource 资源标识符，通过路径变量传入，用于指定需要获取的资源类型
     * @param request HTTP请求对象，包含用户认证信息用于权限验证
     * @return JsonBean<Map<String, TreeNode<Attributes<Object>>>> 返回封装了资源树映射的JsonBean对象，
     *         映射的键为资源名称，值为对应的树节点结构，包含经过权限验证的节点
     */
    @PostMapping("${magic-api.web:/magic-api/web}/{resource}/resource")
    @ResponseBody
    public JsonBean<Map<String, TreeNode<Attributes<Object>>>> resources(@PathVariable("resource") String resource,MagicHttpServletRequest request) {
        MyDefaultMagicResourceService service = (MyDefaultMagicResourceService)MagicConfiguration.getMagicResourceService();
        Map<String, TreeNode<Group>> tree = service.tree();
        Map<String, TreeNode<Attributes<Object>>> result = new HashMap<>();
        // 遍历资源树，查找匹配指定资源标识符的节点
        tree.forEach((key, value) -> {
            if (resource.equals(key)) {
                // 处理匹配的资源节点，进行权限过滤
                TreeNode<Attributes<Object>> node = process(value, request);
                List<TreeNode<Attributes<Object>>> groups = node.getChildren();
                // 如果存在子节点组，向第一个组中添加默认映射的实体资源
                if(groups.size() > 0){
                    List<TreeNode<Attributes<Object>>> nodes = groups.get(0).getChildren();
                    this.configuration.getMagicDynamicRegistries().stream()
                            .filter(it -> it.getMagicResourceStorage().folder().equals(key))
                            .findFirst()
                            .map(MagicDynamicRegistry::defaultMappings)
                            .ifPresent(mappings -> {
                                // 将每个默认映射的实体资源添加到节点列表中
                                for (MagicEntity mapping : mappings) {
                                    nodes.add(new TreeNode<>(mapping));
                                }
                            });
                }
                result.put(key, node);
            }
        });
        return new JsonBean<>(result);
    }


    /**
     * 测试数据库连接
     *
     * 该方法用于验证指定的数据源配置是否可以成功建立数据库连接。
     * 支持从配置中心动态获取数据库连接信息（如果存在），优先使用配置中心的值，
     * 若配置中心不存在则使用请求体中提供的值。
     *
     * 处理流程：
     * 1. 根据数据源的 key 从配置中心获取或回退到请求参数中的 url、username、password、driverClassName
     * 2. 尝试使用获取到的信息建立数据库连接
     * 3. 连接成功后立即关闭连接
     * 4. 若过程中发生异常，返回异常信息；否则返回 "ok"
     *
     * @param info 数据源信息对象，包含 key、url、username、password、driverClassName 等字段
     * @return JsonBean<String> 返回封装了结果的 JsonBean 对象，
     *         成功时返回 "ok"，失败时返回异常消息
     */
    @RequestMapping("${magic-api.web:/magic-api/web}/datasource/myjdbc/test")
    @ResponseBody
    public JsonBean<String> test(@RequestBody DataSourceInfo info) {
        try {
            String url = MagicAPIConfigUtils.getDbConfig(info.getKey(), "url", info.getUrl());
            String username = MagicAPIConfigUtils.getDbConfig(info.getKey(), "username", info.getUsername());
            String password = MagicAPIConfigUtils.getDbConfig(info.getKey(), "password", info.getPassword());
            String driverClassName = MagicAPIConfigUtils.getDbConfig(info.getKey(), "driverClassName", info.getDriverClassName());
            Connection connection = JdbcUtils.getConnection(driverClassName, url, username, password);
            JdbcUtils.close(connection);
        } catch (Exception e) {
            return new JsonBean<>(e.getMessage());
        }
        return new JsonBean<>("ok");
    }

}

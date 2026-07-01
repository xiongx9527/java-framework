package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model.VueInfo;
import lombok.Data;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.TreeNode;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VueInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<VueInfo> implements MagicBaseService {

    /**
     * 从配置文件中读取安全认证用户名
     * 通过@Value注解从spring.security.username配置项获取用户名值，
     * 如果配置项不存在则使用默认空字符串
     */
    @Value("${spring.security.username:}")
    private String username;


    /**
     * 重写父类或接口的folder方法
     *
     * 此方法用于指定前端框架的版本或类型
     * 选择返回"vue3"是因为当前类或模块专注于Vue.js 3.x版本的实现
     *
     * @return 字符串"vue3"，表示与Vue.js 3.x版本相关联的资源或配置
     */
    @Override
    public String folder() {
        return "vue3";
    }


    /**
     * 重写magicClass方法
     * 该方法用于返回一个表示VueInfo类型的Class对象
     * 主要用途是提供给反射机制使用，以便在运行时获取VueInfo类的相关信息
     *
     * @return 返回VueInfo类的Class对象
     */
    @Override
    public Class<VueInfo> magicClass() {
        return VueInfo.class;
    }


    /**
     * 验证VueInfo实体的合法性
     * 该方法主要用于在执行保存、更新或其他相关操作前，验证VueInfo对象的属性是否符合预期的规则和约束
     * 例如，可以检查必填字段是否已填写，字段长度是否超出限制，或者字段值是否符合特定的格式等
     *
     * @param entity VueInfo对象，即待验证的实体
     *               该参数不应为null，否则可能会导致验证过程中的空指针异常
     */
    @Override
    public void validate(VueInfo entity) {
        validateJSON(entity.getParams(),"运行参数");
    }


    /**
     * 查找所有Vue信息列表
     *
     * @return VueInfo对象列表，包含所有找到的Vue文件信息
     */
    public List<VueInfo> findAll() {
        // 获取指定文件夹下的所有文件并返回Vue信息列表
        return this.magicResourceService.files(folder());
    }

    /**
     * 构建并返回分组树结构
     *
     * @return TreeNode<Group> 分组树的根节点，包含完整的树形结构数据
     */
    public TreeNode<Group> groupTree() {
        // 获取指定文件夹下的所有文件并返回Vue信息列表
        return this.magicResourceService.tree(folder());
    }


    /**
     * 构建Vue3组件树结构
     *
     * @param groupTreeList 组织结构树列表，包含分组信息的树形结构
     * @param vueInfoMaps Vue信息映射表，key为分组ID，value为该分组下的Vue组件信息列表
     * @param groupId 当前处理的分组ID，用于筛选指定父节点下的子节点
     * @return Vue3组件信息列表，包含完整的树形结构关系
     */

    private List<Vue3ComponentInfo> buildVue3ComponentTree(
            List<TreeNode<Group>> groupTreeList,
            Map<String, List<VueInfo>> vueInfoMaps,
            String groupId) {

        List<Vue3ComponentInfo> vue3ComponentInfos = new ArrayList<>();

        if (groupTreeList == null || groupTreeList.isEmpty()) {
            return vue3ComponentInfos;
        }

        // 按创建时间排序分组节点
        List<TreeNode<Group>> sortedGroupTrees = groupTreeList.stream()
                .filter(groupTree -> {
                    Group node = groupTree.getNode();
                    return node != null && groupId.equals(node.getParentId());
                })
                .sorted(Comparator.comparing((TreeNode<Group> groupTree) ->
                        groupTree.getNode().getCreateTime() != null ? groupTree.getNode().getCreateTime() : Long.MIN_VALUE))
                .collect(Collectors.toList());

        for (TreeNode<Group> groupTree : sortedGroupTrees) {
            Group node = groupTree.getNode();

            Vue3ComponentInfo vue3ComponentInfo = new Vue3ComponentInfo();
            vue3ComponentInfo.setId(node.getId());
            vue3ComponentInfo.setPid(node.getParentId());
            vue3ComponentInfo.setName(node.getName()+" ("+groupTree.getNode().getPath()+")");
            vue3ComponentInfo.setCreateTime(node.getCreateTime());
            vue3ComponentInfo.setUpdateTime(node.getUpdateTime());
            vue3ComponentInfo.setIsGroup(1);

            List<Vue3ComponentInfo> children = new ArrayList<>();

            // 处理子节点中的分组信息（递归）
            List<TreeNode<Group>> childNodes = groupTree.getChildren();
            if (childNodes != null && !childNodes.isEmpty()) {
                children.addAll(buildVue3ComponentTree(childNodes, vueInfoMaps, node.getId()));
            }

            // 添加当前分组下的Vue组件信息，并按创建时间排序
            List<VueInfo> vueInfos = vueInfoMaps.get(node.getId());
            if (vueInfos != null && !vueInfos.isEmpty()) {
                List<Vue3ComponentInfo> componentChildren = vueInfos.stream()
                        .sorted(Comparator.comparing((VueInfo vueInfo) ->
                                vueInfo.getCreateTime() != null ? vueInfo.getCreateTime() : Long.MIN_VALUE))
                        .map(vueInfo -> {
                            Vue3ComponentInfo childVue3ComponentInfo = new Vue3ComponentInfo();
                            childVue3ComponentInfo.setId(vueInfo.getId());
                            childVue3ComponentInfo.setPid(vueInfo.getGroupId());
                            childVue3ComponentInfo.setIsGroup(0);
                            childVue3ComponentInfo.setName(vueInfo.getName()+" ("+vueInfo.getPath()+")");
                            childVue3ComponentInfo.setCreateTime(vueInfo.getCreateTime());
                            childVue3ComponentInfo.setUpdateTime(vueInfo.getUpdateTime());
                            return childVue3ComponentInfo;
                        })
                        .collect(Collectors.toList());
                children.addAll(componentChildren);
            }

            // 对所有子节点按创建时间排序
            children.sort(Comparator.comparing((Vue3ComponentInfo component) ->
                    component.getCreateTime() != null ? component.getCreateTime() : Long.MIN_VALUE));

            vue3ComponentInfo.setChildren(children);
            vue3ComponentInfos.add(vue3ComponentInfo);
        }
        return vue3ComponentInfos;
    }

    /**
     * 根据ID获取Vue3组件的源码信息
     * @param id 组件ID
     * @return Vue3组件信息对象
     */
    public Vue3ComponentInfo getSourceCode(String id) {
        // 获取Vue组件信息
        VueInfo vueInfo = magicResourceService.file(id);
        // 构建Vue3组件信息对象
        Vue3ComponentInfo vue3ComponentInfo = new Vue3ComponentInfo();
        vue3ComponentInfo.setId(vueInfo.getId());
        vue3ComponentInfo.setName(vueInfo.getName());
        vue3ComponentInfo.setSourceCode(vueInfo.getScript());
        vue3ComponentInfo.setCompileJs(vueInfo.getCompileJs());
        vue3ComponentInfo.setCompileCss(vueInfo.getCompileCss());
        vue3ComponentInfo.setRemark(vueInfo.getDescription());
        vue3ComponentInfo.setCreateTime(vueInfo.getCreateTime());
        vue3ComponentInfo.setUpdateTime(vueInfo.getUpdateTime());

        return vue3ComponentInfo;
    }



    /**
     * 查找Vue3组件树结构
     *
     * @return Vue3组件信息列表，表示完整的组件树结构
     */
    public List<Vue3ComponentInfo> findVue3ComponentTree() {
        // 获取分组树结构
        TreeNode<Group> groupTree = this.groupTree();
        // 查询所有Vue信息
        List<VueInfo> vueInfos = ListUtils.emptyIfNull(this.findAll());
        // 构建Vue3组件树，按分组ID对Vue信息进行分组处理
        Map<String, List<VueInfo>> groupedVueInfos = vueInfos.stream().collect(Collectors.groupingBy(VueInfo::getGroupId));
        String rootGroupId = "0";
        return buildVue3ComponentTree(groupTree.getChildren(), groupedVueInfos, rootGroupId);
    }


    /**
     * 生成映射键
     * 此方法重写自父类，专门用于根据Vue信息和魔法资源服务中的组路径来构建映射键
     *
     * @param vueInfo Vue信息，包含有关Vue组件的数据，此处用于获取组ID以构建映射键
     * @return String 返回构建的映射键字符串
     */
    @Override
    public String buildMappingKey(VueInfo vueInfo) {
        // 调用重载方法，传入Vue信息和从魔法资源服务中获取的组路径
        return buildMappingKey(vueInfo, magicResourceService.getGroupPath(vueInfo.getGroupId()));
    }


    @Data
    public static class Vue3ComponentInfo {
        private String id;
        private String pid;
        private int isGroup;//1:分组；0:组件
        private String name;
        private String sourceCode;
        private String compileJs;
        private String compileCss;
        private String remark;
        private Long createTime;
        private Long updateTime;

        private List<Vue3ComponentInfo> children;
    }

}

package com.xiongx9527.fw.plus.auth.social.enterprise;

import com.xiongx9527.fw.plus.auth.social.IdpConfig;
import com.xiongx9527.fw.plus.auth.social.IdpType;
import java.util.List;

/**
 * 组织用户树能力接口，仅由支持组织架构的企业级 IdP 实现（企业微信 / 钉钉 / 飞书 / Lark）。
 *
 * <p>实现类复用自身已带缓存与「失效自动从远程重取」的应用级 token，无需额外处理鉴权。</p>
 */
public interface SocialOrgTreeProvider {

    /**
     * 是否支持该 IdP 类型。
     */
    boolean supports(IdpType type);

    /**
     * 拉取组织用户树（部门 + 直属用户，递归子部门，一次性全量）。
     *
     * @param config IdP 配置
     * @return 顶层部门节点列表（每个节点含子部门与用户）
     */
    List<SocialOrgNode> getUserTree(IdpConfig config);

    /**
     * 懒加载：按需展开单个节点（取该部门直属子部门与直属用户，不再递归）。
     * 适用于大型组织，避免一次性全量拉取；由调用方在展开时逐级调用。
     *
     * @param config IdP 配置
     * @param nodeId 部门节点标识（企业微信/钉钉=部门 id；飞书=open_department_id；LDAP=条目 DN）
     * @return 该节点（含直属子部门与用户，子部门不递归）
     */
    SocialOrgNode getNode(IdpConfig config, String nodeId);
}

package com.xiongx9527.fw.plus.auth.social.enterprise;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织树节点（部门）。包含子部门与直属用户。
 */
public class SocialOrgNode {

    private String id;
    private String name;
    private String parentId;
    private List<SocialOrgNode> children = new ArrayList<SocialOrgNode>();
    private List<SocialOrgUser> users = new ArrayList<SocialOrgUser>();

    public SocialOrgNode() {
    }

    public SocialOrgNode(String id, String name, String parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<SocialOrgNode> getChildren() {
        return children;
    }

    public void setChildren(List<SocialOrgNode> children) {
        this.children = children;
    }

    public List<SocialOrgUser> getUsers() {
        return users;
    }

    public void setUsers(List<SocialOrgUser> users) {
        this.users = users;
    }

    public void addChild(SocialOrgNode child) {
        children.add(child);
    }

    public void addUser(SocialOrgUser user) {
        users.add(user);
    }
}

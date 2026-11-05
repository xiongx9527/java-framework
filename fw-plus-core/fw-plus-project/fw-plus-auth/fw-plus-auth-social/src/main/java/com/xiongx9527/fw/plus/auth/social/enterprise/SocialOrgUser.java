package com.xiongx9527.fw.plus.auth.social.enterprise;

/**
 * 组织树中的用户节点。
 */
public class SocialOrgUser {

    private String userId;
    private String name;
    private String email;

    public SocialOrgUser() {
    }

    public SocialOrgUser(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

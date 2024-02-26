package com.fastpan.entity.dto;

/**
 * Session作用：管理用户（登录）状态
 * DTO作用：依然是返回结果给前端
 * 如login时存在了session里，uploadFile时就用上了
 */
public class SessionWebUserDto {
    private String nickName;
    private String userId;
    private Boolean isAdmin;
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }
}

package cn.smu.edu.auth.service.impl;

/**
 * 防腐层接口 — edu-auth 通过此接口查询用户，实现解耦
 * 实现：OpenFeign 调用 edu-user，或直接查本地缓存
 */
public interface UserQueryPort {
    UserInfo findByPhone(String phone);
    UserInfo findById(Long userId);
    UserInfo findByWechatOpenId(String openId);
}

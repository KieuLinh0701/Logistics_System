package com.logistics.service.user;

import com.logistics.entity.User;

public interface UserUserService {
    boolean checkLocked(Integer userId);
    Integer getShopId(User user);
    Integer getShopId(Integer userId);
    User getShop(User user);
    User getUser(int userId);
}
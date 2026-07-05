package com.logistics.service.user.impl;

import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.repository.UserRepository;
import com.logistics.service.user.UserUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserUserServiceImpl implements UserUserService {

    private final UserRepository repository;

    @Override
    public boolean checkLocked(Integer id) {
        Integer shopId = getShopId(id);

        User user = getUser(shopId);
        return user.getLocked();
    }

    @Override
    public Integer getShopId(User user) {
        return user.getCurrentShop() != null
                ? user.getCurrentShop()
                .getId()
                : user.getId();
    }

    @Override
    public Integer getShopId(Integer userId) {
        User user = getUser(userId);
        return user.getCurrentShop() != null
                ? user.getCurrentShop()
                .getId()
                : user.getId();
    }

    @Override
    public User getShop(User user) {
        Integer shopId = getShopId(user);

        return getUser(shopId);
    }

    @Override
    public User getUser(int userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }
}
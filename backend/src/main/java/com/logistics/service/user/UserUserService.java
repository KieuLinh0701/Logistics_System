package com.logistics.service.user;

import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserUserService {

    private final UserRepository repository;

    public boolean checkLocked(Integer id) {
        Integer shopId = getShopId(id);

        User user = getUser(shopId);
        return user.getLocked();
    }

    public Integer getShopId(User user) {
        return user.getCurrentShop() != null
                ? user.getCurrentShop()
                .getId()
                : user.getId();
    }

    public Integer getShopId(Integer userId) {
        User user = getUser(userId);
        return user.getCurrentShop() != null
                ? user.getCurrentShop()
                .getId()
                : user.getId();
    }

    public User getUser(int userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }
}
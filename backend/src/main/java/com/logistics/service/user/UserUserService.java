package com.logistics.service.user;

import org.springframework.stereotype.Service;

import com.logistics.entity.User;
import com.logistics.repository.UserRepository;
import com.logistics.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserUserService {

    private final UserRepository repository; 

    public User findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    public ApiResponse<Boolean> checkLocked(Integer id) {
        Integer shopId = getShopId(id);

        User user = getUser(shopId);
        Boolean checked = user.getLocked();
        return new ApiResponse<>(true, "Kiểm tra tài khoản bị khóa thành công", checked);
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
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }
}
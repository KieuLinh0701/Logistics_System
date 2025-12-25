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
        User user = findById(id);
        Boolean checked = user.getLocked();
        return new ApiResponse<>(true, "Kiểm tra tài khoản bị khóa thành công", checked);
    }

}
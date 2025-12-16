package com.logistics.service.user;

import org.springframework.stereotype.Service;

import com.logistics.entity.User;
import com.logistics.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserUserService {

    private final UserRepository repository;

    public User findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

}
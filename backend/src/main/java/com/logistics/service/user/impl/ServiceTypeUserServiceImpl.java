package com.logistics.service.user.impl;

import com.logistics.entity.ServiceType;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.service.user.ServiceTypeUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceTypeUserServiceImpl implements ServiceTypeUserService {

    private final ServiceTypeRepository repository;

    @Override
    public boolean serviceTypeExists(Integer id) {
        return repository.existsById(id);
    }

    @Override
    public Optional<ServiceType> findById(Integer id) {
        return repository.findById(id);
    }

}
package com.logistics.service.user;

import com.logistics.entity.ServiceType;
import com.logistics.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceTypeUserService {

    private final ServiceTypeRepository repository;

    public boolean serviceTypeExists(Integer id) {
        return repository.existsById(id);
    }

    public Optional<ServiceType> findById(Integer id) {
        return repository.findById(id);
    }

}

package com.logistics.service.user;

import com.logistics.entity.ServiceType;

import java.util.Optional;

public interface ServiceTypeUserService {
    boolean serviceTypeExists(Integer id);
    Optional<ServiceType> findById(Integer id);
}
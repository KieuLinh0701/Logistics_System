package com.logistics.repository;

import com.logistics.entity.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Integer>, JpaSpecificationExecutor<Office> {
    Optional<Office> findByCode(String code);

    Optional<Office> findByName(String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    boolean existsByPhoneNumber(String phoneNumber);

    Page<Office> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);

    @Query("SELECT o.cityCode FROM Office o WHERE o.id = :id")
    Integer findCityCodeById(int id);
}
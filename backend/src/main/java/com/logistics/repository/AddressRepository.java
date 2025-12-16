package com.logistics.repository;

import com.logistics.entity.Address;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUserIdOrderByCreatedAtDesc(int userId);

    Optional<Address> findByIdAndUserId(int id, int userId);

    boolean existsByIdAndUser_Id(Integer id, Integer userId);

    long countByUserId(int userId);

    @Query("SELECT a.cityCode FROM Address a WHERE a.id = :id")
    Integer findCityCodeById(int id);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND (:exceptId = -1 OR a.id <> :exceptId)")
    void clearDefaultExcept(int userId, int exceptId);

    @Modifying
    @Query("update Address a set a.isDefault = false where a.user.id = :userId")
    void clearAllDefaultForUser(@Param("userId") int userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Integer userId);

    List<Address> findByUserIdInAndIsDefaultTrue(List<Integer> userIds);

}

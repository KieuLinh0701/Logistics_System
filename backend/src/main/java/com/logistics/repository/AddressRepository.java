package com.logistics.repository;

import com.logistics.entity.Address;
import com.logistics.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository
        extends JpaRepository<Address, Integer>,
        JpaSpecificationExecutor<Address> {
    List<Address> findByUserIdAndTypeOrderByCreatedAtDesc(int userId, AddressType type);

    Optional<Address> findByIdAndUserIdAndType(int id, int userId, AddressType type);

    boolean existsByIdAndUser_IdAndType(Integer id, Integer userId, AddressType type);

    long countByUserIdAndType(int userId, AddressType type);

    @Query("SELECT a.cityCode FROM Address a WHERE a.id = :id")
    Integer findCityCodeById(int id);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND type = :type AND (:exceptId = "
            + "-1 OR a.id <> :exceptId)")
    void clearDefaultExcept(int userId, int exceptId, AddressType type);

    @Modifying
    @Query("update Address a set a.isDefault = false where a.user.id = :userId and a.type = :type")
    void clearAllDefaultForUser(
            @Param("userId") int userId,
            @Param("type") AddressType type);

    Optional<Address> findByUserIdAndIsDefaultTrue(Integer userId);

    List<Address> findByUserIdInAndIsDefaultTrue(List<Integer> userIds);

    Optional<Address> findByPhoneNumberAndFullAddressAndUserIdAndType(
            String phoneNumber,
            String fullAddress,
            int userId,
            AddressType type);

    boolean existsByUserIdAndPhoneNumberAndFullAddressAndType(
            Integer userId,
            String phoneNumber,
            String fullAddress,
            AddressType type);

    boolean existsByUserIdAndPhoneNumberAndFullAddressAndTypeAndIdNot(
            Integer userId,
            String phoneNumber,
            String fullAddress,
            AddressType type,
            Integer addressId);

    List<Address> findByUserIdAndPhoneNumberAndType(
            Integer userId, String phone, AddressType type);
}

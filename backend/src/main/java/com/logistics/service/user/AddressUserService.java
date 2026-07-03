package com.logistics.service.user;

import com.logistics.dto.AddressDto;
import com.logistics.entity.Address;
import com.logistics.enums.AddressType;
import com.logistics.request.user.address.AddressUserRequest;

import java.util.List;
import java.util.Optional;

public interface AddressUserService {
    List<AddressDto> list(int userId);
    AddressDto create(int userId, AddressUserRequest request);
    AddressDto update(int userId, int id, AddressUserRequest request);
    void delete(int userId, int id);
    void setDefault(int userId, int id);

    boolean checkAddressBelongsToUser(Integer senderAddressId, Integer userId);
    Optional<Address> findByIdAndUserIdAndType(Integer addressId, Integer userId, AddressType type);
    Optional<Address> findByPhoneNumberAndFullAddressAndUserIdAndType(String phoneNumber, String fullAddress, Integer userId, AddressType type);
    Address save(Address address);
    void delete(Address address);
}
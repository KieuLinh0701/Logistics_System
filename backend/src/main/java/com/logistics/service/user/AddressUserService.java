package com.logistics.service.user;

import com.logistics.dto.AddressDto;
import com.logistics.enums.AddressType;
import com.logistics.entity.Address;
import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AddressErrorCode;
import com.logistics.mapper.AddressMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.request.user.address.AddressUserRequest;
import com.logistics.utils.AddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressUserService {

    private final AddressRepository addressRepository;
    private final UserUserService userService;

    public List<AddressDto> list(int userId) {
            Integer shopId = userService.getShopId(userId);

            List<Address> addresses = addressRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    shopId,
                    AddressType.SENDER);
            return addresses.stream()
                    .map(AddressMapper::toDto)
                    .toList();
    }

    @Transactional
    public AddressDto create(int userId, AddressUserRequest request) {

            Integer shopId = userService.getShopId(userId);
            User user = userService.getUser(shopId);

            long count = addressRepository.countByUserIdAndType(
                    shopId,
                    AddressType.SENDER);
            if (count >= 10) {
                throw new AppException(AddressErrorCode.ADDRESS_MAX_LIMIT_REACHED);
            }

            String fullAddress = AddressUtils.buildFullAddress(
                    request.getDetail(),
                    request.getWardName(),
                    request.getCityName());

            Address address = new Address();
            address.setUser(user);
            address.setCityCode(request.getCityCode());
            address.setWardCode(request.getWardCode());
            address.setDetail(request.getDetail());
            address.setName(request.getName());
            address.setPhoneNumber(request.getPhoneNumber());
            address.setType(AddressType.SENDER);
            address.setIsDefault(request.isDefault());
            address.setFullAddress(fullAddress);
            address.setCityName(request.getCityName());
            address.setWardName(request.getWardName());
            address.setLatitude(request.getLatitude());
            address.setLongitude(request.getLongitude());

            if (request.isDefault() || count == 0) {
                addressRepository.clearAllDefaultForUser(
                        shopId,
                        AddressType.SENDER);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            save(address);
            return AddressMapper.toDto(address);
    }

    @Transactional
    public AddressDto update(int userId, int id, AddressUserRequest request) {
            Integer shopId = userService.getShopId(userId);

            Address address = addressRepository.findByIdAndUserIdAndType(id, shopId, AddressType.SENDER)
                    .orElseThrow(() -> new AppException(AddressErrorCode.ADDRESS_NOT_FOUND));

            String fullAddress = AddressUtils.buildFullAddress(
                    request.getDetail(),
                    request.getWardName(),
                    request.getCityName());

            address.setCityCode(request.getCityCode());
            address.setWardCode(request.getWardCode());
            address.setCityName(request.getCityName());
            address.setWardName(request.getWardName());
            address.setFullAddress(fullAddress);
            address.setLongitude(request.getLongitude());
            address.setLatitude(request.getLatitude());
            address.setDetail(request.getDetail());
            address.setName(request.getName());
            address.setPhoneNumber(request.getPhoneNumber());

            if (request.isDefault()) {
                addressRepository.clearDefaultExcept(shopId, id, AddressType.SENDER);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            save(address);
            return AddressMapper.toDto(address);
    }

    public void delete(int userId, int id) {
            Integer shopId = userService.getShopId(userId);

            Address address = addressRepository.findByIdAndUserIdAndType(id, shopId, AddressType.SENDER)
                    .orElseThrow(() -> new AppException(AddressErrorCode.ADDRESS_NOT_FOUND));

            if (Boolean.TRUE.equals(address.getIsDefault())) {
                throw new AppException(AddressErrorCode.ADDRESS_IS_DEFAULT);
            }

            delete(address);
    }

    @Transactional
    public void setDefault(int userId, int id) {
            Integer shopId = userService.getShopId(userId);

            Address address = addressRepository.findByIdAndUserIdAndType(id, shopId, AddressType.SENDER)
                    .orElseThrow(() -> new AppException(AddressErrorCode.ADDRESS_NOT_FOUND));

            addressRepository.clearDefaultExcept(shopId, id, AddressType.SENDER);
            address.setIsDefault(true);
            save(address);
    }

    public boolean checkAddressBelongsToUser(Integer senderAddressId, Integer userId) {
        return addressRepository.existsByIdAndUser_IdAndType(
                senderAddressId,
                userId,
                AddressType.SENDER);
    }

    public Optional<Address> findByIdAndUserIdAndType(
            Integer addressId,
            Integer userId,
            AddressType type) {
        return addressRepository.findByIdAndUserIdAndType(addressId, userId, type);
    }

    public Optional<Address> findByPhoneNumberAndFullAddressAndUserIdAndType(
            String phoneNumber,
            String fullAddress,
            Integer userId,
            AddressType type) {
        return addressRepository.findByPhoneNumberAndFullAddressAndUserIdAndType(phoneNumber, fullAddress, userId, type);
    }

    public Address save(Address address) {
        if (address == null) {
            return null;
        }
        return addressRepository.save(address);
    }

    public void delete(Address address) {
        if (address == null) {
            return; 
        }
        addressRepository.delete(address);
    }
}
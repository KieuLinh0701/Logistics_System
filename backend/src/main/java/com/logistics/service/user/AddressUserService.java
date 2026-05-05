package com.logistics.service.user;

import com.logistics.dto.AddressDto;
import com.logistics.enums.AddressType;
import com.logistics.entity.Address;
import com.logistics.entity.User;
import com.logistics.mapper.AddressMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.address.AddressUserRequest;
import com.logistics.response.ApiResponse;
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
    private final UserRepository userRepository;

    public ApiResponse<List<AddressDto>> list(int userId) {
        try {
            List<Address> addresses = addressRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    userId,
                    AddressType.SENDER);
            List<AddressDto> data = addresses.stream()
                    .map(AddressMapper::toDto)
                    .toList();
            return new ApiResponse<>(true, "Lấy danh sách thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<AddressDto> create(int userId, AddressUserRequest request) {
        try {
            validateForm(request);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            long count = addressRepository.countByUserIdAndType(
                    userId,
                    AddressType.SENDER);
            if (count >= 10) {
                return new ApiResponse<>(false, "Chỉ được tạo tối đa 10 địa chỉ", null);
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
                        userId,
                        AddressType.SENDER);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            save(address);
            return new ApiResponse<>(true, "Thêm địa chỉ thành công", AddressMapper.toDto(address));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<AddressDto> update(int userId, int id, AddressUserRequest request) {
        try {
            validateForm(request);

            Address address = addressRepository.findByIdAndUserIdAndType(id, userId, AddressType.SENDER)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

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
                addressRepository.clearDefaultExcept(userId, id, AddressType.SENDER);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            save(address);
            return new ApiResponse<>(true, "Cập nhật địa chỉ thành công", AddressMapper.toDto(address));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> delete(int userId, int id) {
        try {
            Address address = addressRepository.findByIdAndUserIdAndType(id, userId, AddressType.SENDER)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            if (Boolean.TRUE.equals(address.getIsDefault())) {
                return new ApiResponse<>(false, "Vui lòng chọn địa chỉ mặc định khác trước khi xóa", false);
            }

            delete(address);
            return new ApiResponse<>(true, "Xóa địa chỉ thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse<Boolean> setDefault(int userId, int id) {
        try {
            Address address = addressRepository.findByIdAndUserIdAndType(id, userId, AddressType.SENDER)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            addressRepository.clearDefaultExcept(userId, id, AddressType.SENDER);
            address.setIsDefault(true);
            save(address);

            return new ApiResponse<>(true, "Đặt địa chỉ mặc định thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    // Validate dữ liệu nhập
    private void validateForm(AddressUserRequest request) {
        StringBuilder missing = new StringBuilder();
        if (request.getDetail() == null || request.getDetail().isBlank())
            missing.append("Chi tiết địa chỉ, ");
        if (request.getCityCode() <= 0)
            missing.append("Mã thành phố, ");
        if (request.getWardCode() <= 0)
            missing.append("Mã phường/xã, ");
        if (request.getLatitude() <= 0)
            missing.append("Vĩ độ ");
        if (request.getLongitude() <= 0)
            missing.append("Kinh độ, ");
        if (request.getCityName() == null || request.getCityName().isBlank())
            missing.append("Tên thành phố, ");
        if (request.getWardName() == null || request.getWardName().isBlank())
            missing.append("Tên phường/xã, ");
        if (request.getName() == null || request.getName().isBlank())
            missing.append("Tên, ");
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())
            missing.append("Số điện thoại, ");

        if (missing.length() > 0) {
            // Bỏ dấu phẩy cuối
            throw new RuntimeException("Thiếu hoặc không hợp lệ: " + missing.substring(0, missing.length() - 2));
        }
    }

    public boolean checkAddressBelongsToUser(Integer senderAddressId, Integer userId) {
        return addressRepository.existsByIdAndUser_IdAndType(
                senderAddressId,
                userId,
                AddressType.SENDER);
    }

    public Optional<Address> findByIdAndUserIdAndType(
            Integer userId,
            Integer addressId,
            AddressType type) {
        return addressRepository.findByIdAndUserIdAndType(addressId, userId, type);
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
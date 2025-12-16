package com.logistics.service.user;

import com.logistics.dto.AddressDto;
import com.logistics.entity.Address;
import com.logistics.entity.User;
import com.logistics.mapper.AddressMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.address.AddressUserRequest;
import com.logistics.response.ApiResponse;
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
            List<Address> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(userId);
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
            System.out.println("IsDefault" + request.isDefault());
            long count = addressRepository.countByUserId(userId);
            if (count >= 20) {
                return new ApiResponse<>(false, "Chỉ được tạo tối đa 10 địa chỉ", null);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = new Address();
            address.setUser(user);
            address.setCityCode(request.getCityCode());
            address.setWardCode(request.getWardCode());
            address.setDetail(request.getDetail());
            address.setName(request.getName());
            address.setPhoneNumber(request.getPhoneNumber());
            address.setIsDefault(request.isDefault());

            if (request.isDefault() || count == 0) {
                addressRepository.clearAllDefaultForUser(userId);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            addressRepository.save(address);
            return new ApiResponse<>(true, "Thêm địa chỉ thành công", AddressMapper.toDto(address));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<AddressDto> update(int userId, int id, AddressUserRequest request) {
        try {
            validateForm(request);

            Address address = addressRepository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            address.setCityCode(request.getCityCode());
            address.setWardCode(request.getWardCode());
            address.setDetail(request.getDetail());
            address.setName(request.getName());
            address.setPhoneNumber(request.getPhoneNumber());

            if (request.isDefault()) {
                addressRepository.clearDefaultExcept(userId, id);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }

            addressRepository.save(address);
            return new ApiResponse<>(true, "Cập nhật địa chỉ thành công", AddressMapper.toDto(address));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> delete(int userId, int id) {
        try {
            Address address = addressRepository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            if (Boolean.TRUE.equals(address.getIsDefault())) {
                return new ApiResponse<>(false, "Vui lòng chọn địa chỉ mặc định khác trước khi xóa", false);
            }

            addressRepository.delete(address);
            return new ApiResponse<>(true, "Xóa địa chỉ thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    @Transactional
    public ApiResponse<Boolean> setDefault(int userId, int id) {
        try {
            Address address = addressRepository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            addressRepository.clearDefaultExcept(userId, id);
            address.setIsDefault(true);
            addressRepository.save(address);

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
        return addressRepository.existsByIdAndUser_Id(senderAddressId, userId);
    }

    public Optional<Address> findById(Integer id) {
        return addressRepository.findById(id);
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
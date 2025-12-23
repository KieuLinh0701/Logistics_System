package com.logistics.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.request.admin.CreateVehicleRequest;
import com.logistics.request.admin.UpdateVehicleRequest;
import com.logistics.entity.Office;
import com.logistics.entity.Vehicle;
import com.logistics.enums.VehicleStatus;
import com.logistics.enums.VehicleType;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;

@Service
public class VehicleAdminService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OfficeRepository officeRepository;

    public ApiResponse<Map<String, Object>> listVehicles(int page, int limit, String search) {
        try {
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Vehicle> vehiclePage = vehicleRepository.findAll(pageable);

            List<Map<String, Object>> vehicles = vehiclePage.getContent().stream()
                    .map(this::mapVehicle)
                    .collect(Collectors.toList());

            Pagination pagination = new Pagination(
                    (int) vehiclePage.getTotalElements(),
                    page,
                    limit,
                    vehiclePage.getTotalPages());

            Map<String, Object> result = new HashMap<>();
            result.put("data", vehicles);
            result.put("pagination", pagination);

            return new ApiResponse<>(true, "Lấy danh sách phương tiện thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createVehicle(CreateVehicleRequest request) {
        try {
            if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
                return new ApiResponse<>(false, "Biển số xe đã tồn tại", null);
            }

            Office office = officeRepository.findById(request.getOfficeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bưu cục"));

            Vehicle vehicle = new Vehicle();
            vehicle.setLicensePlate(request.getLicensePlate());
            vehicle.setType(VehicleType.valueOf(request.getType()));
            vehicle.setCapacity(request.getCapacity());
            vehicle.setStatus(VehicleStatus.valueOf(request.getStatus()));
            vehicle.setDescription(request.getDescription());
            vehicle.setOffice(office);
            vehicle = vehicleRepository.save(vehicle);

            return new ApiResponse<>(true, "Tạo phương tiện thành công", mapVehicle(vehicle));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateVehicle(Integer vehicleId, UpdateVehicleRequest request) {
        try {
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));

            if (request.getType() != null)
                vehicle.setType(VehicleType.valueOf(request.getType()));
            if (request.getCapacity() != null)
                vehicle.setCapacity(request.getCapacity());
            if (request.getStatus() != null)
                vehicle.setStatus(VehicleStatus.valueOf(request.getStatus()));
            if (request.getDescription() != null)
                vehicle.setDescription(request.getDescription());
            if (request.getOfficeId() != null) {
                Office office = officeRepository.findById(request.getOfficeId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy bưu cục"));
                vehicle.setOffice(office);
            }
            vehicle = vehicleRepository.save(vehicle);

            return new ApiResponse<>(true, "Cập nhật phương tiện thành công", mapVehicle(vehicle));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteVehicle(Integer vehicleId) {
        try {
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));

            vehicleRepository.delete(vehicle);
            return new ApiResponse<>(true, "Xóa phương tiện thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    private Map<String, Object> mapVehicle(Vehicle vehicle) {
        Map<String, Object> vehicleMap = new HashMap<>();
        vehicleMap.put("id", vehicle.getId());
        vehicleMap.put("licensePlate", vehicle.getLicensePlate());
        vehicleMap.put("type", vehicle.getType().name());
        vehicleMap.put("capacity", vehicle.getCapacity());
        vehicleMap.put("status", vehicle.getStatus().name());
        vehicleMap.put("description", vehicle.getDescription());
        vehicleMap.put("officeId", vehicle.getOffice() != null ? vehicle.getOffice().getId() : null);

        if (vehicle.getOffice() != null) {
            Map<String, Object> officeMap = new HashMap<>();
            officeMap.put("id", vehicle.getOffice().getId());
            officeMap.put("name", vehicle.getOffice().getName());
            vehicleMap.put("office", officeMap);
        } else {
            vehicleMap.put("office", null);
        }

        vehicleMap.put("createdAt", vehicle.getCreatedAt());
        vehicleMap.put("updatedAt", vehicle.getUpdatedAt());
        return vehicleMap;
    }
}



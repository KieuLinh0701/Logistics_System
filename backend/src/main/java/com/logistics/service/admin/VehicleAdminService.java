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
import org.springframework.data.jpa.domain.Specification;
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
    
    @Autowired
    private com.logistics.repository.VehicleTrackingRepository vehicleTrackingRepository;

    public ApiResponse<Map<String, Object>> listVehicles(int page, int limit, String search, String type, String status) {
        try {
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Specification<Vehicle> spec = Specification.where(null);

            if (search != null && !search.trim().isEmpty()) {
                String q = "%" + search.trim().toLowerCase() + "%";
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("licensePlate")), q),
                        cb.like(cb.lower(root.get("description")), q),
                        cb.like(cb.lower(root.get("office").get("name")), q)
                ));
            }

            if (type != null && !type.trim().isEmpty()) {
                try {
                    VehicleType vt = VehicleType.valueOf(type);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), vt));
                } catch (IllegalArgumentException ex) {
                    // ignore invalid type filter
                }
            }

            if (status != null && !status.trim().isEmpty()) {
                try {
                    VehicleStatus vs = VehicleStatus.valueOf(status);
                    spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), vs));
                } catch (IllegalArgumentException ex) {
                    // ignore invalid status filter
                }
            }

            Page<Vehicle> vehiclePage = vehicleRepository.findAll(spec, pageable);

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

        vehicleMap.put("gpsDeviceId", vehicle.getGpsDeviceId());
        vehicleMap.put("lastMaintenanceAt", vehicle.getLastMaintenanceAt());
        vehicleMap.put("nextMaintenanceDue", vehicle.getNextMaintenanceDue());
        vehicleMap.put("latitude", vehicle.getLatitude());
        vehicleMap.put("longitude", vehicle.getLongitude());

        vehicleMap.put("createdAt", vehicle.getCreatedAt());
        vehicleMap.put("updatedAt", vehicle.getUpdatedAt());
        return vehicleMap;
    }

    public ApiResponse<Map<String, Object>> getVehicleById(Integer vehicleId) {
        try {
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));
            return new ApiResponse<>(true, "Lấy thông tin phương tiện thành công", mapVehicle(vehicle));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getVehicleTrackings(Integer vehicleId) {
        try {
            java.util.List<com.logistics.entity.VehicleTracking> trackings = vehicleTrackingRepository.findByVehicleId(vehicleId);
            java.util.List<Map<String, Object>> list = trackings.stream().map(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", t.getId());
                m.put("latitude", t.getLatitude());
                m.put("longitude", t.getLongitude());
                m.put("speed", t.getSpeed());
                m.put("recordedAt", t.getRecordedAt());
                m.put("shipmentId", t.getShipment() != null ? t.getShipment().getId() : null);
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("data", list);
            return new ApiResponse<>(true, "Lấy lịch sử hành trình thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }
}



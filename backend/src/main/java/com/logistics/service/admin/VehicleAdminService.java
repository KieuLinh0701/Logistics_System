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
import com.logistics.exception.AppException;
import com.logistics.exception.enums.VehicleErrorCode;
import com.logistics.exception.enums.OfficeErrorCode;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.response.Pagination;
import com.logistics.repository.VehicleTrackingRepository;

@Service
public class VehicleAdminService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private VehicleTrackingRepository vehicleTrackingRepository;

    public Map<String, Object> listVehicles(int page, int limit, String search, String type, String status) {
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

        return result;
    }

    @Transactional
    public void createVehicle(CreateVehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new AppException(VehicleErrorCode.VEHICLE_LICENSE_PLATE_EXISTED);
        }

        Office office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setType(VehicleType.valueOf(request.getType()));
        vehicle.setCapacity(request.getCapacity());
        vehicle.setStatus(VehicleStatus.valueOf(request.getStatus()));
        vehicle.setDescription(request.getDescription());
        vehicle.setOffice(office);
        vehicle = vehicleRepository.save(vehicle);
    }

    @Transactional
    public void updateVehicle(Integer vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(VehicleErrorCode.VEHICLE_NOT_FOUND));

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
                    .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));
            vehicle.setOffice(office);
        }
        vehicle = vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Integer vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(VehicleErrorCode.VEHICLE_NOT_FOUND));

        vehicleRepository.delete(vehicle);
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

    public Map<String, Object> getVehicleById(Integer vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(VehicleErrorCode.VEHICLE_NOT_FOUND));
        return mapVehicle(vehicle);
    }

    public Map<String, Object> getVehicleTrackings(Integer vehicleId) {
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
        return result;
    }
}

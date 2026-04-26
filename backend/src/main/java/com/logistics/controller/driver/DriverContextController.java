package com.logistics.controller.driver;

import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Vehicle;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.response.ApiResponse;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverContextController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContext() {
        try {
            Integer userId = SecurityUtils.getAuthenticatedUserId();
            List<Employee> employees = employeeRepository.findByUserId(userId);
            if (employees == null || employees.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(false, "Không tìm thấy thông tin nhân viên (driver)", null));
            }

            Employee employee = employees.get(0);
            Office office = employee.getOffice();

            Map<String, Object> data = new HashMap<>();
            if (office != null) {
                Map<String, Object> officeMap = new HashMap<>();
                officeMap.put("id", office.getId());
                officeMap.put("name", office.getName() != null ? office.getName() : "");
                officeMap.put("address", office.getDetail());
                data.put("office", officeMap);

                List<Vehicle> vehicles = vehicleRepository.findByOfficeId(office.getId());
                data.put("vehicles", vehicles.stream().map(v -> Map.of(
                        "id", v.getId(),
                        "licensePlate", v.getLicensePlate(),
                        "type", v.getType(),
                        "status", v.getStatus()
                )));
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "Thành công", data));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}

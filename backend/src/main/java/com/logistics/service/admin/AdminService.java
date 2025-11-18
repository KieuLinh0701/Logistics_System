package com.logistics.service.admin;

import com.logistics.dto.admin.*;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.repository.*;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;
import com.logistics.utils.PasswordUtils;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private ShippingRateRepository shippingRateRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // USER MANAGEMENT
    public ApiResponse<Map<String, Object>> listUsers(int page, int limit, String search) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Account> accountPage;

            if (search != null && !search.trim().isEmpty()) {
                // Search by email, firstName, lastName, phoneNumber
                accountPage = accountRepository.findByEmailContainingOrUserFirstNameContainingOrUserLastNameContainingOrUserPhoneNumberContaining(
                    search, search, search, search, pageable);
            } else {
                accountPage = accountRepository.findAll(pageable);
            }

            List<Map<String, Object>> users = accountPage.getContent().stream().map(account -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", account.getUser().getId());
                userMap.put("email", account.getEmail());
                userMap.put("firstName", account.getUser().getFirstName());
                userMap.put("lastName", account.getUser().getLastName());
                userMap.put("phoneNumber", account.getUser().getPhoneNumber());
                userMap.put("role", account.getRole() != null ? account.getRole().getName() : null);
                userMap.put("isActive", account.getIsActive());
                userMap.put("isVerified", account.getIsVerified());
                userMap.put("createdAt", account.getCreatedAt());
                return userMap;
            }).collect(Collectors.toList());

            Pagination pagination = new Pagination(
                (int) accountPage.getTotalElements(),
                page,
                limit,
                accountPage.getTotalPages()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("data", users);
            result.put("pagination", pagination);

            return new ApiResponse<>(true, "Lấy danh sách người dùng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getUserById(Integer userId) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("email", account.getEmail());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("phoneNumber", user.getPhoneNumber());
            userMap.put("role", account.getRole() != null ? account.getRole().getName() : null);
            userMap.put("roleId", account.getRole() != null ? account.getRole().getId() : null);
            userMap.put("isActive", account.getIsActive());
            userMap.put("isVerified", account.getIsVerified());
            userMap.put("images", user.getImages());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("updatedAt", user.getUpdatedAt());

            return new ApiResponse<>(true, "Lấy thông tin người dùng thành công", userMap);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createUser(CreateUserRequest request) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            // Validate
            if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
                return new ApiResponse<>(false, "Email đã tồn tại", null);
            }

            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                return new ApiResponse<>(false, "Số điện thoại đã tồn tại", null);
            }

            // Get role
            Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));

            // Create account
            Account account = new Account();
            account.setEmail(request.getEmail());
            account.setPassword(PasswordUtils.hashPassword(request.getPassword()));
            account.setRole(role);
            account.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            account.setIsVerified(false);
            account = accountRepository.save(account);

            // Create user
            User user = new User();
            user.setAccount(account);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhoneNumber(request.getPhoneNumber());
            user = userRepository.save(user);

            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("email", account.getEmail());
            result.put("firstName", user.getFirstName());
            result.put("lastName", user.getLastName());
            result.put("phoneNumber", user.getPhoneNumber());
            result.put("role", role.getName());

            return new ApiResponse<>(true, "Tạo người dùng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateUser(Integer userId, UpdateUserRequest request) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            // Update user fields
            if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
            if (request.getLastName() != null) user.setLastName(request.getLastName());
            if (request.getPhoneNumber() != null) {
                if (!user.getPhoneNumber().equals(request.getPhoneNumber()) 
                    && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                    return new ApiResponse<>(false, "Số điện thoại đã tồn tại", null);
                }
                user.setPhoneNumber(request.getPhoneNumber());
            }
            user = userRepository.save(user);

            // Update account fields
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                account.setPassword(PasswordUtils.hashPassword(request.getPassword()));
            }
            if (request.getRoleId() != null) {
                Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));
                account.setRole(role);
            }
            if (request.getIsActive() != null) {
                account.setIsActive(request.getIsActive());
            }
            account = accountRepository.save(account);

            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("email", account.getEmail());
            result.put("firstName", user.getFirstName());
            result.put("lastName", user.getLastName());
            result.put("phoneNumber", user.getPhoneNumber());
            result.put("role", account.getRole() != null ? account.getRole().getName() : null);

            return new ApiResponse<>(true, "Cập nhật người dùng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteUser(Integer userId) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Account account = accountRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            userRepository.delete(user);
            accountRepository.delete(account);

            return new ApiResponse<>(true, "Xóa người dùng thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    // OFFICE MANAGEMENT

    public ApiResponse<Map<String, Object>> listOffices(int page, int limit, String search) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Office> officePage;

            if (search != null && !search.trim().isEmpty()) {
                officePage = officeRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(search, search, pageable);
            } else {
                officePage = officeRepository.findAll(pageable);
            }

            List<Map<String, Object>> offices = officePage.getContent().stream().map(office -> {
                return mapOffice(office);
            }).collect(Collectors.toList());

            Pagination pagination = new Pagination(
                (int) officePage.getTotalElements(),
                page,
                limit,
                officePage.getTotalPages()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("data", offices);
            result.put("pagination", pagination);

            return new ApiResponse<>(true, "Lấy danh sách bưu cục thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getOfficeById(Integer officeId) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bưu cục"));

            return new ApiResponse<>(true, "Lấy thông tin bưu cục thành công", mapOffice(office));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createOffice(CreateOfficeRequest request) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            if (request.getCode() == null || request.getCode().isBlank()) {
                return new ApiResponse<>(false, "Mã bưu cục không được để trống", null);
            }

            if (officeRepository.existsByCode(request.getCode())) {
                return new ApiResponse<>(false, "Mã bưu cục đã tồn tại", null);
            }

            if (request.getPhoneNumber() != null && officeRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                return new ApiResponse<>(false, "Số điện thoại đã được sử dụng", null);
            }

            if (request.getWardCode() == null || request.getCityCode() == null || request.getDetailAddress() == null) {
                return new ApiResponse<>(false, "Thông tin địa chỉ không được để trống", null);
            }

            if (request.getLatitude() == null || request.getLongitude() == null) {
                return new ApiResponse<>(false, "Vĩ độ và kinh độ không được để trống", null);
            }

            Address address = new Address();
            address.setWardCode(request.getWardCode());
            address.setCityCode(request.getCityCode());
            address.setDetail(request.getDetailAddress());
            address = addressRepository.save(address);

            Office office = new Office();
            office.setCode(request.getCode());
            office.setPostalCode(request.getPostalCode());
            office.setName(request.getName());
            office.setLatitude(request.getLatitude());
            office.setLongitude(request.getLongitude());
            office.setEmail(request.getEmail());
            office.setPhoneNumber(request.getPhoneNumber());
            office.setOpeningTime(request.getOpeningTime() != null ? request.getOpeningTime() : LocalTime.of(7, 0));
            office.setClosingTime(request.getClosingTime() != null ? request.getClosingTime() : LocalTime.of(17, 0));
            if (request.getType() != null) {
                office.setType(OfficeType.valueOf(request.getType().toUpperCase()));
            }
            if (request.getStatus() != null) {
                office.setStatus(OfficeStatus.valueOf(request.getStatus().toUpperCase()));
            }
            office.setCapacity(request.getCapacity());
            office.setNotes(request.getNotes());
            // office.setAddress(address);

            office = officeRepository.save(office);
            return new ApiResponse<>(true, "Tạo bưu cục thành công", mapOffice(office));
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(false, "Giá trị type/status không hợp lệ", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateOffice(Integer officeId, UpdateOfficeRequest request) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bưu cục"));

            if (request.getCode() != null && !request.getCode().equalsIgnoreCase(office.getCode())
                    && officeRepository.existsByCode(request.getCode())) {
                return new ApiResponse<>(false, "Mã bưu cục đã tồn tại", null);
            }

            if (request.getPhoneNumber() != null && !request.getPhoneNumber().equalsIgnoreCase(office.getPhoneNumber())
                    && officeRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                return new ApiResponse<>(false, "Số điện thoại đã tồn tại", null);
            }

            if (request.getCode() != null) office.setCode(request.getCode());
            if (request.getPostalCode() != null) office.setPostalCode(request.getPostalCode());
            if (request.getName() != null) office.setName(request.getName());
            if (request.getLatitude() != null) office.setLatitude(request.getLatitude());
            if (request.getLongitude() != null) office.setLongitude(request.getLongitude());
            if (request.getEmail() != null) office.setEmail(request.getEmail());
            if (request.getPhoneNumber() != null) office.setPhoneNumber(request.getPhoneNumber());
            if (request.getOpeningTime() != null) office.setOpeningTime(request.getOpeningTime());
            if (request.getClosingTime() != null) office.setClosingTime(request.getClosingTime());
            if (request.getType() != null) office.setType(OfficeType.valueOf(request.getType().toUpperCase()));
            if (request.getStatus() != null) office.setStatus(OfficeStatus.valueOf(request.getStatus().toUpperCase()));
            if (request.getCapacity() != null) office.setCapacity(request.getCapacity());
            if (request.getNotes() != null) office.setNotes(request.getNotes());

            if (request.getWardCode() != null || request.getCityCode() != null || request.getDetailAddress() != null) {
                // Address address = office.getAddress() != null ? office.getAddress() : new Address();
                // if (request.getWardCode() != null) address.setWardCode(request.getWardCode());
                // if (request.getCityCode() != null) address.setCityCode(request.getCityCode());
                // if (request.getDetailAddress() != null) address.setDetail(request.getDetailAddress());
                // address = addressRepository.save(address);
                // office.setAddress(address);
            }

            office = officeRepository.save(office);
            return new ApiResponse<>(true, "Cập nhật bưu cục thành công", mapOffice(office));
        } catch (IllegalArgumentException ex) {
            return new ApiResponse<>(false, "Giá trị type/status không hợp lệ", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteOffice(Integer officeId) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Office office = officeRepository.findById(officeId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bưu cục"));

            officeRepository.delete(office);
            return new ApiResponse<>(true, "Xóa bưu cục thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }


    // VEHICLE MANAGEMENT

    public ApiResponse<Map<String, Object>> listVehicles(int page, int limit, String search) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Vehicle> vehiclePage = vehicleRepository.findAll(pageable);

            List<Map<String, Object>> vehicles = vehiclePage.getContent().stream().map(vehicle -> {
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
                return vehicleMap;
            }).collect(Collectors.toList());

            Pagination pagination = new Pagination(
                (int) vehiclePage.getTotalElements(),
                page,
                limit,
                vehiclePage.getTotalPages()
            );

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
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

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

            Map<String, Object> result = new HashMap<>();
            result.put("id", vehicle.getId());
            result.put("licensePlate", vehicle.getLicensePlate());
            result.put("type", vehicle.getType().name());
            result.put("capacity", vehicle.getCapacity());
            result.put("status", vehicle.getStatus().name());

            return new ApiResponse<>(true, "Tạo phương tiện thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateVehicle(Integer vehicleId, UpdateVehicleRequest request) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));

            if (request.getType() != null) vehicle.setType(VehicleType.valueOf(request.getType()));
            if (request.getCapacity() != null) vehicle.setCapacity(request.getCapacity());
            if (request.getStatus() != null) vehicle.setStatus(VehicleStatus.valueOf(request.getStatus()));
            if (request.getDescription() != null) vehicle.setDescription(request.getDescription());
            if (request.getOfficeId() != null) {
                Office office = officeRepository.findById(request.getOfficeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bưu cục"));
                vehicle.setOffice(office);
            }
            vehicle = vehicleRepository.save(vehicle);

            Map<String, Object> result = new HashMap<>();
            result.put("id", vehicle.getId());
            result.put("licensePlate", vehicle.getLicensePlate());
            result.put("type", vehicle.getType().name());
            result.put("capacity", vehicle.getCapacity());
            result.put("status", vehicle.getStatus().name());

            return new ApiResponse<>(true, "Cập nhật phương tiện thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteVehicle(Integer vehicleId) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương tiện"));

            vehicleRepository.delete(vehicle);
            return new ApiResponse<>(true, "Xóa phương tiện thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    // ORDER MANAGEMENT 

    public ApiResponse<Map<String, Object>> listOrders(int page, int limit, String search, String status) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Order> orderPage;

            if (status != null && !status.trim().isEmpty()) {
                orderPage = orderRepository.findByStatus(OrderStatus.valueOf(status), pageable);
            } else {
                orderPage = orderRepository.findAll(pageable);
            }

            List<Map<String, Object>> orders = orderPage.getContent().stream().map(order -> {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", order.getId());
                orderMap.put("trackingNumber", order.getTrackingNumber());
                orderMap.put("senderName", order.getSenderName());
                orderMap.put("recipientName", order.getRecipientName());
                orderMap.put("status", order.getStatus().name());
                orderMap.put("totalFee", order.getTotalFee());
                orderMap.put("createdAt", order.getCreatedAt());
                return orderMap;
            }).collect(Collectors.toList());

            Pagination pagination = new Pagination(
                (int) orderPage.getTotalElements(),
                page,
                limit,
                orderPage.getTotalPages()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("data", orders);
            result.put("pagination", pagination);

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateOrderStatus(Integer orderId, UpdateOrderStatusRequest request) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            order.setStatus(OrderStatus.valueOf(request.getStatus()));
            order = orderRepository.save(order);

            Map<String, Object> result = new HashMap<>();
            result.put("id", order.getId());
            result.put("trackingNumber", order.getTrackingNumber());
            result.put("status", order.getStatus().name());

            return new ApiResponse<>(true, "Cập nhật trạng thái đơn hàng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteOrder(Integer orderId) {
        try {
            if (!SecurityUtils.hasRole("admin")) {
                return new ApiResponse<>(false, "Không có quyền truy cập", null);
            }

            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            orderRepository.delete(order);
            return new ApiResponse<>(true, "Xóa đơn hàng thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    private Map<String, Object> mapOffice(Office office) {
        Map<String, Object> officeMap = new HashMap<>();
        officeMap.put("id", office.getId());
        officeMap.put("code", office.getCode());
        officeMap.put("postalCode", office.getPostalCode());
        officeMap.put("name", office.getName());
        officeMap.put("email", office.getEmail());
        officeMap.put("phoneNumber", office.getPhoneNumber());
        officeMap.put("type", office.getType() != null ? office.getType().name() : null);
        officeMap.put("status", office.getStatus() != null ? office.getStatus().name() : null);
        officeMap.put("latitude", office.getLatitude());
        officeMap.put("longitude", office.getLongitude());
        officeMap.put("openingTime", office.getOpeningTime());
        officeMap.put("closingTime", office.getClosingTime());
        officeMap.put("capacity", office.getCapacity());
        officeMap.put("notes", office.getNotes());
        officeMap.put("createdAt", office.getCreatedAt());

        // if (office.getAddress() != null) {
        //     Map<String, Object> addressMap = new HashMap<>();
        //     addressMap.put("id", office.getAddress().getId());
        //     addressMap.put("wardCode", office.getAddress().getWardCode());
        //     addressMap.put("cityCode", office.getAddress().getCityCode());
        //     addressMap.put("detail", office.getAddress().getDetail());
        //     officeMap.put("address", addressMap);
        // } else {
        //     officeMap.put("address", null);
        // }

        return officeMap;
    }
}


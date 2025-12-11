package com.logistics.service.manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.ProductDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.entity.Address;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Product;
import com.logistics.entity.User;
import com.logistics.enums.EmployeeStatus;
import com.logistics.mapper.EmployeeMapper;
import com.logistics.mapper.ProductMapper;
import com.logistics.mapper.ShippingRequestMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.EmployeeRepository;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.request.user.product.UserProductSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.EmployeeSpecification;
import com.logistics.specification.ProductSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeManagerService {

    private final EmployeeRepository employeeRepository;

    private final AddressRepository addressRepository;

    public Office getOfficeByUserId(Integer userId) {
        
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là nhân viên"));

        if (employee.getStatus() == EmployeeStatus.LEAVE) {
            throw new RuntimeException("Bạn đã nghỉ, không thể thực hiện thao tác");
        }

        Office office = employee.getOffice();

        if (office == null) {
            throw new RuntimeException("Bạn không thuộc bưu cục nào");
        }
        if (!office.getManager().getId().equals(employee.getId())) {
            throw new RuntimeException("Bạn không phải quản lý bưu cục, không thể thực hiện thao tác");
        }

        return office;
    }

    public User geEmployeeById(Integer userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là nhân viên"));

        if (employee.getStatus() == EmployeeStatus.LEAVE) {
            throw new RuntimeException("Bạn đã nghỉ, không thể thực hiện thao tác");
        }

        return employee.getUser();
    }

    public ApiResponse<ListResponse<ManagerEmployeeListDto>> list(int userId, ManagerEmployeeSearchRequest request) {
        try {
            Office office = getOfficeByUserId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String sort = request.getSort();
            String status = request.getStatus();
            String role = request.getRole();
            String shift = request.getShift();

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Specification<Employee> spec = EmployeeSpecification.unrestrictedEmployee()
                    .and(EmployeeSpecification.officeId(office.getId()))
                    .and(EmployeeSpecification.search(search))
                    .and(EmployeeSpecification.status(status))
                    .and(EmployeeSpecification.role(role, true)) 
                    .and(EmployeeSpecification.shift(shift))
                    .and(EmployeeSpecification.hireDateBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("hireDate").descending();
                case "oldest" -> Sort.by("hireDate").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<Employee> pageData = employeeRepository.findAll(spec, pageable);

             List<Integer> userIds = pageData.getContent()
                    .stream()
                    .map(item -> item.getUser() != null ? item.getUser().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Map<Integer, Address> addressMap = addressRepository
                    .findByUserIdInAndIsDefaultTrue(userIds)
                    .stream()
                    .collect(Collectors.toMap(
                            a -> a.getUser().getId(),
                            a -> a));

            List<ManagerEmployeeListDto> list = pageData.getContent()
                    .stream()
                    .map(item -> {
                        Integer uid = item.getUser() != null
                                ? item.getUser().getId()
                                : null;

                        Address address = (uid != null)
                                ? addressMap.getOrDefault(uid, null)
                                : null;

                        return EmployeeMapper
                                .toManagerEmployeeListDto(item, address);
                    })
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerEmployeeListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách nhân viên thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    // private void checkUserPermission(int userId, Product product) {
    // if (product.getUser() == null || !product.getUser().getId().equals(userId)) {
    // throw new RuntimeException("Không có quyền thực hiện thao tác này");
    // }
    // }

    // private void checkDuplicateName(int userId, String name) {
    // boolean exists = repository.existsByUserIdAndName(userId, name.trim());
    // if (exists) {
    // throw new RuntimeException("Sản phẩm với tên này đã tồn tại");
    // }
    // }

    // @SuppressWarnings("unchecked")
    // private String uploadImage(MultipartFile file) {
    // try {
    // Map<String, Object> result = cloudinary.uploader().upload(
    // file.getBytes(),
    // ObjectUtils.asMap("folder", "products", "resource_type", "image"));
    // return result.get("secure_url").toString();
    // } catch (Exception e) {
    // throw new RuntimeException("Upload ảnh thất bại: " + e.getMessage());
    // }
    // }

    // private ProductDto createProduct(int userId, UserProductForm request) {

    // if (request.getName() == null || request.getName().isBlank()) {
    // throw new RuntimeException("Tên sản phẩm không được để trống");
    // }
    // if (request.getPrice() == null || request.getPrice() < 0) {
    // throw new RuntimeException("Giá sản phẩm không được để trống hoặc âm");
    // }
    // if (request.getWeight() == null ||
    // request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
    // throw new RuntimeException("Trọng lượng sản phẩm phải lớn hơn 0");
    // }
    // if (request.getType() == null || request.getType().isBlank()) {
    // throw new RuntimeException("Loại sản phẩm không được để trống");
    // }

    // checkDuplicateName(userId, request.getName());

    // User user = userRepository.findById(userId)
    // .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

    // Product product = new Product();
    // product.setUser(user);
    // product.setName(request.getName());
    // product.setWeight(request.getWeight());
    // product.setPrice(request.getPrice());
    // product.setType(ProductType.valueOf(request.getType()));
    // product.setStatus(
    // request.getStatus() != null ? ProductStatus.valueOf(request.getStatus()) :
    // ProductStatus.ACTIVE);
    // product.setStock(request.getStock() != null ? request.getStock() : 0);

    // if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
    // product.setImage(uploadImage(request.getImageFile()));
    // }

    // product = repository.save(product);
    // product.setCode("PROD" + product.getId());
    // repository.save(product);

    // return ProductMapper.toDto(product);
    // }

    // public ApiResponse<ProductDto> create(int userId, UserProductForm request) {
    // try {
    // ProductDto dto = createProduct(userId, request);
    // return new ApiResponse<>(true, "Thêm sản phẩm thành công", dto);
    // } catch (Exception e) {
    // return new ApiResponse<>(false, e.getMessage(), null);
    // }
    // }

    // public ApiResponse<ProductDto> update(int userId, UserProductForm request) {
    // try {
    // Product product = repository.findById(request.getId())
    // .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

    // checkUserPermission(userId, product);

    // if (request.getName() != null &&
    // !request.getName().equals(product.getName())) {
    // checkDuplicateName(userId, request.getName());
    // product.setName(request.getName());
    // }

    // product.setWeight(request.getWeight() != null ? request.getWeight() :
    // product.getWeight());
    // product.setPrice(request.getPrice() != null ? request.getPrice() :
    // product.getPrice());
    // product.setType(request.getType() != null ?
    // ProductType.valueOf(request.getType()) : product.getType());
    // product.setStatus(
    // request.getStatus() != null ? ProductStatus.valueOf(request.getStatus()) :
    // product.getStatus());
    // product.setStock(request.getStock() != null ? request.getStock() :
    // product.getStock());

    // if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
    // product.setImage(uploadImage(request.getImageFile()));
    // }

    // repository.save(product);

    // return new ApiResponse<>(true, "Cập nhật sản phẩm thành công",
    // ProductMapper.toDto(product));
    // } catch (Exception e) {
    // return new ApiResponse<>(false, e.getMessage(), null);
    // }
    // }

    // public ApiResponse<ProductDto> delete(int userId, @NonNull Integer productId)
    // {
    // try {
    // Product product = repository.findById(productId)
    // .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

    // checkUserPermission(userId, product);

    // if (product.getSoldQuantity() > 0
    // || (product.getOrderProducts() != null &&
    // !product.getOrderProducts().isEmpty())) {
    // return new ApiResponse<>(false, "Sản phẩm đã có đơn hàng, không thể xóa",
    // null);
    // }

    // repository.delete(product);

    // return new ApiResponse<>(true, "Xóa sản phẩm thành công",
    // ProductMapper.toDto(product));
    // } catch (Exception e) {
    // return new ApiResponse<>(false, e.getMessage(), null);
    // }
    // }

    // @Transactional
    // public BulkResponse<ProductDto> createBulk(Integer userId,
    // UserBulkProductForm request) {

    // List<BulkResponse.BulkResult<ProductDto>> results = new ArrayList<>();
    // Set<String> namesInFile = new HashSet<>();

    // for (UserProductForm form : request.getProducts()) {

    // String trimmedName = form.getName() != null ? form.getName().trim() : "";

    // BulkResponse.BulkResult<ProductDto> result = new BulkResponse.BulkResult<>();
    // result.setName(trimmedName);

    // List<String> missing = new ArrayList<>();
    // if (trimmedName.isEmpty())
    // missing.add("tên");
    // if (form.getWeight() == null)
    // missing.add("trọng lượng");
    // if (form.getPrice() == null)
    // missing.add("giá");
    // if (form.getType() == null)
    // missing.add("loại");

    // if (!missing.isEmpty()) {
    // result.setSuccess(false);
    // result.setMessage("Thiếu thông tin: " + String.join(", ", missing));
    // results.add(result);
    // continue;
    // }

    // if (namesInFile.contains(trimmedName.toLowerCase())) {
    // result.setSuccess(false);
    // result.setMessage("Sản phẩm trùng trong file import");
    // results.add(result);
    // continue;
    // }

    // if (repository.existsByUserIdAndName(userId, trimmedName)) {
    // result.setSuccess(false);
    // result.setMessage("Tên sản phẩm đã tồn tại");
    // results.add(result);
    // continue;
    // }

    // try {
    // ProductDto created = createProduct(userId, form);
    // result.setSuccess(true);
    // result.setMessage("Thêm sản phẩm thành công");
    // result.setResult(created);

    // namesInFile.add(trimmedName.toLowerCase());
    // } catch (Exception e) {
    // result.setSuccess(false);
    // result.setMessage("Lỗi server: " + e.getMessage());
    // }

    // results.add(result);
    // }

    // int totalImported = (int)
    // results.stream().filter(BulkResponse.BulkResult::isSuccess).count();
    // int totalFailed = results.size() - totalImported;

    // BulkResponse<ProductDto> response = new BulkResponse<>();
    // response.setSuccess(true);
    // response.setMessage("Thêm hoàn tất: " + totalImported + " thành công, " +
    // totalFailed + " lỗi");
    // response.setTotalImported(totalImported);
    // response.setTotalFailed(totalFailed);
    // response.setResults(results);

    // return response;
    // }

    // public ApiResponse<ListResponse<ProductDto>>
    // getActiveAndInstockUserProducts(int userId,
    // UserProductSearchRequest request) {
    // try {
    // int page = request.getPage();
    // int limit = request.getLimit();
    // String search = request.getSearch();
    // String type = request.getType();

    // Pageable pageable = PageRequest.of(page - 1, limit,
    // Sort.by(Sort.Direction.DESC, "createdAt"));

    // Specification<Product> spec = ProductSpecification.unrestrictedProduct()
    // .and(ProductSpecification.userId(userId))
    // .and(ProductSpecification.search(search))
    // .and(ProductSpecification.type(type))
    // .and(ProductSpecification.status(ProductStatus.ACTIVE.name()))
    // .and(ProductSpecification.stock("instock"));

    // Page<Product> pageData = repository.findAll(spec, pageable);

    // List<ProductDto> list = pageData.getContent()
    // .stream()
    // .map(ProductMapper::toDto)
    // .toList();

    // int total = (int) pageData.getTotalElements();

    // Pagination pagination = new Pagination(total, page, limit,
    // pageData.getTotalPages());

    // ListResponse<ProductDto> data = new ListResponse<>();
    // data.setList(list);
    // data.setPagination(pagination);

    // return new ApiResponse<>(true, "Lấy danh sách sản phẩm đang bán được thành
    // công", data);
    // } catch (Exception e) {
    // return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
    // }
    // }

    // @Transactional
    // public void restoreStockFromOrder(List<OrderProduct> orderProducts) {
    // if (orderProducts == null || orderProducts.isEmpty()) return;

    // for (OrderProduct op : orderProducts) {
    // Product product = op.getProduct();
    // product.setStock(product.getStock() + op.getQuantity());
    // product.setSoldQuantity(product.getSoldQuantity() - op.getQuantity());
    // repository.save(product);
    // }
    // }

}
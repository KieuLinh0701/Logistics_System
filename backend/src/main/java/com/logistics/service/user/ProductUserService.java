package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.logistics.request.user.product.UserBulkProductForm;
import com.logistics.request.user.product.UserProductForm;
import com.logistics.request.user.product.UserProductSearchRequest;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.dto.ProductDto;
import com.logistics.entity.Product;
import com.logistics.entity.User;
import com.logistics.enums.ProductStatus;
import com.logistics.enums.ProductType;
import com.logistics.mapper.ProductMapper;
import com.logistics.repository.ProductRepository;
import com.logistics.repository.UserRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.ProductSpecification;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service

@RequiredArgsConstructor
public class ProductUserService {

    private final Cloudinary cloudinary;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private UserRepository userRepository;

    public ApiResponse<ListResponse<ProductDto>> list(int userId, UserProductSearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String type = request.getType();
            String status = request.getStatus();
            String stock = request.getStock();
            String sort = request.getSort();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

            Specification<Product> spec = ProductSpecification.unrestrictedProduct()
                    .and(ProductSpecification.userId(userId))
                    .and(ProductSpecification.search(search))
                    .and(ProductSpecification.type(type))
                    .and(ProductSpecification.status(status))
                    .and(ProductSpecification.stock(stock))
                    .and(ProductSpecification.sort(sort))
                    .and(ProductSpecification.createdAtBetween(startDate, endDate));

            Page<Product> pageData = repository.findAll(spec, pageable);

            List<ProductDto> list = pageData.getContent()
                    .stream()
                    .map(ProductMapper::toDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ProductDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách sản phẩm thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private void checkUserPermission(int userId, Product product) {
        if (product.getUser() == null || !product.getUser().getId().equals(userId)) {
            throw new RuntimeException("Không có quyền thực hiện thao tác này");
        }
    }

    private void checkDuplicateName(int userId, String name) {
        boolean exists = repository.existsByUserIdAndName(userId, name.trim());
        if (exists) {
            throw new RuntimeException("Sản phẩm với tên này đã tồn tại");
        }
    }

    @SuppressWarnings("unchecked")
    private String uploadImage(MultipartFile file) {
        try {
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "products", "resource_type", "image"));
            return result.get("secure_url").toString();
        } catch (Exception e) {
            throw new RuntimeException("Upload ảnh thất bại: " + e.getMessage());
        }
    }

    private ProductDto createProduct(int userId, UserProductForm request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên sản phẩm không được để trống");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            throw new RuntimeException("Giá sản phẩm không được để trống hoặc âm");
        }
        if (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Trọng lượng sản phẩm phải lớn hơn 0");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new RuntimeException("Loại sản phẩm không được để trống");
        }

        checkDuplicateName(userId, request.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Product product = new Product();
        product.setUser(user);
        product.setName(request.getName());
        product.setWeight(request.getWeight());
        product.setPrice(request.getPrice());
        product.setType(ProductType.valueOf(request.getType()));
        product.setStatus(
                request.getStatus() != null ? ProductStatus.valueOf(request.getStatus()) : ProductStatus.ACTIVE);
        product.setStock(request.getStock() != null ? request.getStock() : 0);

        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            product.setImage(uploadImage(request.getImageFile()));
        }

        product = repository.save(product);
        product.setCode("PROD" + product.getId());
        repository.save(product);

        return ProductMapper.toDto(product);
    }

    public ApiResponse<ProductDto> create(int userId, UserProductForm request) {
        try {
            ProductDto dto = createProduct(userId, request);
            return new ApiResponse<>(true, "Thêm sản phẩm thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<ProductDto> update(int userId, UserProductForm request) {
        try {
            Product product = repository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            checkUserPermission(userId, product);

            if (request.getName() != null && !request.getName().equals(product.getName())) {
                checkDuplicateName(userId, request.getName());
                product.setName(request.getName());
            }

            product.setWeight(request.getWeight() != null ? request.getWeight() : product.getWeight());
            product.setPrice(request.getPrice() != null ? request.getPrice() : product.getPrice());
            product.setType(request.getType() != null ? ProductType.valueOf(request.getType()) : product.getType());
            product.setStatus(
                    request.getStatus() != null ? ProductStatus.valueOf(request.getStatus()) : product.getStatus());
            product.setStock(request.getStock() != null ? request.getStock() : product.getStock());

            if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
                product.setImage(uploadImage(request.getImageFile()));
            }

            repository.save(product);

            return new ApiResponse<>(true, "Cập nhật sản phẩm thành công", ProductMapper.toDto(product));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<ProductDto> delete(int userId, @NonNull Integer productId) {
        try {
            Product product = repository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            checkUserPermission(userId, product);

            if (product.getSoldQuantity() > 0
                    || (product.getOrderProducts() != null && !product.getOrderProducts().isEmpty())) {
                return new ApiResponse<>(false, "Sản phẩm đã có đơn hàng, không thể xóa", null);
            }

            repository.delete(product);

            return new ApiResponse<>(true, "Xóa sản phẩm thành công", ProductMapper.toDto(product));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public BulkResponse<ProductDto> createBulk(Integer userId, UserBulkProductForm request) {

        List<BulkResponse.BulkResult<ProductDto>> results = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();

        for (UserProductForm form : request.getProducts()) {

            String trimmedName = form.getName() != null ? form.getName().trim() : "";

            BulkResponse.BulkResult<ProductDto> result = new BulkResponse.BulkResult<>();
            result.setName(trimmedName);

            List<String> missing = new ArrayList<>();
            if (trimmedName.isEmpty())
                missing.add("tên");
            if (form.getWeight() == null)
                missing.add("trọng lượng");
            if (form.getPrice() == null)
                missing.add("giá");
            if (form.getType() == null)
                missing.add("loại");

            if (!missing.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Thiếu thông tin: " + String.join(", ", missing));
                results.add(result);
                continue;
            }

            if (namesInFile.contains(trimmedName.toLowerCase())) {
                result.setSuccess(false);
                result.setMessage("Sản phẩm trùng trong file import");
                results.add(result);
                continue;
            }

            if (repository.existsByUserIdAndName(userId, trimmedName)) {
                result.setSuccess(false);
                result.setMessage("Tên sản phẩm đã tồn tại");
                results.add(result);
                continue;
            }

            try {
                ProductDto created = createProduct(userId, form);
                result.setSuccess(true);
                result.setMessage("Thêm sản phẩm thành công");
                result.setResult(created);

                namesInFile.add(trimmedName.toLowerCase());
            } catch (Exception e) {
                result.setSuccess(false);
                result.setMessage("Lỗi server: " + e.getMessage());
            }

            results.add(result);
        }

        int totalImported = (int) results.stream().filter(BulkResponse.BulkResult::isSuccess).count();
        int totalFailed = results.size() - totalImported;

        BulkResponse<ProductDto> response = new BulkResponse<>();
        response.setSuccess(true);
        response.setMessage("Thêm hoàn tất: " + totalImported + " thành công, " + totalFailed + " lỗi");
        response.setTotalImported(totalImported);
        response.setTotalFailed(totalFailed);
        response.setResults(results);

        return response;
    }
}
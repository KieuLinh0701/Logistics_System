package com.logistics.service.user;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.logistics.entity.ShippingRequest;
import com.logistics.specification.ShippingRequestSpecification;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import com.logistics.entity.OrderProduct;
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

import static com.logistics.utils.ProductUtils.translateProductStatus;
import static com.logistics.utils.ProductUtils.translateProductType;
import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestStatus;
import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestType;

@Service
@RequiredArgsConstructor
public class ProductUserService {

    private final Cloudinary cloudinary;

    private final ProductRepository repository;

    private final UserRepository userRepository;

    private final UserUserService userService;

    public ApiResponse<ListResponse<ProductDto>> list(int userId, UserProductSearchRequest request) {
        try {
            Integer shopId = userService.getShopId(userId);

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

            Specification<Product> spec = ProductSpecification.unrestrictedProduct()
                    .and(ProductSpecification.userId(shopId))
                    .and(ProductSpecification.search(search))
                    .and(ProductSpecification.type(type))
                    .and(ProductSpecification.status(status))
                    .and(ProductSpecification.stock(stock))
                    .and(ProductSpecification.createdAtBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("createdAt").descending();
                case "oldest" -> Sort.by("createdAt").ascending();
                case "best_selling" -> Sort.by("soldQuantity").descending();
                case "least_selling" -> Sort.by("soldQuantity").ascending();
                case "highest_price" -> Sort.by("price").descending();
                case "lowest_price" -> Sort.by("price").ascending();
                case "highest_stock" -> Sort.by("stock").descending();
                case "lowest_stock" -> Sort.by("stock").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
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

        return ProductMapper.toDto(product);
    }

    public ApiResponse<ProductDto> create(int userId, UserProductForm request) {
        try {
            Integer shopId = userService.getShopId(userId);

            ProductDto dto = createProduct(shopId, request);
            return new ApiResponse<>(true, "Thêm sản phẩm thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<ProductDto> update(int userId, UserProductForm request) {
        try {
            Integer shopId = userService.getShopId(userId);

            Product product = repository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            checkUserPermission(shopId, product);

            if (request.getName() != null && !request.getName().equals(product.getName())) {
                checkDuplicateName(shopId, request.getName());
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
            Integer shopId = userService.getShopId(userId);

            Product product = repository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            checkUserPermission(shopId, product);

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

        Integer shopId = userService.getShopId(userId);

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

            if (repository.existsByUserIdAndName(shopId, trimmedName)) {
                result.setSuccess(false);
                result.setMessage("Tên sản phẩm đã tồn tại");
                results.add(result);
                continue;
            }

            try {
                ProductDto created = createProduct(shopId, form);
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

    public ApiResponse<ListResponse<ProductDto>> getActiveAndInstockUserProducts(int userId,
            UserProductSearchRequest request) {
        try {
            Integer shopId = userService.getShopId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String type = request.getType();

            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

            Specification<Product> spec = ProductSpecification.unrestrictedProduct()
                    .and(ProductSpecification.userId(shopId))
                    .and(ProductSpecification.search(search))
                    .and(ProductSpecification.type(type))
                    .and(ProductSpecification.status(ProductStatus.ACTIVE.name()))
                    .and(ProductSpecification.stock("instock"));

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

            return new ApiResponse<>(true, "Lấy danh sách sản phẩm đang bán được thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public void restoreStockFromOrder(List<OrderProduct> orderProducts) {
        if (orderProducts == null || orderProducts.isEmpty()) return;

        for (OrderProduct op : orderProducts) {
            Product product = op.getProduct();
            product.setStock(product.getStock() + op.getQuantity());
            product.setSoldQuantity(product.getSoldQuantity() - op.getQuantity());
            repository.save(product);
        }
    }

    public byte[] export(Integer userId, UserProductSearchRequest request) {
        Integer shopId = userService.getShopId(userId);

        String search = request.getSearch();
        String type = request.getType();
        String status = request.getStatus();
        String stock = request.getStock();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Specification<Product> spec = ProductSpecification.unrestrictedProduct()
                .and(ProductSpecification.userId(shopId))
                .and(ProductSpecification.search(search))
                .and(ProductSpecification.type(type))
                .and(ProductSpecification.status(status))
                .and(ProductSpecification.stock(stock))
                .and(ProductSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "best_selling" -> Sort.by("soldQuantity").descending();
            case "least_selling" -> Sort.by("soldQuantity").ascending();
            case "highest_price" -> Sort.by("price").descending();
            case "lowest_price" -> Sort.by("price").ascending();
            case "highest_stock" -> Sort.by("stock").descending();
            case "lowest_stock" -> Sort.by("stock").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<Product> products = repository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã SP",
                    "Tên",
                    "Loại",
                    "Trạng thái",
                    "Khối lượng (Kg)",
                    "Giá SP (VNĐ)",
                    "Tồn kho",
                    "Tổng bán",
                    "Ngày tạo"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            int rowIdx = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(p.getCode() != null ? p.getCode() : "");
                row.createCell(1).setCellValue(p.getName() != null ? p.getName() : "");
                row.createCell(2).setCellValue(translateProductType(p.getType()));
                row.createCell(3).setCellValue(translateProductStatus(p.getStatus()));
                row.createCell(4).setCellValue(p.getWeight() != null ? p.getWeight().doubleValue() : 0);
                row.createCell(5).setCellValue(p.getPrice() != null ? p.getPrice() : 0);
                row.createCell(6).setCellValue(p.getStock() != null ? p.getStock() : 0);
                row.createCell(7).setCellValue(p.getSoldQuantity() != null ? p.getSoldQuantity() : 0);
                row.createCell(8).setCellValue(p.getCreatedAt() != null ? p.getCreatedAt().format(dtf) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất Excel", e);
        }
    }
}
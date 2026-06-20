package com.logistics.service.user;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.logistics.dto.ProductDto;
import com.logistics.entity.OrderProduct;
import com.logistics.entity.Product;
import com.logistics.entity.User;
import com.logistics.enums.ProductStatus;
import com.logistics.enums.ProductType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.ProductErrorCode;
import com.logistics.mapper.ProductMapper;
import com.logistics.repository.ProductRepository;
import com.logistics.request.user.product.UserBulkProductForm;
import com.logistics.request.user.product.UserProductForm;
import com.logistics.request.user.product.UserProductSearchRequest;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.ProductSpecification;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
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

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.logistics.utils.ProductUtils.translateProductStatus;
import static com.logistics.utils.ProductUtils.translateProductType;

@Service
@RequiredArgsConstructor
public class ProductUserService {

    private final Cloudinary cloudinary;

    private final ProductRepository repository;

    private final UserUserService userService;

    public ListResponse<ProductDto> list(int userId, UserProductSearchRequest request) {
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

            return data;
    }

    private void checkUserPermission(int userId, Product product) {
        if (product.getUser() == null || !product.getUser().getId().equals(userId)) {
            throw new AppException(ProductErrorCode.PRODUCT_PERMISSION_DENIED);
        }
    }

    private void checkDuplicateName(int userId, String name) {
        boolean exists = repository.existsByUserIdAndName(userId, name.trim());
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_NAME_EXISTS);
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
            throw new AppException(CommonErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    private ProductDto createProduct(int userId, UserProductForm request) {

        checkDuplicateName(userId, request.name());

        User user = userService.getUser(userId);

        Product product = new Product();
        product.setUser(user);
        product.setName(request.name());
        product.setWeight(request.weight());
        product.setPrice(request.price());
        product.setType(ProductType.valueOf(request.type()));
        product.setStatus(
                request.status() != null ? ProductStatus.valueOf(request.status()) : ProductStatus.ACTIVE);
        product.setStock(request.stock() != null ? request.stock() : 0);

        if (request.imageFile() != null && !request.imageFile().isEmpty()) {
            product.setImage(uploadImage(request.imageFile()));
        }

        product = repository.save(product);

        return ProductMapper.toDto(product);
    }

    public ProductDto create(int userId, UserProductForm request) {
            Integer shopId = userService.getShopId(userId);

            return createProduct(shopId, request);
    }

    public ProductDto update(int userId, UserProductForm request) {
            Integer shopId = userService.getShopId(userId);

            Product product = getProduct(request.id());

            checkUserPermission(shopId, product);

            if (request.name() != null && !request.name().equals(product.getName())) {
                checkDuplicateName(shopId, request.name());
                product.setName(request.name());
            }

            product.setWeight(request.weight() != null ? request.weight() : product.getWeight());
            product.setPrice(request.price() != null ? request.price() : product.getPrice());
            product.setType(request.type() != null ? ProductType.valueOf(request.type()) : product.getType());
            product.setStatus(
                    request.status() != null ? ProductStatus.valueOf(request.status()) : product.getStatus());
            product.setStock(request.stock() != null ? request.stock() : product.getStock());

            if (request.imageFile() != null && !request.imageFile().isEmpty()) {
                product.setImage(uploadImage(request.imageFile()));
            }

            repository.save(product);

            return ProductMapper.toDto(product);
    }

    public ProductDto delete(int userId, @NonNull Integer productId) {
            Integer shopId = userService.getShopId(userId);

            Product product = getProduct(productId);

            checkUserPermission(shopId, product);

            if (product.getSoldQuantity() > 0
                    || (product.getOrderProducts() != null && !product.getOrderProducts().isEmpty())) {
                throw new AppException(ProductErrorCode.PRODUCT_HAS_ORDER);
            }

            repository.delete(product);

            return ProductMapper.toDto(product);
    }

    @Transactional
    public BulkResponse<ProductDto> createBulk(Integer userId, UserBulkProductForm request) {

        Integer shopId = userService.getShopId(userId);

        List<BulkResponse.BulkResult<ProductDto>> results = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();

        for (UserProductForm form : request.getProducts()) {

            String trimmedName = form.name() != null ? form.name().trim() : "";

            BulkResponse.BulkResult<ProductDto> result = new BulkResponse.BulkResult<>();
            result.setName(trimmedName);

            List<String> missing = new ArrayList<>();
            if (trimmedName.isEmpty())
                missing.add("tên");
            if (form.weight() == null)
                missing.add("trọng lượng");
            if (form.price() == null)
                missing.add("giá");
            if (form.type() == null)
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

    public ListResponse<ProductDto> getActiveAndInstockUserProducts(int userId,
            UserProductSearchRequest request) {
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

            return data;
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
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR, e);
        }
    }

    private Product getProduct(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
package com.logistics.service.user;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cloudinary.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dto.user.shippingRequest.UserShippingRequestDetailDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestEditDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestListDto;
import com.logistics.entity.Address;
import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.ShippingRequestAttachment;
import com.logistics.entity.User;
import com.logistics.enums.ShippingRequestAttachmentType;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.ShippingRequestErrorCode;
import com.logistics.mapper.ShippingRequestMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShippingRequestAttachmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.request.user.shippingRequest.UserShippingRequestForm;
import com.logistics.request.user.shippingRequest.UserShippingRequestSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.ShippingRequestSpecification;
import com.logistics.utils.ShippingRequestUtils;

import lombok.RequiredArgsConstructor;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestStatus;
import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestType;

@Service
@RequiredArgsConstructor
public class ShippingRequestUserService {

    private final ShippingRequestRepository repository;
    private final OrderRepository orderRepository;

    private final Cloudinary cloudinary;

    private final ShippingRequestAttachmentRepository shippingRequestAttachmentRepository;

    private final NotificationService notificationService;

    private final AddressRepository addressRepository;
    private final UserUserService userService;

    public ListResponse<UserShippingRequestListDto> list(int userId,
            UserShippingRequestSearchRequest request) {
            Integer shopId = userService.getShopId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String status = request.getStatus();
            String type = request.getType();
            String sort = request.getSort();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Specification<ShippingRequest> spec = ShippingRequestSpecification.unrestrictedShippingRequest()
                    .and(ShippingRequestSpecification.userId(shopId))
                    .and(ShippingRequestSpecification.search(search))
                    .and(ShippingRequestSpecification.status(status))
                    .and(ShippingRequestSpecification.requestType(type))
                    .and(ShippingRequestSpecification.createdAtBetween(startDate, endDate))
                    .and(ShippingRequestSpecification.responseAtBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("paidAt").descending();
                case "oldest" -> Sort.by("paidAt").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<ShippingRequest> pageData = repository.findAll(spec, pageable);

            List<UserShippingRequestListDto> list = pageData.getContent()
                    .stream()
                    .map(ShippingRequestMapper::toUserShippingRequestListDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<UserShippingRequestListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public UserShippingRequestDetailDto getShippingRequestById(int userId, int id) {
            Integer shopId = userService.getShopId(userId);

            ShippingRequest request = repository.findByIdAndUserId(id, shopId)
                    .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_NOT_FOUND));

            List<ShippingRequestAttachment> requestAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.REQUEST);

            List<ShippingRequestAttachment> responseAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.RESPONSE);

            return ShippingRequestMapper.toUserShippingRequestDetailDto(
                    request,
                    requestAttachments,
                    responseAttachments);
    }

    public UserShippingRequestEditDto getShippingRequestByIdForEdit(int userId, int id) {
            Integer shopId = userService.getShopId(userId);

            ShippingRequest request = repository.findByIdAndUserId(id, shopId)
                    .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_NOT_FOUND));

            List<ShippingRequestAttachment> requestAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.REQUEST);

            return ShippingRequestMapper.toUserShippingRequestEditDto(
                    request,
                    requestAttachments);
    }

    public void create(int userId, UserShippingRequestForm request) {

        Integer shopId = userService.getShopId(userId);

        User user = userService.getUser(shopId);

        Order order = getOrderByTrackingNumber(user.getId(), request.trackingNumber());

        ShippingRequestType type = request.requestType();

        if (!ShippingRequestUtils.canUserEmptyTrackingNumber(type) && order == null) {
            throw new AppException(OrderErrorCode.ORDER_NOT_FOUND);
        }

        if (order != null) {
            boolean valid = ShippingRequestUtils.isValidRequestForOrder(type, order.getStatus());
            if (!valid) {
                throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_INVALID_STATUS);
            }
        }

        if (hasActiveRequest(shopId, type, request.requestContent(), order)) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_ALREADY_EXISTS);
        }

        Office office = null;
        if (order != null) {
            office = ShippingRequestUtils.getValidOfficeForRequest(type, order.getStatus(), order.getFromOffice(),
                    order.getToOffice());
        }

        Address address = addressRepository
                    .findByUserIdAndIsDefaultTrue(shopId)
                    .orElse(null);

        ShippingRequest shippingRequest = new ShippingRequest();

        shippingRequest.setUser(user);
        shippingRequest.setContactName(user.getFullName());
        shippingRequest.setContactEmail(user.getAccount().getEmail());
        shippingRequest.setContactPhoneNumber(user.getPhoneNumber());
        shippingRequest.setContactCityCode(address != null ? address.getCityCode() : null);
        shippingRequest.setContactCityName(address != null ? address.getCityName() : null);
        shippingRequest.setContactWardCode(address != null ? address.getWardCode() : null);
        shippingRequest.setContactWardName(address != null ? address.getWardName() : null);
        shippingRequest.setContactDetail(address != null ? address.getDetail() : null);
        shippingRequest.setContactFullAddress(address != null ? address.getFullAddress() : null);

        shippingRequest.setOrder(order);
        shippingRequest.setOffice(office);
        shippingRequest.setRequestType(type);
        shippingRequest.setRequestContent(request.requestContent());

        shippingRequest = repository.save(shippingRequest);

        String code = ShippingRequestUtils.generateRequestCode(shippingRequest.getId());
        shippingRequest.setCode(code);
        repository.save(shippingRequest);

        saveAttachments(shippingRequest, request.attachments());

        if (office != null && office.getManager() != null) {
                notificationService.create(
                        "Yêu cầu hỗ trợ và khiếu nại mới",
                        "Có yêu cầu mới: " + shippingRequest.getCode(),
                        "shipping_request",
                        office.getManager().getUser().getId(), 
                        null,
                        "supports",
                        shippingRequest.getCode());
            }
    }

    public void update(int userId, int id, UserShippingRequestForm request) {

        Integer shopId = userService.getShopId(userId);

        ShippingRequest shippingRequest = repository.findByIdAndUserId(id, shopId)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_NOT_FOUND));

        if (!validateEdit(request, shippingRequest)) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_EDIT_NOT_ALLOWED);
        }

        String oldAttachmentsStr = request.oldAttachments();
        List<Integer> oldAttachmentIds = new ArrayList<>();

        if (oldAttachmentsStr != null && !oldAttachmentsStr.isBlank()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                oldAttachmentIds = mapper.readValue(
                        oldAttachmentsStr,
                        mapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
            } catch (Exception e) {
                throw new AppException(CommonErrorCode.PARSE_ATTACHMENTS_FAILED);
            }
        }

        shippingRequest.setRequestContent(request.requestContent());
        shippingRequest = repository.save(shippingRequest);

        String code = ShippingRequestUtils.generateRequestCode(shippingRequest.getId());
        shippingRequest.setCode(code);
        repository.save(shippingRequest);

        updateAttachments(shippingRequest, request.attachments(), oldAttachmentIds);
    }

    public void cancel(int userId, int id) {

        Integer shopId = userService.getShopId(userId);

        ShippingRequest shippingRequest = repository.findByIdAndUserId(id, shopId)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_NOT_FOUND));

        if (!ShippingRequestUtils.canUserCancel(shippingRequest.getStatus())) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_CANNOT_CANCEL);
        }

        shippingRequest.setStatus(ShippingRequestStatus.CANCELLED);
        repository.save(shippingRequest);

        if (shippingRequest.getOffice() != null && shippingRequest.getOffice().getManager() != null) {
            System.out.println("UserId" + shippingRequest.getOffice().getManager().getUser().getId());
            notificationService.create(
                    "Yêu cầu hỗ trợ và khiếu nại đã bị hủy",
                    "Yêu cầu mã " + shippingRequest.getCode() + " đã bị khách hàng hủy.",
                    "shipping_request",
                    shippingRequest.getOffice().getManager().getUser().getId(),
                    null,
                    "supports",
                    shippingRequest.getCode());
        }
    }

    public byte[] export(Integer userId, UserShippingRequestSearchRequest request) {
        Integer shopId = userService.getShopId(userId);

        String search = request.getSearch();
        String status = request.getStatus();
        String type = request.getType();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Specification<ShippingRequest> spec = ShippingRequestSpecification.unrestrictedShippingRequest()
                .and(ShippingRequestSpecification.userId(shopId))
                .and(ShippingRequestSpecification.search(search))
                .and(ShippingRequestSpecification.status(status))
                .and(ShippingRequestSpecification.requestType(type))
                .and(ShippingRequestSpecification.createdAtBetween(startDate, endDate))
                .and(ShippingRequestSpecification.responseAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("paidAt").descending();
            case "oldest" -> Sort.by("paidAt").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<ShippingRequest> requests = repository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ShippingRequests");

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
                    "Mã yêu cầu",
                    "Loại yêu cầu",
                    "Trạng thái",
                    "ĐH liên quan",
                    "Nội dung yêu cầu",
                    "Nội dung phản hồi",
                    "Thời gian gửi",
                    "Thời gian phản hồi"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (ShippingRequest sr : requests) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(sr.getCode() != null ? sr.getCode() : "");
                row.createCell(1).setCellValue(translateShippingRequestType(sr.getRequestType()));
                row.createCell(2).setCellValue(translateShippingRequestStatus(sr.getStatus()));
                row.createCell(3).setCellValue(
                        (sr.getOrder() != null && sr.getOrder().getTrackingNumber() != null) ?
                        sr.getOrder().getTrackingNumber() : "");
                row.createCell(4).setCellValue(sr.getRequestContent() != null ? sr.getRequestContent() : "");
                row.createCell(5).setCellValue(sr.getResponse() != null ? sr.getResponse() : "Chưa có phản hồi");
                row.createCell(6).setCellValue(sr.getPaidAt() != null ? sr.getPaidAt().format(dtf) : "");
                row.createCell(7).setCellValue(sr.getResponseAt() != null ? sr.getResponseAt().format(dtf) : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    private void saveAttachments(ShippingRequest request, List<MultipartFile> files) {
        if (files != null && !files.isEmpty()) {
            String url;
            for (MultipartFile file : files) {
                url = uploadFile(file);

                ShippingRequestAttachment attachment = new ShippingRequestAttachment();
                attachment.setShippingRequest(request);
                attachment.setFileName(file.getOriginalFilename());
                attachment.setUrl(url);
                attachment.setType(ShippingRequestAttachmentType.REQUEST);

                shippingRequestAttachmentRepository.save(attachment);
            }
        }
    }

    private void updateAttachments(ShippingRequest request,
            List<MultipartFile> newFiles,
            List<Integer> oldAttachmentIds) {
        List<ShippingRequestAttachment> existingAttachments = shippingRequestAttachmentRepository
                .findByShippingRequestIdAndType(request.getId(), ShippingRequestAttachmentType.REQUEST);

        for (ShippingRequestAttachment oldAtt : existingAttachments) {
            if (oldAttachmentIds == null || !oldAttachmentIds.contains(oldAtt.getId())) {
                shippingRequestAttachmentRepository.delete(oldAtt);
            }
        }

        Set<String> existingFileNames = existingAttachments.stream()
                .filter(att -> oldAttachmentIds != null && oldAttachmentIds.contains(att.getId()))
                .map(ShippingRequestAttachment::getFileName)
                .collect(Collectors.toSet());

        if (newFiles != null) {
            for (MultipartFile file : newFiles) {
                String fileName = file.getOriginalFilename();
                if (!existingFileNames.contains(fileName)) {
                    String url = uploadFile(file);

                    ShippingRequestAttachment attachment = new ShippingRequestAttachment();
                    attachment.setShippingRequest(request);
                    attachment.setFileName(fileName);
                    attachment.setUrl(url);
                    attachment.setType(ShippingRequestAttachmentType.REQUEST);

                    shippingRequestAttachmentRepository.save(attachment);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String uploadFile(MultipartFile file) {
        try {
            String original = file.getOriginalFilename();

            String ext = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf("."))
                    : "";

            String publicId = "shipping-requests/" + UUID.randomUUID() + ext;

            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id", publicId));

            return result.get("secure_url").toString();
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    private Order getOrderByTrackingNumber(Integer userId, String trackingNumber) {
        if (StringUtils.isBlank(trackingNumber))
            return null;

        return orderRepository.findByTrackingNumberAndUserId(trackingNumber, userId)
                .orElse(null);
    }

    public boolean hasActiveRequest(
            Integer userId,
            ShippingRequestType type,
            String requestContent,
            Order order) {

        List<ShippingRequest> existingRequests = repository.findActiveRequests(
                userId, type, ShippingRequestUtils.ACTIVE_STATUSES, order, requestContent);

        return !existingRequests.isEmpty();
    }

    private boolean validateEdit(UserShippingRequestForm request, ShippingRequest entity) {
        String newTrackingNumber = request.trackingNumber() != null ? request.trackingNumber().trim() : "";
        String existingTrackingNumber = entity.getOrder() != null && entity.getOrder().getTrackingNumber() != null
                ? entity.getOrder().getTrackingNumber().trim()
                : "";

        if (entity.getRequestType() != request.requestType() || !existingTrackingNumber.equals(newTrackingNumber)) {
            return false;
        }
        return true;
    }

}
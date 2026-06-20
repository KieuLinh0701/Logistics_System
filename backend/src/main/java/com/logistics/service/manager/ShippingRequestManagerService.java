package com.logistics.service.manager;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.entity.*;
import com.logistics.enums.ShippingRequestAttachmentType;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.ShippingRequestErrorCode;
import com.logistics.mapper.ShippingRequestMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.ShippingRequestAttachmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestForm;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.ShippingRequestSpecification;
import com.logistics.utils.ShippingRequestUtils;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestStatus;
import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestType;

@Service
@RequiredArgsConstructor
public class ShippingRequestManagerService {

    private final ShippingRequestRepository repository;

    private final AddressRepository addressRepository;

    private final Cloudinary cloudinary;

    private final ShippingRequestAttachmentRepository shippingRequestAttachmentRepository;

    private final EmployeeManagerService employeeManagerService;

    private final NotificationService notificationService;

    public ListResponse<ManagerShippingRequestListDto> list(
            int userId,
            ManagerShippingRequestSearchRequest request) {
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

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Specification<ShippingRequest> spec = ShippingRequestSpecification.unrestrictedShippingRequest()
                    .and(ShippingRequestSpecification.officeId(userOffice.getId()))
                    .and(ShippingRequestSpecification.managerSearch(search))
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

            List<ManagerShippingRequestListDto> list = pageData.getContent()
                    .stream()
                    .map(item -> {
                        Integer uid = item.getUser() != null
                                ? item.getUser().getId()
                                : null;

                        Address address = (uid != null)
                                ? addressMap.getOrDefault(uid, null)
                                : null;

                        return ShippingRequestMapper
                                .toManagerShippingRequestListDto(item, address);
                    })
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerShippingRequestListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return data;
    }

    public byte[] export(int userId, ManagerShippingRequestSearchRequest request) {
        String search = request.getSearch();
        String status = request.getStatus();
        String type = request.getType();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<ShippingRequest> spec = ShippingRequestSpecification.unrestrictedShippingRequest()
                .and(ShippingRequestSpecification.officeId(userOffice.getId()))
                .and(ShippingRequestSpecification.managerSearch(search))
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

        // Build addressMap giống list
        List<Integer> userIds = requests.stream()
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
                    "Tên người gửi", "Mã người dùng", "Email", "SĐT", "Địa chỉ",
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

                Integer uid = sr.getUser() != null ? sr.getUser().getId() : null;
                Address address = (uid != null) ? addressMap.getOrDefault(uid, null) : null;

                row.createCell(0).setCellValue(sr.getCode() != null ? sr.getCode() : "");
                row.createCell(1).setCellValue(translateShippingRequestType(sr.getRequestType()));
                row.createCell(2).setCellValue(translateShippingRequestStatus(sr.getStatus()));

                // Người gửi
                row.createCell(3).setCellValue(sr.getContactName() != null ? sr.getContactName() : "N/A");
                row.createCell(4).setCellValue(sr.getUser() != null && sr.getUser().getCode() != null ? sr.getUser().getCode() : "Khách vãng lai");
                row.createCell(5).setCellValue(sr.getContactEmail() != null ? sr.getContactEmail() : "N/A");
                row.createCell(6).setCellValue(sr.getContactPhoneNumber() != null ? sr.getContactPhoneNumber() : "N/A");
                row.createCell(7).setCellValue(sr.getContactFullAddress() != null ? sr.getContactFullAddress()
                        : (address != null && address.getFullAddress() != null ? address.getFullAddress() : "N/A"));

                // Nội dung
                row.createCell(8).setCellValue(sr.getOrder() != null && sr.getOrder().getTrackingNumber() != null ? sr.getOrder().getTrackingNumber() : "");
                row.createCell(9).setCellValue(sr.getRequestContent() != null ? sr.getRequestContent() : "");
                row.createCell(10).setCellValue(sr.getResponse() != null ? sr.getResponse() : "Chưa phản hồi");

                // Thời gian
                row.createCell(11).setCellValue(sr.getPaidAt() != null ? sr.getPaidAt().format(dtf) : "");
                row.createCell(12).setCellValue(sr.getResponseAt() != null ? sr.getResponseAt().format(dtf) : "N/A");
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
    
    private void checkPermission(int userId, ShippingRequest request) {
        if (request == null || request.getOffice() == null || request.getOffice().getId() == null) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_ACCESS_DENIED);
        }

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);
        if (userOffice == null) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_ACCESS_DENIED);
        }

        if (!request.getOffice().getId().equals(userOffice.getId())) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_ACCESS_DENIED);
        }
    }

    public ManagerShippingRequestDetailDto getShippingRequestById(int userId, int id) {
            ShippingRequest request = getShippingRequestById(id);

            checkPermission(userId, request);

            List<ShippingRequestAttachment> requestAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.REQUEST);

            List<ShippingRequestAttachment> responseAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.RESPONSE);

            return ShippingRequestMapper.toManagerShippingRequestDetailDto(
                    request,
                    requestAttachments,
                    responseAttachments);
    }

    public void processing(int userId, int id, ManagerShippingRequestForm request) {

        ShippingRequest shippingRequest = getShippingRequestById(id);

        checkPermission(userId, shippingRequest);

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        User user = userOffice.getManager().getUser();

        validateForm(request);

        ShippingRequestStatus newStatus = ShippingRequestStatus.valueOf(request.getStatus());

        if (!ShippingRequestUtils.canManagerChangeStatus(shippingRequest.getStatus(), newStatus)
                && isBlank(request.getStatus())) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_INVALID_STATUS);
        }

        String oldAttachmentsStr = request.getOldAttachments();
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

        shippingRequest.setStatus(newStatus);
        shippingRequest.setResponse(request.getResponse());
        shippingRequest.setResponseAt(LocalDateTime.now());

        shippingRequest.setHandler(user);
        shippingRequest.setHandlerName(user != null ? user.getFullName() : null);
        shippingRequest.setHandlerEmail((user != null && user.getAccount() != null)
                ? user.getAccount().getEmail()
                : null);
        shippingRequest.setHandlerPhoneNumber(user != null ? user.getPhoneNumber() : null);

        shippingRequest = repository.save(shippingRequest);

        saveAttachments(shippingRequest, request.getAttachments(), oldAttachmentIds);

        if (shippingRequest.getUser() != null && shippingRequest.getUser().getId() != null) {
            sendShippingRequestNotification(shippingRequest);
        }
    }

    private void sendShippingRequestNotification(ShippingRequest shippingRequest) {
        Integer userId = shippingRequest.getUser().getId();

        String title;
        String message;

        switch (shippingRequest.getStatus()) {
            case PROCESSING -> {
                title = "Yêu cầu đang xử lý";
                message = "Yêu cầu mã " + shippingRequest.getCode() + " đang được bưu cục xử lý.";
            }
            case RESOLVED -> {
                title = "Yêu cầu đã xử lý xong";
                message = "Yêu cầu mã " + shippingRequest.getCode() + " đã được bưu cục xử lý thành công.";
            }
            case REJECTED -> {
                title = "Yêu cầu bị từ chối";
                message = "Yêu cầu mã " + shippingRequest.getCode() + " đã bị bưu cục từ chối.";
            }
            default -> {
                title = "Cập nhật yêu cầu";
                message = "Yêu cầu mã " + shippingRequest.getCode() + " đã được cập nhật trạng thái: "
                        + shippingRequest.getStatus();
            }
        }

        notificationService.create(
                title,
                message,
                "shipping_request",
                userId,
                null,
                "orders/requests",
                shippingRequest.getCode());
    }

    private void saveAttachments(ShippingRequest request,
            List<MultipartFile> newFiles,
            List<Integer> oldAttachmentIds) {
        List<ShippingRequestAttachment> existingAttachments = shippingRequestAttachmentRepository
                .findByShippingRequestIdAndType(request.getId(), ShippingRequestAttachmentType.RESPONSE);

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
                    attachment.setType(ShippingRequestAttachmentType.RESPONSE);

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

    private void validateForm(ManagerShippingRequestForm request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.getStatus()))
            missing.add("Trạng thái");

        if (!missing.isEmpty())
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missing));

        ShippingRequestStatus status;
        try {
            status = ShippingRequestStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_INVALID_STATUS);
        }

        if (!isBlank(request.getResponse()) && request.getResponse().length() > 1000) {
            throw new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_INVALID_RESPONSE);
        }
    }

    private ShippingRequest getShippingRequestById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ShippingRequestErrorCode.SHIPPING_REQUEST_NOT_FOUND));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}
package com.logistics.service.user;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dto.user.shippingRequest.UserShippingRequestDetailDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestEditDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestListDto;
import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.ShippingRequestAttachment;
import com.logistics.entity.User;
import com.logistics.enums.ShippingRequestAttachmentType;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.enums.ShippingRequestType;
import com.logistics.mapper.ShippingRequestMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShippingRequestAttachmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.shippingRequest.UserShippingRequestForm;
import com.logistics.request.user.shippingRequest.UserShippingRequestSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.NotificationService;
import com.logistics.specification.ShippingRequestSpecification;
import com.logistics.utils.ShippingRequestUtils;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingRequestUserService {

    private final ShippingRequestRepository repository;

    private final UserRepository userRepository;

    private final OrderRepository orderRepository;

    private final Cloudinary cloudinary;

    private final ShippingRequestAttachmentRepository shippingRequestAttachmentRepository;

    private final NotificationService notificationService;

    public ApiResponse<ListResponse<UserShippingRequestListDto>> list(int userId,
            UserShippingRequestSearchRequest request) {
        try {
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
                    .and(ShippingRequestSpecification.userId(userId))
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

            return new ApiResponse<>(true, "Lấy danh sách yêu cầu thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserShippingRequestDetailDto> getShippingRequestById(int userId, int id) {
        try {
            ShippingRequest request = repository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

            List<ShippingRequestAttachment> requestAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.REQUEST);

            List<ShippingRequestAttachment> responseAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.RESPONSE);

            UserShippingRequestDetailDto data = ShippingRequestMapper.toUserShippingRequestDetailDto(
                    request,
                    requestAttachments,
                    responseAttachments);

            return new ApiResponse<>(true, "Lấy chi tiết yêu cầu theo id thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserShippingRequestEditDto> getShippingRequestByIdForEdit(int userId, int id) {
        try {
            ShippingRequest request = repository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

            List<ShippingRequestAttachment> requestAttachments = shippingRequestAttachmentRepository
                    .findByShippingRequestIdAndType(id, ShippingRequestAttachmentType.REQUEST);

            UserShippingRequestEditDto data = ShippingRequestMapper.toUserShippingRequestEditDto(
                    request,
                    requestAttachments);

            return new ApiResponse<>(true, "Lấy chi tiết yêu cầu cho chỉnh sửa theo id thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> create(int userId, UserShippingRequestForm request) {

        validateForm(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Order order = getOrderByTrackingNumber(user.getId(), request.getTrackingNumber());

        ShippingRequestType type = ShippingRequestType.valueOf(request.getRequestType());

        if (!ShippingRequestUtils.canUserEmptyTrackingNumber(type) && order == null) {
            throw new RuntimeException("Không tìm thấy đơn hàng cần tạo yêu cầu");
        }

        if (order != null) {
            boolean valid = ShippingRequestUtils.isValidRequestForOrder(type, order.getStatus());
            if (!valid) {
                return new ApiResponse<>(false, "Yêu cầu không hợp lệ với trạng thái đơn hàng", false);
            }
        }

        if (hasActiveRequest(userId, type, request.getRequestContent(), order)) {
            return new ApiResponse<>(false, "Đã có yêu cầu tương tự cho đơn hàng này đang được xử lý", false);
        }

        Office office = null;
        if (order != null) {
            office = ShippingRequestUtils.getValidOfficeForRequest(type, order.getStatus(), order.getFromOffice(),
                    order.getToOffice());
        }

        ShippingRequest shippingRequest = new ShippingRequest();
        shippingRequest.setUser(user);
        shippingRequest.setOrder(order);
        shippingRequest.setOffice(office);
        shippingRequest.setRequestType(type);
        shippingRequest.setRequestContent(request.getRequestContent());

        shippingRequest = repository.save(shippingRequest);

        String code = ShippingRequestUtils.generateRequestCode(shippingRequest.getId());
        shippingRequest.setCode(code);
        repository.save(shippingRequest);

        saveAttachments(shippingRequest, request.getAttachments());

        if (office != null && office.getManager() != null) {
            System.out.println("UserId" + office.getManager().getUser().getId());
                notificationService.create( 
                        "Yêu cầu hỗ trợ và khiếu nại mới",
                        "Có yêu cầu mới: " + shippingRequest.getCode(),
                        "shipping_request",
                        office.getManager().getUser().getId(), 
                        null,
                        "supports",
                        shippingRequest.getCode());
            }
        return new ApiResponse<>(true, "Tạo yêu cầu thành công", true);
    }

    public ApiResponse<Boolean> update(int userId, int id, UserShippingRequestForm request) {

        ShippingRequest shippingRequest = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu không tồn tại hoặc không thuộc về bạn"));

        validateForm(request);

        if (!validateEdit(request, shippingRequest)) {
            return new ApiResponse<>(false, "Không thể thay đổi loại yêu cầu hoặc đơn hàng khi chỉnh sửa", false);
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
                throw new RuntimeException("Parse oldAttachments thất bại", e);
            }
        }

        shippingRequest.setRequestContent(request.getRequestContent());
        shippingRequest = repository.save(shippingRequest);

        String code = ShippingRequestUtils.generateRequestCode(shippingRequest.getId());
        shippingRequest.setCode(code);
        repository.save(shippingRequest);

        updateAttachments(shippingRequest, request.getAttachments(), oldAttachmentIds);

        return new ApiResponse<>(true, "Sửa yêu cầu thành công", true);
    }

    public ApiResponse<Boolean> cancel(int userId, int id) {

        ShippingRequest shippingRequest = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Yêu cầu không tồn tại hoặc không thuộc về bạn"));

        if (!ShippingRequestUtils.canUserCancel(shippingRequest.getStatus())) {
            String message = ShippingRequestUtils.getCancelErrorMessage(shippingRequest.getStatus());
            return new ApiResponse<>(false, message, true);
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

        return new ApiResponse<>(true, "Hủy yêu cầu thành công", true);
    }

    private void saveAttachments(ShippingRequest request, List<MultipartFile> files) {
        if (files != null && !files.isEmpty()) {
            String url;
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
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
            throw new RuntimeException("Upload file thất bại: " + e.getMessage());
        }
    }

    private Order getOrderByTrackingNumber(Integer userId, String trackingNumber) {
        if (isBlank(trackingNumber))
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
        ShippingRequestType newType = ShippingRequestType.valueOf(request.getRequestType());

        String newTrackingNumber = request.getTrackingNumber() != null ? request.getTrackingNumber().trim() : "";
        String existingTrackingNumber = entity.getOrder() != null && entity.getOrder().getTrackingNumber() != null
                ? entity.getOrder().getTrackingNumber().trim()
                : "";

        if (entity.getRequestType() != newType || !existingTrackingNumber.equals(newTrackingNumber)) {
            return false;
        }
        return true;
    }

    private void validateForm(UserShippingRequestForm request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.getRequestType()))
            missing.add("Loại yêu cầu");

        if (!missing.isEmpty())
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));

        ShippingRequestType type;
        try {
            type = ShippingRequestType.valueOf(request.getRequestType());
        } catch (Exception e) {
            throw new RuntimeException("Loại yêu cầu không hợp lệ");
        }

        if (!isBlank(request.getRequestContent()) && request.getRequestContent().length() > 1000) {
            throw new RuntimeException("Nội dung yêu cầu không được vượt quá 1000 ký tự");
        }

        if (!ShippingRequestUtils.canUserEmptyContentRequest(type) && isBlank(request.getRequestContent())) {
            throw new RuntimeException("Nội dung yêu cầu không được để trống");
        }

        if (!ShippingRequestUtils.canUserEmptyTrackingNumber(type) && isBlank(request.getTrackingNumber())) {
            throw new RuntimeException("Mã đơn hàng không được để trống");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}
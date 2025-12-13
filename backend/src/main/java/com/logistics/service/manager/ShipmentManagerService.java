package com.logistics.service.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.entity.Office;
import com.logistics.entity.Shipment;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.ShipmentMapper;
import com.logistics.repository.ShipmentRepository;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.ShipmentSpecification;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentManagerService {

    private final ShipmentRepository repository;

    private final EmployeeManagerService employeeManagerService;

    public ApiResponse<ListResponse<ManagerShipmentListDto>> list(int userId,
            ManagerShipmentSearchRequest request) {
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

            Office userOffice = employeeManagerService.getOfficeByUserId(userId);

            Specification<Shipment> spec = ShipmentSpecification.unrestricted()
                    .and(ShipmentSpecification.fromOffice(userOffice.getId()))
                    .and(ShipmentSpecification.search(search))
                    .and(ShipmentSpecification.status(status))
                    .and(ShipmentSpecification.type(type))
                    .and(ShipmentSpecification.createdAtBetween(startDate, endDate));

            Sort sortOpt = switch (sort.toLowerCase()) {
                case "newest" -> Sort.by("createdAt").descending();
                case "oldest" -> Sort.by("createdAt").ascending();
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<Shipment> pageData = repository.findAll(spec, pageable);

            List<ManagerShipmentListDto> list = pageData.getContent()
                    .stream()
                    .map(ShipmentMapper::toManagerShipmentListDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerShipmentListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách chuyến hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Đã xảy ra lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ListResponse<ManagerShipmentDetailDto>> getOrdersByShipmentId(
            int userId,
            int shipmentId,
            ManagerOrdersShipmentSearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();

            Office userOffice = employeeManagerService.getOfficeByUserId(userId);

            Shipment shipment = repository.findById(shipmentId)
                    .filter(s -> s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId()))
                    .orElseThrow(() -> new RuntimeException("Shipment not found or no permission"));

            List<ManagerShipmentDetailDto> orders = shipment.getShipmentOrders()
                    .stream()
                    .map(so -> so.getOrder())
                    .filter(o -> search == null || o.getTrackingNumber().contains(search))
                    .map(OrderMapper::toManagerShipmentDetailDto)
                    .toList();

            int total = orders.size();
            int fromIndex = Math.min((page - 1) * limit, total);
            int toIndex = Math.min(fromIndex + limit, total);
            List<ManagerShipmentDetailDto> pagedList = orders.subList(fromIndex, toIndex);

            Pagination pagination = new Pagination(total, page, limit, (int) Math.ceil((double) total / limit));

            ListResponse<ManagerShipmentDetailDto> data = new ListResponse<>();
            data.setList(pagedList);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng của chuyến hàng thành công", data);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Đã xảy ra lỗi: " + e.getMessage(), null);
        }
    }
}
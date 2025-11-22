package com.logistics.service.user;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.dto.OrderDto;
import com.logistics.entity.Order;
import com.logistics.mapper.OrderMapper;
import com.logistics.repository.OrderRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.OrderSpecification;

@Service
public class OrderUserService {

    @Autowired
    private OrderRepository repository;

    public ApiResponse<ListResponse<OrderDto>> list(int userId, UserOrderSearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            String payer = request.getPayer();
            String status = request.getStatus();
            String pickupType = request.getPickupType();
            Integer serviceTypeId = request.getServiceTypeId();
            String paymentStatus = request.getPaymentStatus();
            String cod = request.getCod();
            String sort = request.getSort();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                    .and(OrderSpecification.userId(userId))
                    .and(OrderSpecification.search(search))
                    .and(OrderSpecification.payer(payer))
                    .and(OrderSpecification.status(status))
                    .and(OrderSpecification.pickupType(pickupType))
                    .and(OrderSpecification.serviceTypeId(serviceTypeId))
                    .and(OrderSpecification.paymentStatus(paymentStatus))
                    .and(OrderSpecification.cod(cod))
                    .and(OrderSpecification.sort(sort))
                    .and(OrderSpecification.createdAtBetween(startDate, endDate));

            Pageable pageable = PageRequest.of(page - 1, limit);

            Page<Order> pageData = repository.findAll(spec, pageable);

            List<OrderDto> list = pageData.getContent()
                    .stream()
                    .map(OrderMapper::toDto)
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<OrderDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }
}

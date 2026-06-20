package com.logistics.controller.common;

import com.logistics.dto.order.OrderFulfillmentSummaryDto;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.OrderFulfillmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Fulfillment", description = "Tra cứu thông tin tiến độ xử lý đơn hàng")
public class OrderFulfillmentController {

    private final OrderFulfillmentService orderFulfillmentService;

    @GetMapping("/{id}/fulfillment-summary")
    public ResponseEntity<ApiResponse<OrderFulfillmentSummaryDto>>
    getFulfillmentSummary(@PathVariable Integer id) {

        OrderFulfillmentSummaryDto result = orderFulfillmentService.getFulfillmentSummary(id.longValue());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
package com.logistics.controller.manager;

import com.logistics.dto.manager.attempt.AttemptHistoryListDto;
import com.logistics.request.manager.attempt.AttemptSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.AttemptHistoryManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/attempt-history")
@Tag(name = "Manager - Attempt History", description = "Lịch sử xử lý đơn hàng (pickup + delivery) cho Manager")
public class AttemptHistoryManagerController {

    private final AttemptHistoryManagerService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<AttemptHistoryListDto>>> list(
            @Valid @ModelAttribute AttemptSearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        ListResponse<AttemptHistoryListDto> result = service.list(userId, searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

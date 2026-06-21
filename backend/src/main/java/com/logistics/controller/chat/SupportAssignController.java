package com.logistics.controller.chat;

import com.logistics.dto.chat.SupportAssignManagerOption;
import com.logistics.dto.chat.SupportAssignOptionsResponse;
import com.logistics.response.ApiResponse;
import com.logistics.service.chat.SupportAssignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportAssignController {

    private final SupportAssignService supportAssignService;

    @GetMapping("/tickets/{ticketId}/assign-options")
    public ResponseEntity<ApiResponse<SupportAssignOptionsResponse>> getAssignOptions(
            @PathVariable Integer ticketId) {
        SupportAssignOptionsResponse options = supportAssignService.getAssignOptions(ticketId);
        return ResponseEntity.ok(ApiResponse.success("Success", options));
    }

    @GetMapping("/offices/{officeId}/managers")
    public ResponseEntity<ApiResponse<List<SupportAssignManagerOption>>> getManagersByOffice(
            @PathVariable Integer officeId) {
        List<SupportAssignManagerOption> managers = supportAssignService.getManagersByOffice(officeId);
        return ResponseEntity.ok(ApiResponse.success("Success", managers));
    }
}

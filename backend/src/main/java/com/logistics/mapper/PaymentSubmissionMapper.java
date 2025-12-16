package com.logistics.mapper;

import com.logistics.dto.manager.paymentSubmission.ManagerPaymentSubmissionListDto;
import com.logistics.entity.PaymentSubmission;

public class PaymentSubmissionMapper {

    public static ManagerPaymentSubmissionListDto toDto(PaymentSubmission entity) {
        if (entity == null) return null;

        ManagerPaymentSubmissionListDto dto = new ManagerPaymentSubmissionListDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setSystemAmount(entity.getSystemAmount());
        dto.setActualAmount(entity.getActualAmount());
        dto.setStatus(entity.getStatus().name());
        dto.setCheckedAt(entity.getCheckedAt());
        dto.setPaidAt(entity.getPaidAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setNotes(entity.getNotes());

        // Order
        if (entity.getOrder() != null) {
            dto.setOrder(
                new ManagerPaymentSubmissionListDto.Order(
                    entity.getOrder().getTrackingNumber()
                )
            );
        }

        // Checked by user
        if (entity.getCheckedBy() != null) {
            dto.setCheckedBy(
                new ManagerPaymentSubmissionListDto.User(
                    entity.getCheckedBy().getLastName(),
                    entity.getCheckedBy().getFirstName(),
                    entity.getCheckedBy().getPhoneNumber()
                )
            );
        }

        return dto;
    }
}
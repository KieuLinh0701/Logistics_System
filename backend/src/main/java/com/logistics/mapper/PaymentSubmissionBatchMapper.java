package com.logistics.mapper;

import com.logistics.dto.manager.paymentSubmissionBatch.ManagerPaymentSubmissionBatchListDto;
import com.logistics.entity.PaymentSubmissionBatch;

public class PaymentSubmissionBatchMapper {

    public static ManagerPaymentSubmissionBatchListDto toDto(PaymentSubmissionBatch entity) {
        if (entity == null) return null;

        ManagerPaymentSubmissionBatchListDto dto = new ManagerPaymentSubmissionBatchListDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setTotalSystemAmount(entity.getTotalSystemAmount());
        dto.setTotalActualAmount(entity.getTotalActualAmount());
        dto.setStatus(entity.getStatus().name());
        dto.setCheckedAt(entity.getCheckedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setNotes(entity.getNotes());
        dto.setTotalOrders(entity.getSubmissions().size());

        // Checked by user
        if (entity.getCheckedBy() != null) {
            dto.setCheckedBy(
                new ManagerPaymentSubmissionBatchListDto.User(
                    entity.getCheckedBy().getLastName(),
                    entity.getCheckedBy().getFirstName(),
                    entity.getCheckedBy().getPhoneNumber()
                )
            );
        }

        // shipper
        if (entity.getShipper() != null) {
            dto.setShipper(
                new ManagerPaymentSubmissionBatchListDto.User(
                    entity.getShipper().getLastName(),
                    entity.getShipper().getFirstName(),
                    entity.getShipper().getPhoneNumber()
                )
            );
        }

        return dto;
    }
}
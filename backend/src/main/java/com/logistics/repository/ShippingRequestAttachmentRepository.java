package com.logistics.repository;

import com.logistics.entity.ShippingRequestAttachment;
import com.logistics.enums.ShippingRequestAttachmentType;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingRequestAttachmentRepository extends JpaRepository<ShippingRequestAttachment, Integer>, JpaSpecificationExecutor<ShippingRequestAttachment> {
    // Lấy attachments theo ShippingRequest ID và TYPE
    List<ShippingRequestAttachment> findByShippingRequestIdAndType(
            Integer shippingRequestId,
            ShippingRequestAttachmentType type
    );

    // Lấy tất cả attachments của 1 request (không cần phân loại)
    List<ShippingRequestAttachment> findByShippingRequestId(Integer shippingRequestId);
}

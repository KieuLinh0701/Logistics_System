package com.logistics.repository;

import com.logistics.entity.ShippingRequestAttachment;
import com.logistics.enums.ShippingRequestAttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

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

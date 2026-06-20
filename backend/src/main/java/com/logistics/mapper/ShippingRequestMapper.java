package com.logistics.mapper;

import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestDetailDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestEditDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestListDto;
import com.logistics.entity.Address;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.ShippingRequestAttachment;

import java.util.List;

public class ShippingRequestMapper {

    // Manager
    public static ManagerShippingRequestListDto toManagerShippingRequestListDto(ShippingRequest entity,
            Address address) {
        if (entity == null) {
            return null;
        }

        return new ManagerShippingRequestListDto(
                entity.getId(),
                entity.getCode(),
                entity.getOrder() != null ? entity.getOrder().getTrackingNumber() : null,
                entity.getUser() != null ? entity.getUser().getCode() : null,
                entity.getContactName(),
                entity.getContactEmail(),
                entity.getContactPhoneNumber(),
                entity.getContactFullAddress(),
                entity.getRequestType() != null ? entity.getRequestType().name() : null,
                entity.getRequestContent(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getResponse(),
                entity.getPaidAt(),
                entity.getResponseAt());
    }

    public static ManagerShippingRequestDetailDto toManagerShippingRequestDetailDto(ShippingRequest entity,
            List<ShippingRequestAttachment> requestAttachments,
            List<ShippingRequestAttachment> responseAttachments) {
        if (entity == null) {
            return null;
        }

        return new ManagerShippingRequestDetailDto(
                entity.getId(),
                entity.getCode(),
                entity.getOrder() != null ? entity.getOrder().getTrackingNumber() : null,
                entity.getUser() != null ? entity.getUser().getCode() : null,
                entity.getContactName(),
                entity.getContactEmail(),
                entity.getContactPhoneNumber(),
                entity.getContactFullAddress(),
                entity.getHandlerName(),
                entity.getHandlerPhoneNumber(),
                entity.getHandlerEmail(),
                entity.getRequestType() != null ? entity.getRequestType().name() : null,
                entity.getRequestContent(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getResponse(),
                entity.getPaidAt(),
                entity.getResponseAt(),
                ShippingRequestAttachmentMapper.toDtoList(requestAttachments),
                ShippingRequestAttachmentMapper.toDtoList(responseAttachments));
    }

    // User
    public static UserShippingRequestListDto toUserShippingRequestListDto(ShippingRequest entity) {
        if (entity == null) {
            return null;
        }

        return new UserShippingRequestListDto(
                entity.getId(),
                entity.getCode(),
                entity.getOrder() != null ? entity.getOrder().getTrackingNumber() : null,
                entity.getRequestType() != null ? entity.getRequestType().name() : null,
                entity.getRequestContent(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getResponse(),
                entity.getPaidAt(),
                entity.getResponseAt());
    }

    public static UserShippingRequestDetailDto toUserShippingRequestDetailDto(ShippingRequest entity,
            List<ShippingRequestAttachment> requestAttachments,
            List<ShippingRequestAttachment> responseAttachments) {
        if (entity == null) {
            return null;
        }

        return new UserShippingRequestDetailDto(
                entity.getId(),
                entity.getCode(),
                entity.getOrder() != null ? entity.getOrder().getTrackingNumber() : null,
                entity.getHandlerName(),
                entity.getHandlerPhoneNumber(),
                entity.getHandlerEmail(),
                entity.getRequestType() != null ? entity.getRequestType().name() : null,
                entity.getRequestContent(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getResponse(),
                entity.getPaidAt(),
                entity.getResponseAt(),
                ShippingRequestAttachmentMapper.toDtoList(requestAttachments),
                ShippingRequestAttachmentMapper.toDtoList(responseAttachments));
    }

    public static UserShippingRequestEditDto toUserShippingRequestEditDto(ShippingRequest entity,
            List<ShippingRequestAttachment> requestAttachments) {
        if (entity == null) {
            return null;
        }

        return new UserShippingRequestEditDto(
                entity.getId(),
                entity.getCode(),
                entity.getOrder() != null ? entity.getOrder().getTrackingNumber() : null,
                entity.getRequestType() != null ? entity.getRequestType().name() : null,
                entity.getRequestContent(),
                ShippingRequestAttachmentMapper.toDtoList(requestAttachments));
    }
}
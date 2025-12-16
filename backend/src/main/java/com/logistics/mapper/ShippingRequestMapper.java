package com.logistics.mapper;

import java.util.List;

import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestDetailDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestEditDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestListDto;
import com.logistics.entity.Address;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.ShippingRequestAttachment;

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
                entity.getUser() != null ? entity.getUser().getFullName() : entity.getContactName(),
                entity.getUser() != null ? entity.getUser().getAccount().getEmail() : entity.getContactEmail(),
                entity.getUser() != null ? entity.getUser().getPhoneNumber() : entity.getContactPhoneNumber(),
                entity.getAddress() != null ? entity.getAddress().getCityCode() : address.getCityCode(),
                entity.getAddress() != null ? entity.getAddress().getWardCode() : address.getWardCode(),
                entity.getAddress() != null ? entity.getAddress().getDetail() : address.getDetail(),
                entity.getRequestType() != null ? entity.getRequestType().name() : null,
                entity.getRequestContent(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getResponse(),
                entity.getPaidAt(),
                entity.getResponseAt());
    }

    public static ManagerShippingRequestDetailDto toManagerShippingRequestDetailDto(ShippingRequest entity,
    Address address,
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
                entity.getUser() != null ? entity.getUser().getFullName() : entity.getContactName(),
                entity.getUser() != null ? entity.getUser().getAccount().getEmail() : entity.getContactEmail(),
                entity.getUser() != null ? entity.getUser().getPhoneNumber() : entity.getContactPhoneNumber(),
                entity.getAddress() != null ? entity.getAddress().getCityCode() : address.getCityCode(),
                entity.getAddress() != null ? entity.getAddress().getWardCode() : address.getWardCode(),
                entity.getAddress() != null ? entity.getAddress().getDetail() : address.getDetail(),
                entity.getHandler() != null ? entity.getHandler().getFullName() : null,
                entity.getHandler() != null ? entity.getHandler().getPhoneNumber() : null,
                entity.getHandler() != null ? entity.getHandler().getAccount().getEmail() : null,
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
                entity.getHandler() != null ? entity.getHandler().getFullName() : null,
                entity.getHandler() != null ? entity.getHandler().getPhoneNumber() : null,
                entity.getHandler() != null ? entity.getHandler().getAccount().getEmail() : null,
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
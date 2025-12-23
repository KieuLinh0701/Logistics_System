package com.logistics.mapper;

import com.logistics.dto.manager.incidentReport.ManagerIncidentReportDetailDto;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportListDto;
import com.logistics.entity.IncidentReport;

public class IncidentReportMapper {

    // Mapping cho List DTO
    public static ManagerIncidentReportListDto toListDto(IncidentReport entity) {
        if (entity == null)
            return null;

        ManagerIncidentReportListDto dto = new ManagerIncidentReportListDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        if (entity.getOrder() != null) {
            dto.setOrder(new ManagerIncidentReportListDto.Order(entity.getOrder().getTrackingNumber()));
        }
        dto.setTitle(entity.getTitle());
        dto.setIncidentType(entity.getIncidentType() != null ? entity.getIncidentType().name() : null);
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setPriority(entity.getPriority() != null ? entity.getPriority().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setHandledAt(entity.getHandledAt());

        return dto;
    }

    // Mapping cho Detail DTO
    public static ManagerIncidentReportDetailDto toDetailDto(IncidentReport entity) {
        if (entity == null)
            return null;

        ManagerIncidentReportDetailDto dto = new ManagerIncidentReportDetailDto();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());

        if (entity.getOrder() != null) {
            dto.setOrder(new ManagerIncidentReportDetailDto.Order(entity.getOrder().getTrackingNumber()));
        }

        if (entity.getShipper() != null) {
            dto.setShipper(new ManagerIncidentReportDetailDto.User(
                    entity.getShipper().getFullName(),
                    entity.getShipper().getPhoneNumber()
            ));
        }

        if (entity.getHandler() != null) {
            dto.setHandler(new ManagerIncidentReportDetailDto.User(
                    entity.getHandler().getFullName(),
                    entity.getHandler().getPhoneNumber()
            ));
        }

        dto.setTitle(entity.getTitle());
        dto.setIncidentType(entity.getIncidentType() != null ? entity.getIncidentType().name() : null);
        dto.setDescription(entity.getDescription());

        if (entity.getAddress() != null) {
            dto.setAddress(new ManagerIncidentReportDetailDto.Address(
                    entity.getAddress().getDetail(),
                    entity.getAddress().getCityCode(),
                    entity.getAddress().getWardCode()
            ));
        }

        dto.setImages(entity.getImages());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setPriority(entity.getPriority() != null ? entity.getPriority().name() : null);
        dto.setResolution(entity.getResolution());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setHandledAt(entity.getHandledAt());

        return dto;
    }
}
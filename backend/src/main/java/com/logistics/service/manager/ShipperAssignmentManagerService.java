package com.logistics.service.manager;

import com.logistics.dto.manager.shipperAssignment.ManagerShipperAssignmentListDto;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentEditRequest;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentSearchRequest;
import com.logistics.response.ListResponse;

public interface ShipperAssignmentManagerService {
    void create(int userId, ManagerShipperAssignmentEditRequest request);
    void update(int userId, Long assignmentId, ManagerShipperAssignmentEditRequest request);
    void deleteFutureAssignment(int userId, Long assignmentId);
    ListResponse<ManagerShipperAssignmentListDto> list(Integer userId, ManagerShipperAssignmentSearchRequest request);
    byte[] exportShipperAssignmentsExcel(Integer userId, ManagerShipperAssignmentSearchRequest request);
}
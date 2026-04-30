import React, { useCallback, useEffect, useMemo, useState } from "react";
import { message } from "antd";
import axiosClient from "../../../api/axiosClient";
import { translateShippingRequestStatus, translateShippingRequestType } from "../../../utils/shippingRequestUtils";
import AssignOfficeModal from "./components/AssignOfficeModal";
import ShippingRequestDetailsModal from "./components/ShippingRequestDetailsModal";
import ShippingRequestsTable from "./components/ShippingRequestsTable";
import ShippingRequestsToolbar from "./components/ShippingRequestsToolbar";
import type { OfficeOption, ShippingRequestRow } from "../../../types/shippingRequest";
import "../../../styles/ListPage.css";
import "../../../pages/hr/recruitment/components/RecruitmentShared.css";
import "./ShippingRequestsPage.css";

const ShippingRequestsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [requests, setRequests] = useState<ShippingRequestRow[]>([]);
  const [offices, setOffices] = useState<OfficeOption[]>([]);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>();
  const [typeFilter, setTypeFilter] = useState<string>();

  const [detailOpen, setDetailOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<ShippingRequestRow | null>(null);
  const [selectedOfficeId, setSelectedOfficeId] = useState<number>();
  const [assigning, setAssigning] = useState(false);

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    try {
      const res = await axiosClient.get<any>("/admin/shipping-requests");
      const list = Array.isArray(res) ? res : res?.data || res?.list || [];
      const normalized: ShippingRequestRow[] = (list || []).map((item: any) => ({
        id: item.id,
        code: item.code,
        requestType:
          typeof item.requestType === "string" ? item.requestType : item.requestType?.name || "",
        status: typeof item.status === "string" ? item.status : item.status?.name || "",
        office: item.office
          ? {
              id: item.office.id,
              name: item.office.name || item.office.code || "-",
            }
          : undefined,
        userName: item.user
          ? `${item.user.firstName || ""} ${item.user.lastName || ""}`.trim()
          : undefined,
        createdAt: item.paidAt || item.createdAt || item.created_at ||"",
        content: item.requestContent || item.content || item.request_content || "",
        contactName:
          item.contactName ||
          item.senderName ||
          (item.user
            ? `${item.user.firstName || ""} ${item.user.lastName || ""}`.trim()
            : undefined),
        contactPhoneNumber: item.contactPhoneNumber || item.contactPhone || item.user?.phoneNumber,
        contactEmail: item.contactEmail || item.user?.email,
        orderTrackingNumber: item.orderTrackingNumber || item.trackingNumber || item.order_tracking_number,
        response: item.response || item.managerResponse,
        responseAt: item.responseAt || item.respondedAt || item.response_at || null,
      }));
      setRequests(normalized);
    } catch (error) {
      message.error("Khong the tai danh sach yeu cau");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchOffices = useCallback(async () => {
    try {
      const res = await axiosClient.get<any>("/admin/offices");
      const list = Array.isArray(res) ? res : res?.data?.data || res?.data || [];
      setOffices((list || []).map((office: any) => ({ id: office.id, name: office.name })));
    } catch {
      setOffices([]);
    }
  }, []);

  useEffect(() => {
    fetchRequests();
    fetchOffices();
  }, [fetchRequests, fetchOffices]);

  const filteredRequests = useMemo(() => {
    return requests.filter((row) => {
      const q = keyword.trim().toLowerCase();
      const matchedKeyword =
        !q ||
        [row.code, row.content, row.contactName, row.userName]
          .filter(Boolean)
          .join(" ")
          .toLowerCase()
          .includes(q);
      const matchedStatus = !statusFilter || row.status === statusFilter;
      const matchedType = !typeFilter || row.requestType === typeFilter;
      return matchedKeyword && matchedStatus && matchedType;
    });
  }, [requests, keyword, statusFilter, typeFilter]);

  const statusOptions = useMemo(() => {
    const values = Array.from(new Set(requests.map((row) => row.status).filter(Boolean)));
    return values.map((value) => ({
      value,
      label: translateShippingRequestStatus(value),
    }));
  }, [requests]);

  const typeOptions = useMemo(() => {
    const values = Array.from(new Set(requests.map((row) => row.requestType).filter(Boolean)));
    return values.map((value) => ({
      value,
      label: translateShippingRequestType(value),
    }));
  }, [requests]);

  const handleView = (request: ShippingRequestRow) => {
    setSelectedRequest(request);
    setDetailOpen(true);
  };

  const handleAssignOpen = (request: ShippingRequestRow) => {
    setSelectedRequest(request);
    setSelectedOfficeId(undefined);
    setAssignOpen(true);
  };

  const handleAssignSubmit = async () => {
    if (!selectedRequest || !selectedOfficeId) return;
    try {
      setAssigning(true);
      await axiosClient.patch(
        `/admin/shipping-requests/${selectedRequest.id}/assign?officeId=${selectedOfficeId}`
      );
      message.success("Phan cong buu cuc thanh cong");
      setAssignOpen(false);
      setSelectedOfficeId(undefined);
      setSelectedRequest(null);
      fetchRequests();
    } catch {
      message.error("Khong the phan cong buu cuc");
    } finally {
      setAssigning(false);
    }
  };

  const handleRefreshAll = () => {
    setKeyword("");
    setStatusFilter(undefined);
    setTypeFilter(undefined);
    fetchRequests();
  };

  return (
    <div className="list-page-layout shipping-requests-page">
      <div className="list-page-content">
        <ShippingRequestsToolbar
          searchValue={keyword}
          filterType={typeFilter}
          filterStatus={statusFilter}
          typeOptions={typeOptions}
          statusOptions={statusOptions}
          onSearchChange={setKeyword}
          onTypeChange={(value) => setTypeFilter(value)}
          onStatusChange={(value) => setStatusFilter(value)}
          onRefresh={handleRefreshAll}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý yêu cầu hỗ trợ / khiếu nại</h3>
            <div style={{ marginTop: 12 }}>
              <div className="list-page-tag">Kết quả trả về: {filteredRequests.length} yêu cầu</div>
            </div>
          </div>
        </div>

        <div className="list-page-table">
          <ShippingRequestsTable
            loading={loading}
            data={filteredRequests}
            getStatusColor={() => "default"}
            getTypeColor={() => "default"}
            formatStatus={translateShippingRequestStatus}
            formatType={translateShippingRequestType}
            onView={handleView}
            onAssign={handleAssignOpen}
          />
        </div>
      </div>

      <ShippingRequestDetailsModal
        open={detailOpen}
        request={selectedRequest}
        typeText={translateShippingRequestType}
        statusText={translateShippingRequestStatus}
        onClose={() => {
          setDetailOpen(false);
          setSelectedRequest(null);
        }}
      />

      <AssignOfficeModal
        open={assignOpen}
        offices={offices}
        selectedOfficeId={selectedOfficeId}
        submitting={assigning}
        onChangeOffice={(value) => setSelectedOfficeId(value)}
        onSubmit={handleAssignSubmit}
        onCancel={() => {
          setAssignOpen(false);
          setSelectedRequest(null);
          setSelectedOfficeId(undefined);
        }}
      />
    </div>
  );
};

export default ShippingRequestsPage;

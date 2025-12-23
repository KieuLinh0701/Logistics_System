import React, { useEffect, useState } from "react";
import { Table, Button, Select, Modal, message, Tag, Typography } from "antd";
import axiosClient from "../../api/axiosClient";
import { translateShippingRequestStatus, translateShippingRequestType } from "../../utils/shippingRequestUtils";

const { Option } = Select;
const { Title } = Typography;

interface Office {
  id: number;
  name: string;
}

interface ShippingRequest {
  id: number;
  code: string;
  requestType: string;
  status: string;
  office?: Office;
  user?: { firstName: string; lastName: string };
  createdAt: string;
  content: string;
  contactName?: string;
  contactPhoneNumber?: string;
  contactEmail?: string;
  contactCityCode?: number;
  contactWardCode?: number;
  contactDetail?: string;
  userCode?: string;
  orderTrackingNumber?: string | null;
  requestContent?: string;
  response?: string;
  responseAt?: string | null;
  requestAttachments?: any[];
  responseAttachments?: any[];
}

const ShippingRequestsAdmin: React.FC = () => {
  const [requests, setRequests] = useState<ShippingRequest[]>([]);
  const [offices, setOffices] = useState<Office[]>([]);
  const [assignModal, setAssignModal] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<ShippingRequest | null>(null);
  const [selectedOffice, setSelectedOffice] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchRequests();
    fetchOffices();
  }, []);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const res = await axiosClient.get<any>("/admin/shipping-requests");

      let list: any[] = [];
      if (res && typeof res === "object" && "success" in res) {
        list = res.data || [];
      } else if (Array.isArray(res)) {
        list = res;
      } else {
        list = res?.data || res?.list || [];
      }
      const normalized = (list || []).map((r: any) => ({
        id: r.id,
        code: r.code,
        requestType: typeof r.requestType === "string" ? r.requestType : r.requestType?.name || "",
        status: typeof r.status === "string" ? r.status : r.status?.name || "",
        office: r.office ? { id: r.office.id, name: r.office.name || r.office.code || "" } : undefined,
        user: r.user ? { firstName: r.user.firstName, lastName: r.user.lastName } : undefined,
        contactName: r.contactName || r.senderName || (r.user ? `${r.user.firstName || ''} ${r.user.lastName || ''}`.trim() : undefined) || undefined,
        contactPhoneNumber: r.contactPhoneNumber || r.contactPhone || r.user?.phoneNumber || undefined,
        contactEmail: r.contactEmail || r.user?.email || undefined,
        contactCityCode: r.contactCityCode || r.contact_city_code || undefined,
        contactWardCode: r.contactWardCode || r.contact_ward_code || undefined,
        contactDetail: r.contactDetail || r.address || r.contact_address || undefined,
        userCode: r.user?.code || r.userCode || r.userCode || undefined,
        orderTrackingNumber: r.orderTrackingNumber || r.trackingNumber || r.order_tracking_number || null,
        requestContent: r.requestContent || r.content || r.request_content || "",
        content: r.requestContent || r.content || r.request_content || "",
        response: r.response || r.managerResponse || undefined,
        responseAt: r.responseAt || r.respondedAt || r.response_at || null,
        createdAt: r.paidAt || r.createdAt || r.created_at || null,
        requestAttachments: r.requestAttachments || r.attachments || [],
        responseAttachments: r.responseAttachments || r.response_attachments || [],

        __raw: r,
      }));

      setRequests(normalized);
    } catch (e) {
      message.error("Lỗi khi tải danh sách yêu cầu hỗ trợ");
    } finally {
      setLoading(false);
    }
  };

  const fetchOffices = async () => {
    try {
      const res = await axiosClient.get<any>("/admin/offices");
      if (res && typeof res === "object" && "success" in res) {
        const inner = res.data;
        if (inner && Array.isArray(inner.data)) {
          setOffices(inner.data);
        } else if (Array.isArray(inner)) {
          setOffices(inner);
        } else {
          setOffices([]);
        }
      } else if (Array.isArray(res)) {
        setOffices(res);
      } else {
        setOffices(res?.data || []);
      }
    } catch (e) {
      setOffices([]);
    } finally {
    }
  };

  const handleAssign = (request: ShippingRequest) => {
    setSelectedRequest(request);
    setAssignModal(true);
  };

  const handleAssignOk = async () => {
    if (!selectedRequest || !selectedOffice) return;
    try {
      const res = await axiosClient.patch<any>(
        `/admin/shipping-requests/${selectedRequest.id}/assign?officeId=${selectedOffice}`
      );
      message.success("Đã phân công cho bưu cục thành công");
      setAssignModal(false);
      setSelectedOffice(null);
      fetchRequests();
    } catch (e) {
      message.error("Lỗi khi phân công bưu cục");
    } finally {
    }
  };

  const columns = [
    { title: "Mã yêu cầu", dataIndex: "code", key: "code", render: (text: string) => <span style={{ fontWeight: 600, color: '#111827' }}>{text}</span> },
    { title: "Loại yêu cầu", dataIndex: "requestType", key: "requestType", render: (type: string) => {
        const color = getTypeColor(type);
        return <Tag color={color}>{translateShippingRequestType(type)}</Tag>;
      }
    },
    { title: "Trạng thái", dataIndex: "status", key: "status", render: (s: string) => {
        const color = getStatusColor(s);
        return <Tag color={color} style={{ fontWeight: 600 }}>{translateShippingRequestStatus(s)}</Tag>;
      }
    },
    { title: "Bưu cục", dataIndex: ["office", "name"], key: "office", render: (_: any, r: ShippingRequest) => r.office?.name || "-" },
    { title: "Người gửi", dataIndex: ["user", "firstName"], key: "user", render: (_: any, r: ShippingRequest) => r.user ? `${r.user.firstName} ${r.user.lastName}` : "Khách" },
    { title: "Ngày tạo", dataIndex: "createdAt", key: "createdAt" },
    { title: "Nội dung", dataIndex: "content", key: "content", ellipsis: true },
    {
      title: "Hành động",
      key: "action",
      render: (_: any, record: ShippingRequest) => (
        <Button onClick={() => handleAssign(record)} disabled={!!record.office}>
          Phân cho bưu cục
        </Button>
      ),
    },
  ];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'orange';
      case 'PROCESSING': return 'processing';
      case 'RESOLVED': return 'success';
      case 'REJECTED': return 'error';
      case 'CANCELLED': return 'default';
      default: return 'default';
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'COMPLAINT': return 'red';
      case 'INQUIRY': return 'blue';
      case 'DELIVERY_REMINDER': return 'gold';
      case 'PICKUP_REMINDER': return 'purple';
      case 'CHANGE_ORDER_INFO': return 'cyan';
      default: return 'default';
    }
  };

  

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Title level={2} style={{ color: "#1C3D90", marginBottom: 16 }}>Quản lý yêu cầu hỗ trợ / khiếu nại</Title>
      <Table rowKey="id" loading={loading} columns={columns} dataSource={requests} />
      <Modal
        title="Phân công cho bưu cục"
        open={assignModal}
        onOk={handleAssignOk}
        onCancel={() => setAssignModal(false)}
        okButtonProps={{ disabled: !selectedOffice }}
      >
        <Select
          style={{ width: "100%" }}
          placeholder="Chọn bưu cục"
          value={selectedOffice ?? undefined}
          onChange={setSelectedOffice}
        >
          {offices.map((o) => (
            <Option key={o.id} value={o.id}>
              {o.name}
            </Option>
          ))}
        </Select>
      </Modal>
    </div>
  );
};

export default ShippingRequestsAdmin;

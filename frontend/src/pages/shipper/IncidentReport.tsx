import React, { useState, useEffect } from "react";
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Typography,
  message,
  Modal,
  Table,
  Tag,
} from "antd";
import {
  PlusOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi from "../../api/orderApi";
import dayjs from "dayjs";

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface IncidentReport {
  id: number;
  orderId?: number;
  trackingNumber?: string;
  incidentType?: string;
  title: string;
  description?: string;
  priority?: string;
  status?: string;
  createdAt: string;
  handledAt?: string;
}

const ShipperIncidentReport: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [submitModal, setSubmitModal] = useState(false);
  const [selectedImages, setSelectedImages] = useState<string[]>([]);

  useEffect(() => {
    fetchReports();
  }, []);

  const fetchReports = async () => {
    try {
      setLoading(true);
      const response = await orderApi.getShipperIncidents();
      setReports(response || []);
    } catch (error) {
      console.error("Error fetching incident reports:", error);
      message.error("Lỗi khi tải danh sách báo cáo sự cố");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitReport = async (values: any) => {
    try {
      setLoading(true);
      await orderApi.createShipperIncident({
        orderId: values.orderId,
        incidentType: values.incidentType,
        title: values.title,
        description: values.description,
        priority: values.priority,
      });

      message.success("Đã gửi báo cáo sự cố thành công");
      setSubmitModal(false);
      form.resetFields();
      setSelectedImages([]);
      fetchReports();
    } catch (error) {
      message.error("Lỗi khi gửi báo cáo sự cố");
    } finally {
      setLoading(false);
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority?.toLowerCase()) {
      case "low":
        return "green";
      case "medium":
        return "orange";
      case "high":
        return "red";
      default:
        return "default";
    }
  };

  const getPriorityText = (priority: string) => {
    switch (priority?.toLowerCase()) {
      case "low":
        return "Thấp";
      case "medium":
        return "Trung bình";
      case "high":
        return "Cao";
      default:
        return priority;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status?.toUpperCase()) {
      case "PENDING":
        return "orange";
      case "PROCESSING":
        return "blue";
      case "RESOLVED":
        return "success";
      case "REJECTED":
        return "red";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status?.toUpperCase()) {
      case "PENDING":
        return "Chờ xử lý";
      case "PROCESSING":
        return "Đang xử lý";
      case "RESOLVED":
        return "Đã giải quyết";
      case "REJECTED":
        return "Từ chối";
      default:
        return status;
    }
  };

  const getIncidentTypeText = (incidentType: string) => {
    switch (incidentType?.toUpperCase()) {
      case "RECIPIENT_NOT_AVAILABLE":
        return "Người nhận không có mặt";
      case "WRONG_ADDRESS":
        return "Sai địa chỉ";
      case "PACKAGE_DAMAGED":
        return "Hàng hóa bị hỏng";
      case "RECIPIENT_REFUSED":
        return "Người nhận từ chối";
      case "SECURITY_ISSUE":
        return "Vấn đề an ninh";
      case "OTHER":
        return "Khác";
      default:
        return incidentType;
    }
  };

  const columns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => <Text strong style={{ fontSize: "13px" }}>{text || "—"}</Text>,
    },
    {
      title: "Tiêu đề",
      dataIndex: "title",
      key: "title",
      width: 200,
      render: (text: string) => <Text ellipsis style={{ maxWidth: 200 }}>{text}</Text>,
    },
    {
      title: "Loại sự cố",
      dataIndex: "incidentType",
      key: "incidentType",
      width: 150,
      render: (type: string) => getIncidentTypeText(type || ""),
    },
    {
      title: "Mức độ",
      dataIndex: "priority",
      key: "priority",
      width: 100,
      render: (priority: string) => <Tag color={getPriorityColor(priority || "")}>{getPriorityText(priority || "")}</Tag>,
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      width: 120,
      render: (status: string) => <Tag color={getStatusColor(status || "")}>{getStatusText(status || "")}</Tag>,
    },
    {
      title: "Ngày tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 150,
      render: (date: string) => (date ? dayjs(date).format("DD/MM/YYYY HH:mm") : "—"),
    },
  ];

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Title level={2} style={{ color: "#1C3D90", margin: 0 }}>
          Báo cáo sự cố
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setSubmitModal(true)}>
          Tạo báo cáo mới
        </Button>
      </div>

      <Card>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={reports}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title="Tạo báo cáo sự cố"
        open={submitModal}
        onOk={() => form.submit()}
        onCancel={() => {
          setSubmitModal(false);
          form.resetFields();
          setSelectedImages([]);
        }}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmitReport}>
          <Form.Item name="orderId" label="ID Đơn hàng" rules={[{ required: true, message: "Vui lòng nhập ID đơn hàng" }]}>
            <Input type="number" placeholder="Nhập ID đơn hàng" />
          </Form.Item>
          <Form.Item name="incidentType" label="Loại sự cố" rules={[{ required: true, message: "Vui lòng chọn loại sự cố" }]}>
            <Select placeholder="Chọn loại sự cố">
              <Option value="RECIPIENT_NOT_AVAILABLE">Người nhận không có mặt</Option>
              <Option value="WRONG_ADDRESS">Sai địa chỉ</Option>
              <Option value="PACKAGE_DAMAGED">Hàng hóa bị hỏng</Option>
              <Option value="RECIPIENT_REFUSED">Người nhận từ chối</Option>
              <Option value="SECURITY_ISSUE">Vấn đề an ninh</Option>
              <Option value="OTHER">Khác</Option>
            </Select>
          </Form.Item>
          <Form.Item name="title" label="Tiêu đề" rules={[{ required: true, message: "Vui lòng nhập tiêu đề" }]}>
            <Input placeholder="Nhập tiêu đề báo cáo" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả chi tiết">
            <TextArea rows={4} placeholder="Mô tả chi tiết về sự cố" />
          </Form.Item>
          <Form.Item name="priority" label="Mức độ ưu tiên" initialValue="MEDIUM">
            <Select>
              <Option value="LOW">Thấp</Option>
              <Option value="MEDIUM">Trung bình</Option>
              <Option value="HIGH">Cao</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ShipperIncidentReport;

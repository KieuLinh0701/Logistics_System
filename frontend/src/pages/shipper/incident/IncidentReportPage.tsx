import React, {useEffect, useState} from "react";
import {Button, Form, Input, message, Modal, Select, Spin, Upload,} from "antd";
import {PlusOutlined, ReloadOutlined} from "@ant-design/icons";
import orderApi from "../../../api/orderApi";
import "../../../styles/ListPage.css";
import "../ShipperPagesShared.css";
import IncidentReportsTable from "./components/IncidentReportsTable";

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

interface OrderOption {
  id: number;
  trackingNumber: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  status: string;
}

const IncidentReportPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [submitModal, setSubmitModal] = useState(false);
  const [selectedImages, setSelectedImages] = useState<File[]>([]);
  const [uploadList, setUploadList] = useState<any[]>([]);
  const [orderOptions, setOrderOptions] = useState<OrderOption[]>([]);
  const [orderLoading, setOrderLoading] = useState(false);

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

  useEffect(() => {
    fetchReports();
  }, []);

  // Fetch orders cho Select khi mở modal
  useEffect(() => {
    if (submitModal) {
      fetchOrderOptions();
    }
  }, [submitModal]);

  const fetchOrderOptions = async () => {
    setOrderLoading(true);
    try {
      // Lấy tất cả orders của shipper (limit cao để hiển thị nhiều options)
      const response = await orderApi.getShipperOrders({ page: 1, limit: 200 });
      const orders = response?.orders || [];

      // Map sang OrderOption format
      const options: OrderOption[] = orders.map((order: any) => ({
        id: order.id,
        trackingNumber: order.trackingNumber,
        recipientName: order.recipientName,
        recipientPhone: order.recipientPhone || '',
        recipientAddress: order.recipientFullAddress || `${order.recipientDetail || ''}, ${order.recipientWardName || ''}, ${order.recipientCityName || ''}`.replace(/^, |, $/g, ''),
        status: order.status,
      }));

      setOrderOptions(options);
    } catch (error) {
      console.error("Error fetching orders for select:", error);
      message.error("Lỗi khi tải danh sách đơn hàng");
    } finally {
      setOrderLoading(false);
    }
  };

  // Filter orders theo search text (mã vận đơn, tên, số điện thoại)
  const filterOrderOptions = (input: string, option: any) => {
    if (!input || input.length < 2) return true; // Hiển thị tất cả nếu chưa search
    const searchLower = input.toLowerCase();
    const order: OrderOption = option.orderData;

    const matchTracking = order.trackingNumber?.toLowerCase().includes(searchLower);
    const matchName = order.recipientName?.toLowerCase().includes(searchLower);
    const matchPhone = order.recipientPhone?.toLowerCase().includes(searchLower);

    return matchTracking || matchName || matchPhone;
  };

  // Format hiển thị option
  const orderOptionLabel = (order: OrderOption) => {
    const shortAddress = order.recipientAddress.length > 40
      ? order.recipientAddress.substring(0, 40) + "..."
      : order.recipientAddress;
    return `${order.trackingNumber} - ${order.recipientName} - ${shortAddress}`;
  };

  // Tạo Option children cho Select
  const orderOptionsList = orderOptions.map((order) => (
    <Option
      key={order.id}
      value={order.id}
      orderData={order}
      label={orderOptionLabel(order)}
    >
      <div style={{ padding: '4px 0' }}>
        <div style={{ fontWeight: 500 }}>{order.trackingNumber}</div>
        <div style={{ fontSize: 12, color: '#888' }}>
          {order.recipientName}
        </div>
        <div style={{ fontSize: 11, color: '#aaa' }}>
          {order.recipientAddress.length > 50
            ? order.recipientAddress.substring(0, 50) + "..."
            : order.recipientAddress}
        </div>
      </div>
    </Option>
  ));

  const handleSubmitReport = async (values: any) => {
    try {
      setLoading(true);

      const orderIdToSend = values.orderId;
      if (!orderIdToSend) {
        message.error("Vui lòng chọn đơn hàng");
        setLoading(false);
        return;
      }

      const fd = new FormData();
      fd.append("orderId", String(orderIdToSend));
      if (values.incidentType) fd.append("incidentType", values.incidentType);
      fd.append("title", values.title);
      if (values.description) fd.append("description", values.description);
      if (values.priority) fd.append("priority", values.priority);
      selectedImages.forEach((f) => fd.append("images", f));

      await orderApi.createShipperIncident(fd);

      message.success("Đã gửi báo cáo sự cố thành công");
      setSubmitModal(false);
      form.resetFields();
      setSelectedImages([]);
      setUploadList([]);
      fetchReports();
    } catch (error) {
      message.error("Lỗi khi gửi báo cáo sự cố");
    } finally {
      setLoading(false);
    }
  };

  const beforeUpload = (file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const result = e.target?.result as string;
      setSelectedImages((s) => [...s, file]);
      setUploadList((u) => [
        ...u,
        { uid: String(Date.now()), name: file.name, status: "done", url: result, originFileObj: file },
      ]);
    };
    reader.readAsDataURL(file);
    return false;
  };

  const handleRemoveImage = (file: any) => {
    setUploadList((u) => u.filter((it) => it.uid !== file.uid));
    setSelectedImages((s) => s.filter((f) => f.name !== file.name));
  };

  const closeModal = () => {
    setSubmitModal(false);
    form.resetFields();
    setSelectedImages([]);
    setUploadList([]);
  };

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel shipper-filter-panel--end">
          <div className="shipper-filter-actions">
            <Button icon={<ReloadOutlined />} onClick={fetchReports}>
              Làm mới
            </Button>
            <Button
              type="primary"
              className="primary-button"
              icon={<PlusOutlined />}
              onClick={() => setSubmitModal(true)}
            >
              Tạo báo cáo mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Báo cáo sự cố</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {reports.length} báo cáo</div>
            </div>
          </div>
        </div>

        <div className="list-page-table shipper-page-table">
          <IncidentReportsTable reports={reports} loading={loading} />
        </div>
      </div>

      <Modal
        title="Tạo báo cáo sự cố"
        open={submitModal}
        onOk={() => form.submit()}
        onCancel={closeModal}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmitReport}>
          <Form.Item
            name="orderId"
            label="Đơn hàng"
            rules={[{ required: true, message: "Vui lòng chọn đơn hàng" }]}
          >
            <Select
              showSearch
              placeholder="Chọn hoặc tìm đơn hàng"
              filterOption={filterOrderOptions}
              notFoundContent={orderLoading ? <Spin size="small" /> : "Không tìm thấy đơn hàng"}
              loading={orderLoading}
              allowClear
            >
              {orderOptionsList}
            </Select>
          </Form.Item>
          <Form.Item
            name="incidentType"
            label="Loại sự cố"
            rules={[{ required: true, message: "Vui lòng chọn loại sự cố" }]}
          >
            <Select placeholder="Chọn loại sự cố">
              <Option value="RECIPIENT_NOT_AVAILABLE">Người nhận không có mặt</Option>
              <Option value="WRONG_ADDRESS">Sai địa chỉ</Option>
              <Option value="PACKAGE_DAMAGED">Hàng hóa bị hỏng</Option>
              <Option value="RECIPIENT_REFUSED">Người nhận từ chối</Option>
              <Option value="SECURITY_ISSUE">Vấn đề an ninh</Option>
              <Option value="OTHER">Khác</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="title"
            label="Tiêu đề"
            rules={[{ required: true, message: "Vui lòng nhập tiêu đề" }]}
          >
            <Input placeholder="Nhập tiêu đề báo cáo" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả chi tiết">
            <TextArea rows={4} placeholder="Mô tả chi tiết về sự cố" />
          </Form.Item>
          <Form.Item label="Ảnh (nếu có)">
            <Upload
              listType="picture-card"
              beforeUpload={beforeUpload}
              onRemove={handleRemoveImage}
              fileList={uploadList}
              accept="image/*"
            >
              {uploadList.length >= 5 ? null : (
                <div>
                  <PlusOutlined />
                  <div style={{ marginTop: 8 }}>Thêm ảnh</div>
                </div>
              )}
            </Upload>
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

export default IncidentReportPage;

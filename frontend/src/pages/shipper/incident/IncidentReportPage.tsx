import React, { useEffect, useState } from "react";
import {
  Button,
  Form,
  Input,
  message,
  Modal,
  Select,
  Upload,
} from "antd";
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons";
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

const IncidentReportPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState<IncidentReport[]>([]);
  const [submitModal, setSubmitModal] = useState(false);
  const [selectedImages, setSelectedImages] = useState<File[]>([]);
  const [uploadList, setUploadList] = useState<any[]>([]);

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

      let orderIdToSend: number | undefined = undefined;
      const idOrTracking = values.orderId;
      if (!idOrTracking) {
        message.error("Vui lòng nhập mã vận đơn hoặc ID đơn hàng");
        setLoading(false);
        return;
      }

      if (!isNaN(Number(idOrTracking))) {
        orderIdToSend = Number(idOrTracking);
      } else {
        try {
          const sh = await orderApi.getShipperOrderByTrackingNumber(idOrTracking);
          if (sh && sh.id) {
            orderIdToSend = sh.id;
          }
        } catch (e) {
          try {
            const res = await orderApi.getUserOrderByTrackingNumber(idOrTracking);
            if (res && res.data && res.data.id) {
              orderIdToSend = res.data.id;
            }
          } catch (e2) {
            try {
              await orderApi.getPublicOrderByTrackingNumber(idOrTracking);
            } catch (e3) {
              // ignore
            }
          }
        }
      }

      if (!orderIdToSend) {
        message.error("Không tìm thấy đơn hàng với mã/ID cung cấp");
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
            label="Mã vận đơn"
            rules={[{ required: true, message: "Vui lòng nhập mã vận đơn hoặc ID đơn hàng" }]}
          >
            <Input placeholder="Nhập mã vận đơn hoặc ID đơn hàng" />
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

import React, { useEffect, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Typography,
  message,
} from "antd";
import shipperVehicleApi from "../../api/shipperVehicleApi";
import type {
  ShipperVehicleSetting,
  ShipperVehicleStatus,
  ShipperVehicleType,
} from "../../types/shipperVehicle";

const { Text } = Typography;

interface VehicleFormValues {
  vehicleType: ShipperVehicleType;
  maxOrders: number;
  maxWeightKg: number;
  batteryLevel?: number;
  status: ShipperVehicleStatus;
  notes?: string;
}

const ShipperVehicleSettingPage: React.FC = () => {
  const [form] = Form.useForm<VehicleFormValues>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [data, setData] = useState<ShipperVehicleSetting | null>(null);

  const vehicleType = Form.useWatch("vehicleType", form);
  const status = Form.useWatch("status", form);
  const batteryLevel = Form.useWatch("batteryLevel", form);

  const isElectric = vehicleType === "ELECTRIC_BIKE";

  const vehicleTypeLabel = vehicleType === "ELECTRIC_BIKE" ? "Xe điện" : "Xe máy";
  const statusLabel = status === "ACTIVE" ? "Hoạt động" : status === "MAINTENANCE" ? "Bảo trì" : "Tạm nghỉ";

  const maxOrders = Form.useWatch("maxOrders", form) ?? data?.maxOrders ?? 0;
  const maxWeightKg = Form.useWatch("maxWeightKg", form) ?? data?.maxWeightKg ?? 0;
  const currentOrders = data?.currentOrders ?? 0;
  const currentWeightKg = data?.currentWeightKg ?? 0;

  const ordersPercent = maxOrders > 0 ? Math.min(100, Math.round((currentOrders / maxOrders) * 100)) : 0;
  const weightPercent = maxWeightKg > 0 ? Math.min(100, Math.round((currentWeightKg / maxWeightKg) * 100)) : 0;

  const showBattery = isElectric && batteryLevel != null;

  const loadData = async () => {
    try {
      setLoading(true);
      const res = await shipperVehicleApi.getVehicleSetting();
      setData(res);
      form.setFieldsValue({
        vehicleType: res.vehicleType,
        maxOrders: res.maxOrders,
        maxWeightKg: res.maxWeightKg,
        batteryLevel: res.batteryLevel ?? undefined,
        status: res.status,
        notes: res.notes ?? "",
      });
    } catch {
      message.error("Không tải được cài đặt phương tiện");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleVehicleTypeChange = (nextType: ShipperVehicleType) => {
    if (nextType === "MOTORBIKE") {
      form.setFieldValue("batteryLevel", undefined);
      if (form.getFieldValue("maxOrders") == null) form.setFieldValue("maxOrders", 20);
      if (form.getFieldValue("maxWeightKg") == null) form.setFieldValue("maxWeightKg", 35);
      return;
    }
    if (form.getFieldValue("batteryLevel") == null) {
      form.setFieldValue("batteryLevel", 100);
    }
  };

  const handleSaveAll = async (values: VehicleFormValues) => {
    try {
      setSaving(true);
      const updated = await shipperVehicleApi.updateVehicleSetting({
        vehicleType: values.vehicleType,
        maxOrders: values.maxOrders,
        maxWeightKg: values.maxWeightKg,
        batteryLevel: values.vehicleType === "ELECTRIC_BIKE" ? values.batteryLevel ?? 100 : null,
        notes: values.notes ?? "",
      });

      if (data?.status !== values.status) {
        await shipperVehicleApi.updateVehicleStatus({ status: values.status });
      }

      setData({ ...updated, status: values.status });
      message.success("Đã lưu cài đặt phương tiện");
      await loadData();
    } catch {
      message.error("Lưu cài đặt thất bại");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="tab-content vehicle-settings-tab">
      <Card className="profile-form-card vehicle-settings-card" loading={loading}>
        <div className="vehicle-settings-summary">
          <div className="vehicle-settings-summary-grid">
            <div className="vehicle-settings-summary-item">
              <Text type="secondary">Loại xe</Text>
              <div className="vehicle-settings-summary-value">{vehicleTypeLabel}</div>
            </div>
            <div className="vehicle-settings-summary-item">
              <Text type="secondary">Trạng thái</Text>
              <div className="vehicle-settings-summary-value">{statusLabel}</div>
            </div>
            <div className="vehicle-settings-summary-item">
              <Text type="secondary">Tải hiện tại</Text>
              <div className="vehicle-settings-summary-value">
                {currentWeightKg}/{maxWeightKg} kg
              </div>
            </div>
            <div className="vehicle-settings-summary-item">
              <Text type="secondary">Đơn hiện tại</Text>
              <div className="vehicle-settings-summary-value">
                {currentOrders}/{maxOrders} đơn
              </div>
            </div>
            {showBattery && (
              <div className="vehicle-settings-summary-item">
                <Text type="secondary">Pin</Text>
                <div className="vehicle-settings-summary-value">{batteryLevel ?? 0}%</div>
              </div>
            )}
          </div>

          <div className="vehicle-settings-progress-grid">
            <div className="vehicle-settings-progress-item">
              <div className="vehicle-settings-progress-label">
                <Text>Tải trọng</Text>
                <Text type="secondary">{currentWeightKg}/{maxWeightKg} kg</Text>
              </div>
              <Progress percent={weightPercent} showInfo={false} strokeColor="#1C3D90" />
            </div>
            <div className="vehicle-settings-progress-item">
              <div className="vehicle-settings-progress-label">
                <Text>Số đơn</Text>
                <Text type="secondary">{currentOrders}/{maxOrders} đơn</Text>
              </div>
              <Progress percent={ordersPercent} showInfo={false} strokeColor="#1C3D90" />
            </div>
          </div>
        </div>

        {status && ["INACTIVE", "MAINTENANCE"].includes(status) && (
          <Alert
            type="warning"
            showIcon
            message="Bạn sẽ không được AI phân công đơn mới khi phương tiện không hoạt động."
            className="vehicle-settings-alert"
          />
        )}

        {isElectric && typeof batteryLevel === "number" && batteryLevel < 20 && (
          <Alert
            type="warning"
            showIcon
            message="Pin thấp, hệ thống có thể hạn chế phân công tuyến xa."
            className="vehicle-settings-alert"
          />
        )}

        <Form
          layout="vertical"
          form={form}
          onFinish={handleSaveAll}
          initialValues={{
            vehicleType: "MOTORBIKE",
            maxOrders: 20,
            maxWeightKg: 35,
            status: "ACTIVE",
            notes: "",
          }}
          className="vehicle-settings-form"
        >
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item label="Loại phương tiện" name="vehicleType" rules={[{ required: true, message: "Vui lòng chọn loại phương tiện" }]}>
                <Radio.Group onChange={(e) => handleVehicleTypeChange(e.target.value)}>
                  <Space direction="vertical">
                    <Radio value="MOTORBIKE">Xe máy</Radio>
                    <Radio value="ELECTRIC_BIKE">Xe điện</Radio>
                  </Space>
                </Radio.Group>
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Trạng thái phương tiện" name="status" rules={[{ required: true, message: "Vui lòng chọn trạng thái" }]}>
                <Select
                  options={[
                    { value: "ACTIVE", label: "Hoạt động" },
                    { value: "INACTIVE", label: "Tạm nghỉ" },
                    { value: "MAINTENANCE", label: "Bảo trì" },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item label="Số đơn tối đa" name="maxOrders" rules={[{ required: true, message: "Vui lòng nhập số đơn tối đa" }]}>
                <InputNumber min={1} max={100} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Khối lượng tối đa (kg)" name="maxWeightKg" rules={[{ required: true, message: "Vui lòng nhập khối lượng tối đa" }]}>
                <InputNumber min={1} max={100} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>

          {isElectric && (
            <Row gutter={16}>
              <Col xs={24} md={12}>
                <Form.Item
                  label="Pin hiện tại (%)"
                  name="batteryLevel"
                  rules={[{ required: true, message: "Vui lòng nhập mức pin" }]}
                >
                  <InputNumber min={0} max={100} style={{ width: "100%" }} />
                </Form.Item>
              </Col>
            </Row>
          )}

          <Form.Item label="Ghi chú" name="notes">
            <Input.TextArea rows={4} placeholder="Nhập ghi chú phương tiện (nếu có)" maxLength={1000} showCount />
          </Form.Item>

          <div className="vehicle-settings-actions">
            <Button type="primary" htmlType="submit" loading={saving} className="btn-primary">Lưu cài đặt</Button>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default ShipperVehicleSettingPage;

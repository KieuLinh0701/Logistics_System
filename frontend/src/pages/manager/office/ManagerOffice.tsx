import { useEffect, useState } from 'react';
import { Row, Col, Typography, Space, Tag, Form, Input, TimePicker, Select, InputNumber, Button, message, Tooltip } from 'antd';
import { BankOutlined, IdcardOutlined, PhoneOutlined, MailOutlined, ClockCircleOutlined, EditOutlined, CompassOutlined, FileTextOutlined, TagOutlined, UserOutlined, EnvironmentOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { Office, OfficeEditRequest } from '../../../types/office';
import officeApi from '../../../api/officeApi';
import './ManagerOffice.css';
import locationApi from '../../../api/locationApi';
import { OFFICE_STATUSES, translateOfficeStatus } from '../../../utils/officeUtils';

const { Title } = Typography;
const { Option } = Select;

const ManagerOffice = () => {
  const [office, setOffice] = useState<Office | null>(null);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [formattedAddress, setFormattedAddress] = useState<string>("");

  const fetchOffice = async () => {
    try {
      setLoading(true);
      const res = await officeApi.getManagerOffice();
      if (res.success && res.data) {
        setOffice(res.data);
        form.setFieldsValue({
          email: res.data.email,
          phoneNumber: res.data.phoneNumber,
          openingTime: res.data.openingTime ? dayjs(res.data.openingTime, "HH:mm") : null,
          closingTime: res.data.closingTime ? dayjs(res.data.closingTime, "HH:mm") : null,
          status: res.data.status,
          capacity: res.data.capacity,
          notes: res.data.notes
        });
      } else {
        message.error(res.message || 'Lỗi khi lấy thông tin bưu cục');
      }
    } catch (error: any) {
      message.error(error?.message || 'Có lỗi khi lấy thông tin bưu cục');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOffice();
  }, []);

  useEffect(() => {
    const formatAddress = async (detail: string | undefined, wardCode?: number, cityCode?: number) => {
      try {
        const cityName = cityCode ? (await locationApi.getCityNameByCode(cityCode)) : "";
        const wardName = cityCode && wardCode ? (await locationApi.getWardNameByCode(cityCode, wardCode)) : "";
        return [detail, wardName, cityName].filter(Boolean).join(", ");
      } catch (error) {
        console.error("Error formatting address:", error);
        return detail || "";
      }
    };

    if (office) {
      formatAddress(office.detail, office.wardCode, office.cityCode)
        .then((result) => setFormattedAddress(result));
    }
  }, [office]);

  const handleUpdate = async () => {
    try {
      const values = await form.validateFields();
      if (!office) return;

      const payload: OfficeEditRequest = {
        email: values.email,
        phoneNumber: values.phoneNumber,
        status: values.status,
        capacity: values.capacity,
        notes: values.notes,
        openingTime: values.openingTime ? dayjs(values.openingTime).format("HH:mm") : null,
        closingTime: values.closingTime ? dayjs(values.closingTime).format("HH:mm") : null,
      };

      const result = await officeApi.updateManagerOffice(payload);

      if (result.success && result.data) {
        message.success("Cập nhật thành công");

        setOffice((prev) => {
          if (!prev) return prev;

          return {
            ...prev, 
            email: payload.email ?? prev.email,
            phoneNumber: payload.phoneNumber ?? prev.phoneNumber,
            status: payload.status ?? prev.status,
            capacity: payload.capacity ?? prev.capacity,
            notes: payload.notes ?? prev.notes,
            openingTime: payload.openingTime ?? prev.openingTime,
            closingTime: payload.closingTime ?? prev.closingTime,
          };
        });

        form.setFieldsValue({
          email: payload.email,
          phoneNumber: payload.phoneNumber,
          status: payload.status,
          capacity: payload.capacity,
          notes: payload.notes,
          openingTime: payload.openingTime ? dayjs(payload.openingTime, "HH:mm") : null,
          closingTime: payload.closingTime ? dayjs(payload.closingTime, "HH:mm") : null,
        });
      } else {
        message.error(result.message || "Cập nhật thất bại");
      }
    } catch (error: any) {
      message.error(error?.message || 'Cập nhật thất bại!');
    }
  };


  if (loading) return <div className="manager-office-loading">Đang tải thông tin bưu cục...</div>;

  return (
    <div className="manager-office">
      <Row gutter={[24, 24]}>
        {/* Bên trái: thông tin chỉ đọc */}
        <Col xs={24} md={12}>
          <div className="manager-office-info-card">
            <div className="manager-office-header">
              <Title level={3} className="manager-office-title">
                <BankOutlined /> {office?.name || 'Chưa có tên bưu cục'}
              </Title>
              {office?.postalCode && (
                <Tag className="manager-office-code-tag">
                  <IdcardOutlined /> {office.postalCode}
                </Tag>
              )}
            </div>

            <Space direction="vertical" className="manager-office-details">
              <div className="manager-office-detail-item">
                <PhoneOutlined className="manager-office-detail-icon" />
                <span className="manager-office-detail-label">Số điện thoại:</span>
                <span className="manager-office-detail-value">{office?.phoneNumber || 'Chưa có'}</span>
              </div>

              <div className="manager-office-detail-item">
                <MailOutlined className="manager-office-detail-icon" />
                <span className="manager-office-detail-label">Email:</span>
                <span className="manager-office-detail-value">{office?.email || 'Chưa có'}</span>
              </div>

              <div className="manager-office-detail-item">
                <EnvironmentOutlined className="manager-office-detail-icon" />
                <span className="manager-office-detail-label">Địa chỉ:</span>
                {office?.latitude && office?.longitude ? (
                  <Tooltip title="Nhấn để mở Google Maps">
                    <span
                      className="manager-office-detail-value navigate-link"
                      onClick={() =>
                        window.open(
                          `https://www.google.com/maps?q=${office.latitude},${office.longitude}`,
                          "_blank",
                          "noopener,noreferrer"
                        )
                      }
                    >
                      {formattedAddress || 'Chưa có'}
                    </span>
                  </Tooltip>
                ) : (
                  <Tooltip title="Địa chỉ không có tọa độ">
                    <span className="manager-office-detail-value">{formattedAddress || 'Chưa có'}</span>
                  </Tooltip>
                )}
              </div>

              <div className="manager-office-detail-item">
                <UserOutlined className="manager-office-detail-icon" />
                <span className="manager-office-detail-label">Sức chứa:</span>
                <span className="manager-office-detail-value">
                  {office?.capacity ? `${office.capacity} đơn hàng` : 'Chưa có'}
                </span>
              </div>


              <div className="manager-office-detail-item">
                <ClockCircleOutlined className="manager-office-detail-icon" />
                <span className="manager-office-detail-label">Giờ hoạt động:</span>
                <span className="manager-office-detail-value">{office?.openingTime || 'Chưa có'} - {office?.closingTime || 'Chưa có'}</span>
              </div>

              <div className="manager-office-detail-item">
                <TagOutlined className="manager-office-detail-icon" />
                <span className="manager-office-detail-label">Trạng thái:</span>
                <span className="manager-office-detail-value">{office ? translateOfficeStatus(office.status) : 'Chưa có'}</span>
              </div>

              {office?.notes && (
                <div className="manager-office-detail-item">
                  <FileTextOutlined className="manager-office-detail-icon" />
                  <span className="manager-office-detail-label">Ghi chú:</span>
                  <div className="manager-office-detail-value">{office.notes}</div>
                </div>
              )}
            </Space>
          </div>
        </Col>

        {/* Bên phải: form chỉnh sửa */}
        <Col xs={24} md={12}>
          <div className="manager-office-edit-card">
            <Title level={4} className="manager-office-edit-title">
              <EditOutlined /> Chỉnh sửa thông tin
            </Title>

            <Form form={form} layout="vertical" className="manager-office-edit-form">
              <Form.Item
                name="email"
                label="Email"
                rules={[
                  { required: true, message: 'Vui lòng nhập email!' },
                  { type: "email", message: "Email không hợp lệ!" },
                ]}
              >
                <Input className="manager-office-input" />
              </Form.Item>

              <Form.Item
                name="phoneNumber"
                label="Số điện thoại"
                rules={[
                  { required: true, message: "Vui lòng nhập số điện thoại!" },
                  { pattern: /^[0-9]{10,11}$/, message: "Số điện thoại không hợp lệ!" },
                ]}
              >
                <Input className="manager-office-input" />
              </Form.Item>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="openingTime"
                    label="Giờ mở cửa"
                    rules={[{ required: true, message: 'Vui lòng nhập giờ mở cửa!' }]}>
                    <TimePicker
                      className="manager-office-date-picker"
                      format="HH:mm"
                      style={{ width: '100%' }}
                    />
                  </Form.Item>
                </Col>

                <Col span={12}>
                  <Form.Item
                    name="closingTime"
                    label="Giờ đóng cửa"
                    rules={[{ required: true, message: 'Vui lòng nhập giờ đóng cửa!' }]}>
                    <TimePicker
                      className="manager-office-date-picker"
                      format="HH:mm"
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="status"
                    label="Trạng thái"
                    rules={[{ required: true, message: 'Vui lòng chọn trạng thái!' }]}>
                    <Select className="manager-office-select">
                      {OFFICE_STATUSES.map((status) => (
                        <Option key={status} value={status}>
                          {translateOfficeStatus(status)}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>

                <Col span={12}>
                  <Form.Item
                    name="capacity"
                    label="Sức chứa (đơn hàng)"
                    rules={[{ required: true, message: 'Vui lòng nhập số đơn hàng có thể chứa của bưu cục!' }]}>
                    <InputNumber
                      className="manager-office-input-number"
                      min={0}
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="notes" label="Ghi chú">
                <Input.TextArea
                  className="manager-office-textarea"
                  rows={3}
                />
              </Form.Item>


              <Button
                type="primary"
                onClick={handleUpdate}
                icon={<EditOutlined />}
                className="manager-office-update-button"
                size="large"
              >
                Cập nhật thông tin
              </Button>
            </Form>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default ManagerOffice;
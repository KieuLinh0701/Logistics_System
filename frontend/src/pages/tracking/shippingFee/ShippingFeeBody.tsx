import React, { useEffect, useState } from "react";
import { Form, InputNumber, Select, Button, Typography, Row, Col, message } from "antd";
import axios from "axios";
import type { City } from "../../../types/location";
import type { ServiceType } from "../../../types/serviceType";
import serviceTypeApi from "../../../api/serviceTypeApi";
import "./ShippingFee.css";
import shippingFeeHeroImage from "../../../assets/images/serviceTypes/serviceTypes.jpg";
import Paragraph from "antd/es/typography/Paragraph";
import locationApi from "../../../api/locationApi";
import shippingFeeApi from "../../../api/shippingFeeApi";

const { Option } = Select;
const { Title, Text } = Typography;

const ShippingFeeBody: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(true);
  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);
  const [cities, setCities] = useState<City[]>([]);
  const [selectedServiceType, setSelectedServiceType] = useState<ServiceType | null>(null);
  const [shippingFeeResult, setShippingFeeResult] = useState<number | null>(null);

  const fetchCities = async () => {
    try {
      setLoading(true);
      const response = await locationApi.getCities();
      if (response) {
        setCities(response);
      }
    } catch (error) {
      console.error("Error fetching Cities:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCities();
  }, []);

  const fetchServiceTypes = async () => {
    try {
      setLoading(true);
      const response = await serviceTypeApi.getActiveServiceTypes();
      if (response.success && response.data) {
        setServiceTypes(response.data);
      }
    } catch (error) {
      console.error("Error fetching Service types:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServiceTypes();
  }, []);

  const calculateShippingFee = async (values: any) => {
    if (!selectedServiceType) {
      message.error("Vui lòng chọn loại dịch vụ");
      return;
    }

    try {
      const result = await shippingFeeApi.calculateShippingFee({
          weight: values.weight,
          serviceTypeId: selectedServiceType.id,
          senderCodeCity: values.fromProvince,
          recipientCodeCity: values.toProvince,
        });

      if (result.data) {
        setShippingFeeResult(result.data);
      }
    } catch (error) {
      message.error("Tính cước thất bại");
      console.error(error);
    }
  };

  return (
    <div className="shipping-fee-page">
      {/* Hero Section */}
      <div
        className="shipping-fee-hero"
        style={{
          backgroundImage: `url(${shippingFeeHeroImage})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
        }}
      >
        <div className="shipping-fee-hero-overlay">
          <div className="shipping-fee-hero-content">
            <Title level={2} className="shipping-fee-hero-title">
              Tra Cứu Cước Phí
            </Title>
            <Paragraph className="shipping-fee-hero-subtitle">
              Tính toán chi phí vận chuyển nhanh chóng và chính xác
            </Paragraph>
          </div>
        </div>
      </div>

      <div className="shipping-fee-container">
        <div className="shipping-fee-calculator-section">
          <Form form={form} layout="vertical" className="shipping-fee-calculator-form">
            <Row gutter={24} className="shipping-fee-form-row">
              <Col xs={24} md={12}>
                <Form.Item
                  name="fromProvince"
                  label={<span className="shipping-form-label">Gửi từ</span>}
                  rules={[{ required: true, message: "Chọn tỉnh/thành phố gửi hàng!" }]}
                >
                  <Select
                    placeholder="Tỉnh/thành phố"
                    className="shipping-form-input"
                    size="large"
                  >
                    {cities.map((c) => (
                      <Option key={c.code} value={c.code}>
                        {c.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>

              <Col xs={24} md={12}>
                <Form.Item
                  name="toProvince"
                  label={<span className="shipping-form-label">Gửi đến</span>}
                  rules={[{ required: true, message: "Chọn tỉnh/thành phố nhận hàng!" }]}
                >
                  <Select
                    placeholder="Tỉnh/thành phố"
                    className="shipping-form-input"
                    size="large"
                  >
                    {cities.map((c) => (
                      <Option key={c.code} value={c.code}>
                        {c.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={24} className="shipping-fee-form-row">
              <Col xs={24} md={12}>
                <Form.Item
                  name="weight"
                  label={<span className="shipping-form-label">Khối lượng (kg)</span>}
                  rules={[{ required: true, message: "Nhập khối lượng!" }]}
                >
                  <InputNumber
                    className="shipping-form-input"
                    min={0}
                    placeholder="Ví dụ: 0.5"
                    step={0.1}
                    size="large"
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Col>

              <Col xs={24} md={12}>
                <Form.Item
                  name="serviceType"
                  label={<span className="shipping-form-label">Loại dịch vụ giao hàng</span>}
                  rules={[{ required: true, message: "Chọn loại dịch vụ!" }]}
                >
                  <Select
                    placeholder="Chọn dịch vụ"
                    className="shipping-form-input"
                    size="large"
                    onChange={(value) => {
                      const selected = serviceTypes?.find((s) => s.id === value);
                      setSelectedServiceType(selected || null);
                      form.setFieldsValue({ serviceType: value });
                    }}
                  >
                    {loading && <Option value="" disabled>Đang tải...</Option>}
                    {serviceTypes?.map((s) => (
                      <Option key={s.id} value={s.id}>
                        {s.name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Form.Item>
              <Button
                type="primary"
                className="shipping-fee-calculate-btn"
                onClick={() =>
                  form
                    .validateFields()
                    .then(calculateShippingFee)
                    .catch(() => { })
                }
              >
                Tra cứu
              </Button>
            </Form.Item>

            {shippingFeeResult !== null && (
              <div className="shipping-fee-result-section">
                <Text strong className="shipping-fee-result-title">TỔNG TIỀN CƯỚC VẬN CHUYỂN ƯỚC TÍNH</Text>
                <div className="shipping-fee-result-amount">
                  {shippingFeeResult.toLocaleString()} VNĐ
                </div>
                <Text strong className="shipping-fee-result-time">
                  Thời gian giao hàng từ {selectedServiceType?.deliveryTime}
                </Text>
              </div>
            )}
          </Form>
        </div>
      </div>
    </div>
  );
};

export default ShippingFeeBody;
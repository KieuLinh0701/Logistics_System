import React from "react";
import { Card, Typography, Button, Spin } from "antd";
import { CheckCircleOutlined, SafetyCertificateOutlined, StarOutlined } from "@ant-design/icons";
import type { ServiceType } from "../../../../types/serviceType";

const { Title, Text } = Typography;

interface ShippingServicesProps {
  loading: boolean;
  services: ServiceType[];
  onViewAllDetails: () => void;
}

const ShippingServices: React.FC<ShippingServicesProps> = ({
  loading,
  services = [],
  onViewAllDetails
}) => {

  const serviceList = services;

  if (loading) {
    return (
      <div className="home-section home-loading">
        <Spin size="large" tip="Đang tải dịch vụ..." />
      </div>
    );
  }

  return (
    <div className="home-section">
      <div className="home-container">
        {/* Header với tiêu đề và nút xem chi tiết */}
        <div className="home-section-header">
          <div>
            <Title level={2} className="home-section-title">
              Dịch Vụ Giao Hàng
            </Title>
            <Text style={{ color: '#666', fontSize: '1.1rem' }}>
              Lựa chọn giải pháp vận chuyển phù hợp nhất với nhu cầu của bạn
            </Text>
          </div>
          <Button
            type="default"
            onClick={onViewAllDetails}
            className="home-detail-link"
          >
            Xem tất cả dịch vụ ›
          </Button>
        </div>

        {/* Grid dịch vụ */}
        <div className="home-grid">
          {serviceList.map((service) => (
            <Card
              key={service.id}
              className="home-card"
              onClick={() => console.log(`Selected: ${service.name}`)}
            >
              <Title level={4} className="home-card-title">
                {service.name}
              </Title>
              <Text className="home-card-description">
                Thời gian giao hàng từ {service.deliveryTime}
              </Text>
            </Card>
          ))}
        </div>

        {/* Features */}
        <div className="home-features">
          <div className="home-feature">
            <CheckCircleOutlined className="home-feature-icon" />
            <span>Đảm bảo thời gian giao hàng</span>
          </div>
          <div className="home-feature">
            <SafetyCertificateOutlined className="home-feature-icon" />
            <span>Bảo hiểm hàng hóa đầy đủ</span>
          </div>
          <div className="home-feature">
            <StarOutlined className="home-feature-icon" />
            <span>Hỗ trợ 24/7</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ShippingServices;
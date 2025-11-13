import React from "react";
import { Card, Typography, Button } from "antd";
import "./ShippingServices.css";
import { CheckCircleOutlined, SafetyCertificateOutlined, StarOutlined } from "@ant-design/icons";
import type { ServiceType } from "../../../types/serviceType";

const { Title, Text } = Typography;

interface ShippingServicesProps {
  services: ServiceType[];
  onViewAllDetails: () => void;
}

const ShippingServices: React.FC<ShippingServicesProps> = ({ 
  services = [],
  onViewAllDetails 
}) => {

  const serviceList = services;

  return (
    <div className="shipping-section">
      <div className="shipping-container">
        {/* Header với tiêu đề và nút xem chi tiết */}
        <div className="shipping-section-header">
          <div>
            <Title level={2} className="shipping-section-title">
              Dịch Vụ Giao Hàng
            </Title>
            <Text style={{ color: '#666', fontSize: '1.1rem' }}>
              Lựa chọn giải pháp vận chuyển phù hợp nhất với nhu cầu của bạn
            </Text>
          </div>
          <Button 
            type="default"
            onClick={onViewAllDetails}
            className="shipping-detail-link"
          >
            Xem tất cả dịch vụ ›
          </Button>
        </div>

        {/* Grid dịch vụ */}
        <div className="shipping-services-grid">
          {serviceList.map((service) => (
            <Card
              key={service.id}
              className="shipping-service-card"
              onClick={() => console.log(`Selected: ${service.name}`)}
            >
              <Title level={4} className="shipping-card-title">
                {service.name}
              </Title>
              <Text className="shipping-card-description">
                Thời gian giao hàng từ {service.deliveryTime}
              </Text>
            </Card>
          ))}
        </div>

        {/* Features */}
        <div className="shipping-features">
          <div className="shipping-feature">
            <CheckCircleOutlined className="shipping-feature-icon" />
            <span>Đảm bảo thời gian giao hàng</span>
          </div>
          <div className="shipping-feature">
            <SafetyCertificateOutlined className="shipping-feature-icon" />
            <span>Bảo hiểm hàng hóa đầy đủ</span>
          </div>
          <div className="shipping-feature">
            <StarOutlined className="shipping-feature-icon" />
            <span>Hỗ trợ 24/7</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ShippingServices;
import React from "react";
import { Card, Typography, Button } from "antd";
import "./ShippingServices.css";
import { CheckCircleOutlined, SafetyCertificateOutlined, StarOutlined } from "@ant-design/icons";

const { Title, Text } = Typography;

export interface ShippingService {
  id: string;
  name: string;
  description: string;
}

interface ShippingServicesProps {
  services?: ShippingService[];
  onViewAllDetails?: () => void;
}

const ShippingServices: React.FC<ShippingServicesProps> = ({ 
  services = [],
  onViewAllDetails 
}) => {
  const defaultServices: ShippingService[] = [
    {
      id: "standard",
      name: "Giao Hàng Tiêu Chuẩn",
      description: "Phù hợp cho hàng hóa thông thường, thời gian giao hàng linh hoạt",
    },
    {
      id: "express", 
      name: "Giao Hàng Nhanh",
      description: "Ưu tiên xử lý, phù hợp cho hàng hóa cần giao gấp",
    },
    {
      id: "flash",
      name: "Giao Hàng Hỏa Tốc",
      description: "Dịch vụ cao cấp nhất, cam kết thời gian giao hàng nhanh chóng",
    }
  ];

  const serviceList = services.length > 0 ? services : defaultServices;

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
                {service.description}
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
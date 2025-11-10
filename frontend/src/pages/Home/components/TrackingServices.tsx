import React from "react";
import { Card, Typography, Button } from "antd";
import {
  DollarOutlined,
  EnvironmentOutlined,
  FileSearchOutlined,
  ProfileOutlined,
} from "@ant-design/icons";
import "./TrackingService.css";

const { Title, Text } = Typography;

const TrackingServices: React.FC = () => {
  return (
    <div className="tracking-section">
      <div className="tracking-container">
        {/* Section Header */}
        <div className="tracking-section-header">
          <div>
            <Title level={2} className="tracking-section-title">
              Tra Cứu & Tính Toán
            </Title>
            <Text style={{ color: '#666', fontSize: '1.1rem' }}>
              Các công cụ hỗ trợ giúp bạn quản lý và theo dõi đơn hàng hiệu quả
            </Text>
          </div>
        </div>

        {/* Services Grid */}
        <div className="tracking-services-grid">
          {/* Cước phí */}
          <Card
            hoverable
            className="tracking-service-card"
            onClick={() => (window.location.href = "/tracking/shipping-fee")}
          >
            <div className="tracking-card-icon-wrapper">
              <DollarOutlined className="tracking-service-icon" />
            </div>
            <Title level={4} className="tracking-card-title">Tính Cước Phí</Title>
            <Text className="tracking-card-description">
              Tính toán chi phí vận chuyển dựa trên khối lượng, kích thước và khoảng cách
            </Text>
            <Button type="primary" className="tracking-service-btn">
              Tính Phí Ngay
            </Button>
          </Card>

          {/* Bưu cục */}
          <Card
            hoverable
            className="tracking-service-card"
            onClick={() => (window.location.href = "/tracking/office-search")}
          >
            <div className="tracking-card-icon-wrapper">
              <EnvironmentOutlined className="tracking-service-icon" />
            </div>
            <Title level={4} className="tracking-card-title">Tìm Bưu Cục</Title>
            <Text className="tracking-card-description">
              Khám phá mạng lưới bưu cục rộng khắp với đầy đủ thông tin chi tiết và vị trí
            </Text>
            <Button type="primary" className="tracking-service-btn">
              Tìm Kiếm
            </Button>
          </Card>

          {/* Vận đơn */}
          <Card
            hoverable
            className="tracking-service-card"
            onClick={() => (window.location.href = "/tracking/order-tracking")}
          >
            <div className="tracking-card-icon-wrapper">
              <FileSearchOutlined className="tracking-service-icon" />
            </div>
            <Title level={4} className="tracking-card-title">Tra Cứu Vận Đơn</Title>
            <Text className="tracking-card-description">
              Theo dõi trạng thái và lịch trình giao hàng theo thời gian thực với mã vận đơn
            </Text>
            <Button type="primary" className="tracking-service-btn">
              Tra Cứu Ngay
            </Button>
          </Card>

          {/* Bảng giá */}
          <Card
            hoverable
            className="tracking-service-card"
            onClick={() => (window.location.href = "/info/shipping-rates")}
          >
            <div className="tracking-card-icon-wrapper">
              <ProfileOutlined className="tracking-service-icon" />
            </div>
            <Title level={4} className="tracking-card-title">Bảng Giá Dịch Vụ</Title>
            <Text className="tracking-card-description">
              Xem chi tiết bảng giá cạnh tranh cho tất cả các dịch vụ vận chuyển
            </Text>
            <Button type="primary" className="tracking-service-btn">
              Xem Bảng Giá
            </Button>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default TrackingServices;
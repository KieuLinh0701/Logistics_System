import React from "react";
import { Card, Typography, Button } from "antd";
import {
  DollarOutlined,
  EnvironmentOutlined,
  FileSearchOutlined,
  ProfileOutlined,
} from "@ant-design/icons";

const { Title, Text } = Typography;

const TrackingServices: React.FC = () => {
  return (
    <div className="home-section">
      <div className="home-container">
        {/* Section Header */}
        <div className="home-section-header">
          <div>
            <Title level={2} className="home-section-title">
              Tra Cứu & Tính Toán
            </Title>
            <Text style={{ color: '#666', fontSize: '1.1rem' }}>
              Các công cụ hỗ trợ giúp bạn quản lý và theo dõi đơn hàng hiệu quả
            </Text>
          </div>
        </div>

        {/* Services Grid */}
        <div className="home-grid">
          {/* Cước phí */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/tracking/shipping-fee")}
          >
            <div className="home-card-icon-wrapper">
              <DollarOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Tính Cước Phí</Title>
            <Text className="home-card-description">
              Tính toán chi phí vận chuyển dựa trên khối lượng, kích thước và khoảng cách
            </Text>
            <Button type="primary" className="home-card-btn">
              Tính Phí Ngay
            </Button>
          </Card>

          {/* Bưu cục */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/tracking/office-search")}
          >
            <div className="home-card-icon-wrapper">
              <EnvironmentOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Tìm Bưu Cục</Title>
            <Text className="home-card-description">
              Khám phá mạng lưới bưu cục rộng khắp với đầy đủ thông tin chi tiết và vị trí
            </Text>
            <Button type="primary" className="home-card-btn">
              Tìm Kiếm
            </Button>
          </Card>

          {/* Vận đơn */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/tracking/order-tracking")}
          >
            <div className="home-card-icon-wrapper">
              <FileSearchOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Tra Cứu Vận Đơn</Title>
            <Text className="home-card-description">
              Theo dõi trạng thái và lịch trình giao hàng theo thời gian thực với mã vận đơn
            </Text>
            <Button type="primary" className="home-card-btn">
              Tra Cứu Ngay
            </Button>
          </Card>

          {/* Bảng giá */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/info/shipping-rates")}
          >
            <div className="home-card-icon-wrapper">
              <ProfileOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Bảng Giá Dịch Vụ</Title>
            <Text className="home-card-description">
              Xem chi tiết bảng giá cạnh tranh cho tất cả các dịch vụ vận chuyển
            </Text>
            <Button type="primary" className="home-card-btn">
              Xem Bảng Giá
            </Button>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default TrackingServices;
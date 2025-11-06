import React from "react";
import { Row, Col, Card, Typography, Button } from "antd";
import {
  DollarOutlined,
  EnvironmentOutlined,
  FileSearchOutlined,
  ProfileOutlined,
} from "@ant-design/icons";
import "./TrackingService.css";

const { Title, Paragraph } = Typography;

const TrackingServices: React.FC = () => {
  return (
    <div className="tracking-section">
      <div className="container">
        {/* Section Header */}
        <div className="section-header">
          <Title level={2} className="section-title">Tra Cứu & Tính Toán</Title>
        </div>

        {/* Services Grid */}
        <Row gutter={[24, 24]}>
          {/* Cước phí */}
          <Col xs={24} sm={12} lg={6}>
            <Card
              hoverable
              className="service-card"
              onClick={() => (window.location.href = "/tracking/shipping-fee")}
            >
              <div className="card-icon-wrapper">
                <DollarOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Tính Cước Phí</Title>
              <Paragraph className="card-description">
                Tính toán chi phí vận chuyển dựa trên khối lượng, kích thước và khoảng cách
              </Paragraph>
              <Button type="primary" className="service-btn">
                Tính Phí Ngay
              </Button>
            </Card>
          </Col>

          {/* Bưu cục */}
          <Col xs={24} sm={12} lg={6}>
            <Card
              hoverable
              className="service-card"
              onClick={() => (window.location.href = "/tracking/office-search")}
            >
              <div className="card-icon-wrapper">
                <EnvironmentOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Tìm Bưu Cục</Title>
              <Paragraph className="card-description">
                Khám phá mạng lưới bưu cục rộng khắp với đầy đủ thông tin chi tiết và vị trí
              </Paragraph>
              <Button type="primary" className="service-btn">
                Tìm Kiếm
              </Button>
            </Card>
          </Col>

          {/* Vận đơn */}
          <Col xs={24} sm={12} lg={6}>
            <Card
              hoverable
              className="service-card"
              onClick={() => (window.location.href = "/tracking/order-tracking")}
            >
              <div className="card-icon-wrapper">
                <FileSearchOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Theo Dõi Đơn Hàng</Title>
              <Paragraph className="card-description">
                Theo dõi trạng thái và lịch trình giao hàng theo thời gian thực với mã vận đơn
              </Paragraph>
              <Button type="primary" className="service-btn">
                Tra Cứu Ngay
              </Button>
            </Card>
          </Col>

          {/* Bảng giá */}
          <Col xs={24} sm={12} lg={6}>
            <Card
              hoverable
              className="service-card"
              onClick={() => (window.location.href = "/info/shipping-rates")}
            >
              <div className="card-icon-wrapper">
                <ProfileOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Bảng Giá Dịch Vụ</Title>
              <Paragraph className="card-description">
                Xem chi tiết bảng giá cạnh tranh cho tất cả các dịch vụ vận chuyển của chúng tôi
              </Paragraph>
              <Button type="primary" className="service-btn">
                Xem Bảng Giá
              </Button>
            </Card>
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default TrackingServices;
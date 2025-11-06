import React from "react";
import { Row, Col, Card, Typography, Button } from "antd";
import { ClockCircleOutlined, RocketOutlined, ThunderboltOutlined, ArrowRightOutlined } from "@ant-design/icons";
import "./ShippingServices.css";

const { Title, Paragraph } = Typography;

const ShippingServices: React.FC = () => {
  return (
    <div className="shipping-section">
      <div className="container">
        <div className="section-header">
          <Title level={4} className="section-title">Dịch Vụ Giao Hàng</Title>
        </div>

        <Row gutter={[24, 24]}>
          {/* Tiêu chuẩn */}
          <Col xs={24} sm={8}>
            <Card
              hoverable
              className="service-card"
              onClick={() => window.location.href = "/services/standard"}
            >
              <div className="card-icon-wrapper">
                <ClockCircleOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Tiêu Chuẩn</Title>
              <Paragraph className="card-description">
                Giao hàng trong 3-5 ngày - Phù hợp cho mọi loại hàng hóa
              </Paragraph>
              <Button 
                type="primary" 
                className="detail-btn"
              >
                Xem Chi Tiết
              </Button>
            </Card>
          </Col>

          {/* Nhanh */}
          <Col xs={24} sm={8}>
            <Card
              hoverable
              className="service-card"
              onClick={() => window.location.href = "/services/express"}
            >
              <div className="card-icon-wrapper">
                <RocketOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Nhanh</Title>
              <Paragraph className="card-description">
                Giao hàng trong 1-2 ngày - Ưu tiên xử lý
              </Paragraph>
              <Button 
                type="primary" 
                className="detail-btn"
              >
                Xem Chi Tiết
              </Button>
            </Card>
          </Col>

          {/* Hỏa tốc */}
          <Col xs={24} sm={8}>
            <Card
              hoverable
              className="service-card"
              onClick={() => window.location.href = "/services/flash"}
            >
              <div className="card-icon-wrapper">
                <ThunderboltOutlined className="service-icon" />
              </div>
              <Title level={4} className="card-title">Hỏa Tốc</Title>
              <Paragraph className="card-description">
                Giao hàng trong 4-8 giờ - Dịch vụ khẩn cấp
              </Paragraph>
              <Button 
                type="primary" 
                className="detail-btn"
              >
                Xem Chi Tiết
              </Button>
            </Card>
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default ShippingServices;
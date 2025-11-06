import React from "react";
import { Row, Col, Card, Typography, Button } from "antd";
import { ClockCircleOutlined, RocketOutlined, ThunderboltOutlined } from "@ant-design/icons";
import "./ShippingServices.css";

const { Title } = Typography;

const ShippingServices: React.FC = () => {
  return (
    <div className="shipping-section">
      <div className="container">
        <div className="section-header">
          <Title level={4} className="section-title">Dịch Vụ Giao Hàng</Title>
        </div>

        <Row gutter={[20, 20]}>
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
              <div className="card-description">
                Giao hàng trong 3-5 ngày
              </div>
              <Button 
                type="primary" 
                className="detail-btn"
                size="small"
              >
                Xem chi tiết
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
              <div className="card-description">
                Giao hàng trong 1-2 ngày
              </div>
              <Button 
                type="primary" 
                className="detail-btn"
                size="small"
              >
                Xem chi tiết
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
              <div className="card-description">
                Giao hàng trong 4-8 giờ
              </div>
              <Button 
                type="primary" 
                className="detail-btn"
                size="small"
              >
                Xem chi tiết
              </Button>
            </Card>
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default ShippingServices;
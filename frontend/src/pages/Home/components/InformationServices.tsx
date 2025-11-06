import React from "react";
import { Row, Col, Card, Typography, Button } from "antd";
import { InfoCircleOutlined, PhoneOutlined, GiftOutlined } from "@ant-design/icons";
import "./InformationServices.css";

const { Title, Paragraph } = Typography;

const InformationServices: React.FC = () => {
  return (
    <div className="info-section">
      <div className="container">
        {/* Section Header */}
        <div className="section-header">
          <Title level={4} className="section-title">Ưu Đãi & Hỗ Trợ</Title>
        </div>

        {/* Information Grid */}
        <Row gutter={[24, 24]}>
          {/* Khuyến mãi */}
          <Col xs={24} sm={12} lg={8}>
            <Card
              hoverable
              className="info-card"
              onClick={() => (window.location.href = "/promotions")}
            >
              <div className="card-icon-wrapper">
                <GiftOutlined className="info-icon" />
              </div>
              <Title level={4} className="card-title">Khuyến Mãi</Title>
              <Paragraph className="card-description">
                Khám phá các chương trình ưu đãi, giảm giá đặc biệt và quà tặng hấp dẫn
              </Paragraph>
              <div className="promo-badge">MỚI</div>
              <Button type="primary" className="info-btn">
                Xem Ngay
              </Button>
            </Card>
          </Col>

          {/* Về chúng tôi */}
          <Col xs={24} sm={12} lg={8}>
            <Card
              hoverable
              className="info-card"
              onClick={() => (window.location.href = "/info/company")}
            >
              <div className="card-icon-wrapper">
                <InfoCircleOutlined className="info-icon" />
              </div>
              <Title level={4} className="card-title">Về Chúng Tôi</Title>
              <Paragraph className="card-description">
                Tìm hiểu về hành trình phát triển và những giá trị cốt lõi của UTE Logistics
              </Paragraph>
              <Button type="default" className="info-btn">
                Khám Phá
              </Button>
            </Card>
          </Col>

          {/* Liên hệ */}
          <Col xs={24} sm={12} lg={8}>
            <Card
              hoverable
              className="info-card"
              onClick={() => (window.location.href = "/info/contact")}
            >
              <div className="card-icon-wrapper">
                <PhoneOutlined className="info-icon" />
              </div>
              <Title level={4} className="card-title">Liên Hệ</Title>
              <Paragraph className="card-description">
                Kết nối với đội ngũ hỗ trợ 24/7 để được tư vấn và giải đáp mọi thắc mắc
              </Paragraph>
              <Button type="primary" className="info-btn">
                Liên Hệ Ngay
              </Button>
            </Card>
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default InformationServices;
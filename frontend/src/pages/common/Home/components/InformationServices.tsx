import React from "react";
import { Card, Typography, Button } from "antd";
import { InfoCircleOutlined, PhoneOutlined, GiftOutlined } from "@ant-design/icons";
// import "./InformationServices.css";

const { Title, Text } = Typography;

const InformationServices: React.FC = () => {
  return (
    <div className="home-section">
      <div className="home-container">
        {/* Section Header */}
        <div className="home-section-header">
          <div>
            <Title level={2} className="home-section-title">
              Ưu Đãi & Hỗ Trợ
            </Title>
            <Text style={{ color: '#666', fontSize: '1.1rem' }}>
              Khám phá các chương trình ưu đãi và dịch vụ hỗ trợ khách hàng
            </Text>
          </div>
        </div>

        {/* Information Grid */}
        <div className="home-grid">
          {/* Khuyến mãi */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/promotions")}
          >
            <div className="home-card-icon-wrapper">
              <GiftOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Khuyến Mãi</Title>
            <Text className="home-card-description">
              Khám phá các chương trình ưu đãi, giảm giá đặc biệt và quà tặng hấp dẫn
            </Text>
            <div className="home-promo-badge">MỚI</div>
            <Button type="primary" className="home-card-btn">
              Xem Ngay
            </Button>
          </Card>

          {/* Về chúng tôi */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/info/company")}
          >
            <div className="home-card-icon-wrapper">
              <InfoCircleOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Về Chúng Tôi</Title>
            <Text className="home-card-description">
              Tìm hiểu về hành trình phát triển và những giá trị cốt lõi của UTE Logistics
            </Text>
            <Button type="primary" className="home-card-btn">
              Khám Phá
            </Button>
          </Card>

          {/* Liên hệ */}
          <Card
            hoverable
            className="home-card"
            onClick={() => (window.location.href = "/info/contact")}
          >
            <div className="home-card-icon-wrapper">
              <PhoneOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Liên Hệ</Title>
            <Text className="home-card-description">
              Kết nối với đội ngũ hỗ trợ 24/7 để được tư vấn và giải đáp mọi thắc mắc
            </Text>
            <Button type="primary" className="home-card-btn">
              Liên Hệ Ngay
            </Button>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default InformationServices;
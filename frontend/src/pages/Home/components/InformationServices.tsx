import React from "react";
import { Card, Typography, Button } from "antd";
import { InfoCircleOutlined, PhoneOutlined, GiftOutlined } from "@ant-design/icons";
import "./InformationServices.css";

const { Title, Text } = Typography;

const InformationServices: React.FC = () => {
  return (
    <div className="info-section">
      <div className="info-container">
        {/* Section Header */}
        <div className="info-section-header">
          <div>
            <Title level={2} className="info-section-title">
              Ưu Đãi & Hỗ Trợ
            </Title>
            <Text style={{ color: '#666', fontSize: '1.1rem' }}>
              Khám phá các chương trình ưu đãi và dịch vụ hỗ trợ khách hàng
            </Text>
          </div>
        </div>

        {/* Information Grid */}
        <div className="info-services-grid">
          {/* Khuyến mãi */}
          <Card
            hoverable
            className="info-card"
            onClick={() => (window.location.href = "/promotions")}
          >
            <div className="info-card-icon-wrapper">
              <GiftOutlined className="info-icon" />
            </div>
            <Title level={4} className="info-card-title">Khuyến Mãi</Title>
            <Text className="info-card-description">
              Khám phá các chương trình ưu đãi, giảm giá đặc biệt và quà tặng hấp dẫn
            </Text>
            <div className="info-promo-badge">MỚI</div>
            <Button type="primary" className="info-card-btn">
              Xem Ngay
            </Button>
          </Card>

          {/* Về chúng tôi */}
          <Card
            hoverable
            className="info-card"
            onClick={() => (window.location.href = "/info/company")}
          >
            <div className="info-card-icon-wrapper">
              <InfoCircleOutlined className="info-icon" />
            </div>
            <Title level={4} className="info-card-title">Về Chúng Tôi</Title>
            <Text className="info-card-description">
              Tìm hiểu về hành trình phát triển và những giá trị cốt lõi của UTE Logistics
            </Text>
            <Button type="primary" className="info-card-btn">
              Khám Phá
            </Button>
          </Card>

          {/* Liên hệ */}
          <Card
            hoverable
            className="info-card"
            onClick={() => (window.location.href = "/info/contact")}
          >
            <div className="info-card-icon-wrapper">
              <PhoneOutlined className="info-icon" />
            </div>
            <Title level={4} className="info-card-title">Liên Hệ</Title>
            <Text className="info-card-description">
              Kết nối với đội ngũ hỗ trợ 24/7 để được tư vấn và giải đáp mọi thắc mắc
            </Text>
            <Button type="primary" className="info-card-btn">
              Liên Hệ Ngay
            </Button>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default InformationServices;
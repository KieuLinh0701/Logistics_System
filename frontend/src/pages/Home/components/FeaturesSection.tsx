import React from "react";
import { Row, Col, Typography, Button } from "antd";
import { StarOutlined, TruckOutlined, DollarOutlined, UserOutlined, LoginOutlined } from "@ant-design/icons";
import "./FeaturesSection.css";

const { Title, Paragraph } = Typography;

const FeaturesSection: React.FC = () => {
  return (
    <div className="features-section">
      <div className="container">
        {/* Features Grid */}
        <div className="features-content">
          <Title level={4} className="features-title">
            Tại Sao Nên Chọn UTE Logistics?
          </Title>

          <Row gutter={[32, 32]}>
            <Col xs={24} md={8}>
              <div className="feature-item">
                <div className="feature-icon-wrapper">
                  <StarOutlined className="feature-icon" />
                </div>
                <Title level={4} className="feature-item-title">Chất Lượng Cao</Title>
                <Paragraph className="feature-description">
                  Dịch vụ vận chuyển chuyên nghiệp với tỷ lệ thành công 99.5% và đội ngũ nhân viên được đào tạo bài bản
                </Paragraph>
              </div>
            </Col>

            <Col xs={24} md={8}>
              <div className="feature-item">
                <div className="feature-icon-wrapper">
                  <TruckOutlined className="feature-icon" />
                </div>
                <Title level={4} className="feature-item-title">Nhanh Chóng</Title>
                <Paragraph className="feature-description">
                  Giao hàng đúng hẹn với nhiều lựa chọn dịch vụ phù hợp, hỗ trợ vận chuyển 24/7
                </Paragraph>
              </div>
            </Col>

            <Col xs={24} md={8}>
              <div className="feature-item">
                <div className="feature-icon-wrapper">
                  <DollarOutlined className="feature-icon" />
                </div>
                <Title level={4} className="feature-item-title">Giá Cả Hợp Lý</Title>
                <Paragraph className="feature-description">
                  Bảng giá minh bạch, cạnh tranh với nhiều ưu đãi hấp dẫn và không phát sinh chi phí
                </Paragraph>
              </div>
            </Col>
          </Row>
        </div>

        {/* CTA Section - Đăng ký/Đăng nhập */}
        <div className="cta-section">
          <div className="cta-content">
            <div className="cta-text">
              <Title level={2} className="cta-title">
                Bắt Đầu Hành Trình Cùng Chúng Tôi
              </Title>
              <Paragraph className="cta-subtitle">
                Đăng ký tài khoản ngay để trải nghiệm dịch vụ tốt nhất và nhận nhiều ưu đãi đặc biệt
              </Paragraph>
            </div>
            <div className="cta-buttons">
              <Button
                type="primary"
                size="large"
                className="cta-primary-btn"
                icon={<UserOutlined />}
                onClick={() => (window.location.href = "/register")}
              >
                Đăng Ký Ngay
              </Button>
              <Button
                size="large"
                className="cta-secondary-btn"
                icon={<LoginOutlined />}
                onClick={() => (window.location.href = "/login")}
              >
                Đăng Nhập
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FeaturesSection;
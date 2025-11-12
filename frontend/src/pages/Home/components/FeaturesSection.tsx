import React from "react";
import { Typography, Button } from "antd";
import { StarOutlined, TruckOutlined, DollarOutlined, UserOutlined, LoginOutlined } from "@ant-design/icons";
import "./FeaturesSection.css";
import { getCurrentUser } from "../../../utils/authUtils";
import { getFullName } from "../../../types/auth";

const { Title, Text } = Typography;

const FeaturesSection: React.FC = () => {
  const user = getCurrentUser();

  return (
    <div className="features-section">
      <div className="features-container">
        {/* Section Header */}
        <div className="features-section-header">
          <div>
            <Title level={2} className="features-section-title">
              Tại Sao Nên Chọn UTE Logistics?
            </Title>
            <Text className="features-section-subtitle">
              Khám phá những lý do hàng đầu để tin tưởng và lựa chọn dịch vụ của chúng tôi
            </Text>
          </div>
        </div>

        {/* Features Grid */}
        <div className="features-grid">
          {/* Chất lượng cao */}
          <div className="feature-card">
            <div className="feature-icon-wrapper">
              <StarOutlined className="feature-icon" />
            </div>
            <Title level={4} className="feature-card-title">Chất Lượng Cao</Title>
            <Text className="feature-card-description">
              Dịch vụ vận chuyển chuyên nghiệp với tỷ lệ thành công 99.5% và đội ngũ nhân viên được đào tạo bài bản
            </Text>
          </div>

          {/* Nhanh chóng */}
          <div className="feature-card">
            <div className="feature-icon-wrapper">
              <TruckOutlined className="feature-icon" />
            </div>
            <Title level={4} className="feature-card-title">Nhanh Chóng</Title>
            <Text className="feature-card-description">
              Giao hàng đúng hẹn với nhiều lựa chọn dịch vụ phù hợp, hỗ trợ vận chuyển 24/7
            </Text>
          </div>

          {/* Giá cả hợp lý */}
          <div className="feature-card">
            <div className="feature-icon-wrapper">
              <DollarOutlined className="feature-icon" />
            </div>
            <Title level={4} className="feature-card-title">Giá Cả Hợp Lý</Title>
            <Text className="feature-card-description">
              Bảng giá minh bạch, cạnh tranh với nhiều ưu đãi hấp dẫn và không phát sinh chi phí
            </Text>
          </div>
        </div>

        {/* CTA Section */}
        <div className="cta-section">
          <div className="cta-content">
            <div className="cta-text">
              <Title level={3} className="cta-title">
                Bắt Đầu Hành Trình Cùng Chúng Tôi
              </Title>

              <Text className="cta-subtitle">
                {!user
                  ? "Đăng ký tài khoản ngay để trải nghiệm dịch vụ tốt nhất và nhận nhiều ưu đãi đặc biệt!"
                  : `Chào ${getFullName(user!)}! Truy cập ngay trang quản lý của bạn để theo dõi đơn hàng và nhận thông báo.`}
              </Text>
            </div>

            <div className="cta-buttons">
              {!user ? (
                <>
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
                </>
              ) : (
                <Button
                  type="primary"
                  size="large"
                  className="cta-primary-btn"
                  icon={<UserOutlined />}
                  onClick={() => (window.location.href = `/dashboard`)}
                >
                  Vào Trang Quản Lý
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FeaturesSection;
import React from "react";
import { Typography, Button } from "antd";
import { StarOutlined, TruckOutlined, DollarOutlined, UserOutlined, LoginOutlined } from "@ant-design/icons";
import { getCurrentUser } from "../../../../utils/authUtils";
import { getFullName } from "../../../../types/auth";

const { Title, Text } = Typography;

const FeaturesSection: React.FC = () => {
  const user = getCurrentUser();

  return (
    <div className="home-section">
      <div className="home-container">
        {/* Section Header */}
        <div className="home-section-header">
          <div>
            <Title level={2} className="home-section-title">
              Tại Sao Nên Chọn UTE Logistics?
            </Title>
            <Text className="home-section-subtitle">
              Khám phá những lý do hàng đầu để tin tưởng và lựa chọn dịch vụ của chúng tôi
            </Text>
          </div>
        </div>

        {/* Features Grid */}
        <div className="home-grid">
          {/* Chất lượng cao */}
          <div className="home-card">
            <div className="home-icon-wrapper">
              <StarOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Chất Lượng Cao</Title>
            <Text className="home-card-description">
              Dịch vụ vận chuyển chuyên nghiệp với tỷ lệ thành công 99.5% và đội ngũ nhân viên được đào tạo bài bản
            </Text>
          </div>

          {/* Nhanh chóng */}
          <div className="home-card">
            <div className="home-icon-wrapper">
              <TruckOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Nhanh Chóng</Title>
            <Text className="home-card-description">
              Giao hàng đúng hẹn với nhiều lựa chọn dịch vụ phù hợp, hỗ trợ vận chuyển 24/7
            </Text>
          </div>

          {/* Giá cả hợp lý */}
          <div className="home-card">
            <div className="home-icon-wrapper">
              <DollarOutlined className="home-icon" />
            </div>
            <Title level={4} className="home-card-title">Giá Cả Hợp Lý</Title>
            <Text className="home-card-description">
              Bảng giá minh bạch, cạnh tranh với nhiều ưu đãi hấp dẫn và không phát sinh chi phí
            </Text>
          </div>
        </div>

        {/* CTA Section */}
        <div className="home-cta-section">
          <div className="home-cta-content">
            <div className="home-cta-text">
              <Title level={3} className="home-cta-title">
                Bắt Đầu Hành Trình Cùng Chúng Tôi
              </Title>

              <Text className="home-cta-subtitle">
                {!user
                  ? "Đăng ký tài khoản ngay để trải nghiệm dịch vụ tốt nhất và nhận nhiều ưu đãi đặc biệt!"
                  : `Chào ${getFullName(user!)}! Truy cập ngay trang quản lý của bạn để theo dõi đơn hàng và nhận thông báo.`}
              </Text>
            </div>

            <div className="home-cta-buttons">
              {!user ? (
                <>
                  <Button
                    type="primary"
                    size="large"
                    className="home-cta-primary-btn"
                    icon={<UserOutlined />}
                    onClick={() => (window.location.href = "/register")}
                  >
                    Đăng Ký Ngay
                  </Button>
                  <Button
                    size="large"
                    className="home-cta-secondary-btn"
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
                  className="home-cta-primary-btn"
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
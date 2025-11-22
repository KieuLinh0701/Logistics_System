import React, { useState } from "react";
import { Input, Typography, Button, Space } from "antd";
import heroImage from "../../../../assets/images/home/h1_hero.jpg";

const { Title, Paragraph } = Typography;

const HeroSection: React.FC = () => {
  const [trackingCode, setTrackingCode] = useState("");

  const handleSearch = () => {
    if (!trackingCode.trim()) return;
    window.location.href = `/tracking/${trackingCode}`;
  };

  return (
    <div
      className="home-hero-section"
      style={{
        backgroundImage: `url(${heroImage})`,
        backgroundSize: "cover",
        backgroundPosition: "center",
        backgroundRepeat: "no-repeat",
      }}
    >
      <div className="home-hero-overlay">
        <div className="home-hero-content">
          <Title level={1} className="home-hero-title">
            UTE Logistics
          </Title>

          <Paragraph className="home-hero-subtitle">
            Dịch vụ vận chuyển hàng hóa chuyên nghiệp, nhanh chóng và an toàn
          </Paragraph>

          <div className="home-hero-actions">
            <div className="home-search-container">
              <Space.Compact style={{ width: "100%", maxWidth: 400 }}>
                <Input
                  placeholder="Nhập mã vận đơn để tra cứu..."
                  size="large"
                  value={trackingCode}
                  onChange={(e) => setTrackingCode(e.target.value)}
                  onPressEnter={handleSearch}
                />
                <Button
                  type="primary"
                  size="large"
                  onClick={handleSearch}
                >
                  Tra cứu
                </Button>
              </Space.Compact>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HeroSection;
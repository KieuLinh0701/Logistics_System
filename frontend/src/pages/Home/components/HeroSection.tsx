import React, { useState } from "react";
import { Input, Typography } from "antd";
import "./HeroSection.css";
import heroImage from "../../../assets/images/home/h1_hero.jpg";

const { Title, Paragraph } = Typography;

const HeroSection: React.FC = () => {
    const [trackingCode, setTrackingCode] = useState("");

    const handleSearch = () => {
        if (!trackingCode.trim()) return;
        window.location.href = `/tracking/${trackingCode}`;
    };

    return (
        <div
            className="hero-section"
            style={{
                backgroundImage: `url(${heroImage})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                backgroundRepeat: "no-repeat",
            }}
        >
            <div className="hero-overlay">
                <div className="hero-content">
                    <Title level={1} className="hero-title">
                        UTE Logistics
                    </Title>

                    <Paragraph className="hero-subtitle">
                        Dịch vụ vận chuyển hàng hóa chuyên nghiệp, nhanh chóng và an toàn
                    </Paragraph>

                    <div className="hero-actions">
                        <div className="search-container">
                            <Input.Search
                                placeholder="Nhập mã vận đơn để tra cứu..."
                                enterButton="Search"
                                size="large"
                                value={trackingCode}
                                onChange={(e) => setTrackingCode(e.target.value)}
                                onSearch={handleSearch}
                                className="hero-search"
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default HeroSection;
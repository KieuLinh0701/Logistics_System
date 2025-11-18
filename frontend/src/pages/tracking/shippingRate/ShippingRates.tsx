import React from "react";
import ShippingRatesBody from "./ShippingRatesBody";
import HeaderHome from "../../../components/common/HeaderHome";
import FooterHome from "../../../components/common/FooterHome";
import Title from "antd/es/typography/Title";
import Paragraph from "antd/es/typography/Paragraph";
import trackingHeroImage from "../../../assets/images/shippingRates.jpg";
import "./ShippingRates.css";

const ShippingRates: React.FC = () => {
  return (
    <div>
      <HeaderHome />
      <div className="shipping-rates-page">
        {/* Hero Section */}
        <div
          className="shipping-rates-hero"
          style={{
            backgroundImage: `url(${trackingHeroImage})`,
            backgroundSize: "cover",
            backgroundPosition: "center",
            backgroundRepeat: "no-repeat",
          }}
        >
          <div className="shipping-rates-hero-overlay">
            <div className="shipping-rates-hero-content">
              <Title level={2} className="shipping-rates-hero-title">
                Tra Cứu Bảng Gía Vận Chuyển
              </Title>
              <Paragraph className="shipping-rates-hero-subtitle">
                Tra cứu bảng giá vận chuyển cập nhật mới nhất
              </Paragraph>
            </div>
          </div>
        </div>
        <ShippingRatesBody />
      </div>
      <FooterHome />
    </div>
  );
};

export default ShippingRates;
import React from "react";
import ShippingFeeBody from "./ShippingFeeBody";
import HeaderHome from "../../../components/common/HeaderHome";
import FooterHome from "../../../components/common/FooterHome";
import Title from "antd/es/typography/Title";
import Paragraph from "antd/es/typography/Paragraph";
import trackingHeroImage from "../../../assets/images/shippingFee.jpg";
import "./ShippingFee.css";

const ShippingFee: React.FC = () => {
  return (
    <div>
      <HeaderHome />
      <div className="shipping-fee-page">
        {/* Hero Section */}
        <div
          className="shipping-fee-hero"
          style={{
            backgroundImage: `url(${trackingHeroImage})`,
            backgroundSize: "cover",
            backgroundPosition: "center",
            backgroundRepeat: "no-repeat",
          }}
        >
          <div className="shipping-fee-hero-overlay">
            <div className="shipping-fee-hero-content">
              <Title level={2} className="shipping-fee-hero-title">
                Tra Cứu Cước Vận Chuyển
              </Title>
              <Paragraph className="shipping-fee-hero-subtitle">
                Tính toán chi phí vận chuyển nhanh chóng và chính xác
              </Paragraph>
            </div>
          </div>
        </div>
        <ShippingFeeBody />
      </div>
      <FooterHome />
    </div>
  );
};

export default ShippingFee;
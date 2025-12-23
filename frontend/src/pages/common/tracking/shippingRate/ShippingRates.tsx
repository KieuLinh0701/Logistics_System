import React from "react";
import ShippingRatesBody from "./ShippingRatesBody";
import HeaderHome from "../../../../components/common/HeaderHome";
import FooterHome from "../../../../components/common/FooterHome";
import Paragraph from "antd/es/typography/Paragraph";
import trackingHeroImage from "../../../../assets/images/shippingRates.jpg";
import "./ShippingRates.css";
import { ArrowRightOutlined, FileSearchOutlined, GiftOutlined, LineChartOutlined } from "@ant-design/icons";
import { Button, Card, Typography } from "antd";

const { Title, Text } = Typography;

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

        {/* Công cụ tính phí nhanh */}
        <div className="shipping-rates-cta-section">
          <div className="shipping-rates-cta-content">
            <div className="shipping-rates-cta-text">
              <Title level={3} className="shipping-rates-cta-title">
                Công cụ tính phí nhanh
              </Title>

              <Text className="shipping-rates-cta-subtitle">
                Ước tính chi phí vận chuyển theo khối lượng và khu vực chỉ với vài bước.
              </Text>
            </div>

            <div className="shipping-rates-cta-buttons">
              <Button
                type="primary"
                size="large"
                className="shipping-rates-cta-primary-btn"
                onClick={() => window.location.href = '/tracking/shipping-fee'}
              >
                Tính phí ngay <ArrowRightOutlined />
              </Button>
            </div>
          </div>
        </div>



        {/* Chính sách giá */}
        <Card className="shipping-rates-guide-card">
          <Title level={3} className="shipping-rates-guide-title">
            Chính sách giá
          </Title>

          <div className="shipping-rates-grid">
            {/* Giá cạnh tranh */}
            <div className="shipping-rates-card">
              <div className="shipping-rates-icon-wrapper">
                <LineChartOutlined className="shipping-rates-icon" />
              </div>
              <Title level={4} className="shipping-rates-card-title">Giá cạnh tranh</Title>
              <Text className="shipping-rates-card-description">
                Mức giá được tối ưu nhằm mang lại lựa chọn phù hợp cho đa dạng nhu cầu giao hàng.
              </Text>
            </div>

            {/* Minh bạch */}
            <div className="shipping-rates-card">
              <div className="shipping-rates-icon-wrapper">
                <FileSearchOutlined className="shipping-rates-icon" />
              </div>
              <Title level={4} className="shipping-rates-card-title">Minh bạch</Title>
              <Text className="shipping-rates-card-description">
                Các chi phí được công khai rõ ràng. Không có phí phát sinh ngoài các khoản tiêu chuẩn.
              </Text>
            </div>

            {/* Ưu đãi */}
            <div className="shipping-rates-card">
              <div className="shipping-rates-icon-wrapper">
                <GiftOutlined className="shipping-rates-icon" />
              </div>
              <Title level={4} className="shipping-rates-card-title">Ưu đãi</Title>
              <Text className="shipping-rates-card-description">
                Liên tục có các chương trình ưu đãi dành cho khách hàng thân thiết và doanh nghiệp.
              </Text>
            </div>
          </div>
        </Card>
      </div>
      <FooterHome />
    </div>
  );
};

export default ShippingRates;
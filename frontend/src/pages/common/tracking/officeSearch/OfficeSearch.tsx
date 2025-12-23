import React from "react";
import OfficeSearchBody from "./OfficeSearchBody";
import { Typography } from "antd";
import officeHeroImage from "../../../../assets/images/office.jpg";
import "./OfficeSearch.css";
import HeaderHome from "../../../../components/common/HeaderHome";
import FooterHome from "../../../../components/common/FooterHome";

const { Title, Paragraph } = Typography;

const OfficeSearch: React.FC = () => {
  return (
    <div>
      <HeaderHome />
      <div className="office-search-page">
        {/* Hero Section */}
        <div
          className="office-search-hero"
          style={{
            backgroundImage: `url(${officeHeroImage})`,
            backgroundSize: "cover",
            backgroundPosition: "center",
            backgroundRepeat: "no-repeat",
          }}
        >
          <div className="office-search-hero-overlay">
            <div className="office-search-hero-content">
              <Title level={2} className="office-search-hero-title">
                Tra Cứu Bưu Cục
              </Title>
              <Paragraph className="office-search-hero-subtitle">
                Tìm kiếm bưu cục gần bạn nhanh chóng và tiện lợi
              </Paragraph>
            </div>
          </div>
        </div>

        <OfficeSearchBody />
      </div>
      <FooterHome />
    </div>
  );
};

export default OfficeSearch;
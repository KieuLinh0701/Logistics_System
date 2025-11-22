import React, { useEffect, useState } from "react";
import { Typography, Card, Row, Col, message, Spin } from "antd";
import {
  EnvironmentOutlined,
  PhoneOutlined,
  MailOutlined,
  TeamOutlined,
  TrophyOutlined,
  SafetyOutlined
} from "@ant-design/icons";
import companyInfoImage from "../../../assets/images/companyInfo.jpg";
import HeaderHome from "../../../components/common/HeaderHome";
import FooterHome from "../../../components/common/FooterHome";
import type { Office } from "../../../types/office";
import officeApi from "../../../api/officeApi";
import locationApi from "../../../api/locationApi";
import "./CompanyInfo.css";

const { Title, Text, Paragraph } = Typography;

const CompanyInfo: React.FC = () => {
  const [office, setOffice] = useState<Office | null>(null);
  const [loading, setLoading] = useState(true);
  const [address, setAddress] = useState("");

  useEffect(() => {
    const fetchAddress = async () => {
      if (!office) return;
      try {
        const city = await locationApi.getCityNameByCode(office.cityCode);
        const ward = await locationApi.getWardNameByCode(office?.cityCode, office?.wardCode);

        const parts = [office.detail, ward, city].filter(Boolean);
        setAddress(parts.join(", "));
      } catch (error) {
        console.error("Error getting address:", error);
        setAddress(office.detail);
      }
    };

    fetchAddress();
  }, [office]);

  useEffect(() => {
    const fetchCompanyInfo = async () => {
      try {
        const result = await officeApi.getHeadOffice();
        if (result.success) {
          setOffice(result.data);
        }
      } catch (error) {
        message.error("Không thể tải thông tin công ty");
      } finally {
        setLoading(false);
      }
    };

    fetchCompanyInfo();
  }, []);

  if (loading) {
    return (
      <div>
        <HeaderHome />
        <div className="company-info-loading">
          <Spin size="large" />
          <div className="company-info-loading-text">Đang tải thông tin công ty...</div>
        </div>
        <FooterHome />
      </div>
    );
  }

  return (
    <div className="company-info-page">
      <HeaderHome />

      {/* Hero Section */}
      <div
        className="company-info-hero"
        style={{
          backgroundImage: `url(${companyInfoImage})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
        }}
      >
        <div className="company-info-hero-overlay">
          <div className="company-info-hero-content">
            <Title level={2} className="company-info-hero-title">
              Về Chúng Tôi
            </Title>
            <Paragraph className="company-info-hero-subtitle">
              Khám phá hành trình và giá trị của UTE Logistics
            </Paragraph>
          </div>
        </div>
      </div>

      <div className="company-info-container">
        {/* Header */}
        <div className="company-info-header">
          <Title level={2}>Giới thiệu công ty</Title>
        </div>

        {/* Office Information */}
        {office && (
          <Card className="company-info-office-card">
            <Row gutter={[32, 32]}>

              <Col span={24}>
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <div className="company-info-contact-item">
                      <EnvironmentOutlined className="company-info-contact-icon" />
                      <div className="company-info-contact-text">
                        <Text className="company-info-contact-label">Địa chỉ:</Text>
                        <Text className="company-info-contact-value">{address}</Text>
                      </div>
                    </div>
                  </Col>

                  <Col xs={24} md={12}>
                    <div className="company-info-contact-item">
                      <PhoneOutlined className="company-info-contact-icon" />
                      <div className="company-info-contact-text">
                        <Text className="company-info-contact-label">Hotline:</Text>
                        <Text className="company-info-contact-value">{office.phoneNumber}</Text>
                      </div>
                    </div>
                  </Col>

                  <Col xs={24} md={12}>
                    <div className="company-info-contact-item">
                      <MailOutlined className="company-info-contact-icon" />
                      <div className="company-info-contact-text">
                        <Text className="company-info-contact-label">Email:</Text>
                        <Text className="company-info-contact-value">{office.email}</Text>
                      </div>
                    </div>
                  </Col>
                </Row>
              </Col>
            </Row>
          </Card>
        )}

        {/* Features */}
        <Row gutter={[24, 24]}>
          <Col xs={24} md={8}>
            <Card className="company-info-feature-card">
              <TeamOutlined className="company-info-feature-icon" />
              <Title level={4} className="company-info-feature-title">Đội ngũ chuyên nghiệp</Title>
              <Paragraph className="company-info-feature-description">
                Với đội ngũ nhân viên giàu kinh nghiệm và chuyên nghiệp, chúng tôi cam kết mang đến
                dịch vụ vận chuyển tốt nhất cho khách hàng.
              </Paragraph>
            </Card>
          </Col>

          <Col xs={24} md={8}>
            <Card className="company-info-feature-card">
              <TrophyOutlined className="company-info-feature-icon" />
              <Title level={4} className="company-info-feature-title">Chất lượng dịch vụ</Title>
              <Paragraph className="company-info-feature-description">
                Chúng tôi luôn đặt chất lượng dịch vụ lên hàng đầu, đảm bảo hàng hóa được vận chuyển
                an toàn và đúng thời gian.
              </Paragraph>
            </Card>
          </Col>

          <Col xs={24} md={8}>
            <Card className="company-info-feature-card">
              <SafetyOutlined className="company-info-feature-icon" />
              <Title level={4} className="company-info-feature-title">An toàn & Bảo mật</Title>
              <Paragraph className="company-info-feature-description">
                Hệ thống bảo mật hiện đại và quy trình vận chuyển chặt chẽ đảm bảo hàng hóa của bạn
                luôn được bảo vệ tốt nhất.
              </Paragraph>
            </Card>
          </Col>
        </Row>

        {/* Mission & Vision */}
        <Card className="company-info-mission-card">
          <Title level={3} className="company-info-mission-title">
            Tầm nhìn & Sứ mệnh
          </Title>

          <Row gutter={[32, 32]}>
            <Col xs={24} md={12}>
              <div className="company-info-mission-section">
                <Title level={4} className="company-info-mission-subtitle">Tầm nhìn</Title>
                <Paragraph className="company-info-mission-text">
                  Trở thành công ty vận chuyển hàng đầu Việt Nam, được khách hàng tin tưởng và lựa chọn
                  với dịch vụ chất lượng cao, giá cả hợp lý.
                </Paragraph>
              </div>
            </Col>

            <Col xs={24} md={12}>
              <div className="company-info-mission-section">
                <Title level={4} className="company-info-mission-subtitle">Sứ mệnh</Title>
                <Paragraph className="company-info-mission-text">
                  Cung cấp dịch vụ vận chuyển nhanh chóng, an toàn và tiện lợi, góp phần kết nối
                  mọi miền đất nước và thúc đẩy thương mại điện tử phát triển.
                </Paragraph>
              </div>
            </Col>
          </Row>
        </Card>

        {/* Core Values */}
        <Card className="company-info-values-card">
          <Title level={3} className="company-info-values-title">
            Giá trị cốt lõi
          </Title>

          <Row gutter={[16, 16]}>
            <Col xs={12} md={6}>
              <div className="company-info-value-item">
                <Title level={5} className="company-info-value-title">Tin cậy</Title>
                <Text className="company-info-value-description">Đáng tin cậy trong mọi giao dịch</Text>
              </div>
            </Col>
            <Col xs={12} md={6}>
              <div className="company-info-value-item">
                <Title level={5} className="company-info-value-title">Nhanh chóng</Title>
                <Text className="company-info-value-description">Giao hàng nhanh, đúng hẹn</Text>
              </div>
            </Col>
            <Col xs={12} md={6}>
              <div className="company-info-value-item">
                <Title level={5} className="company-info-value-title">An toàn</Title>
                <Text className="company-info-value-description">Bảo vệ hàng hóa tối đa</Text>
              </div>
            </Col>
            <Col xs={12} md={6}>
              <div className="company-info-value-item">
                <Title level={5} className="company-info-value-title">Chuyên nghiệp</Title>
                <Text className="company-info-value-description">Dịch vụ chuyên nghiệp, tận tâm</Text>
              </div>
            </Col>
          </Row>
        </Card>
      </div>

      <FooterHome />
    </div>
  );
};

export default CompanyInfo;
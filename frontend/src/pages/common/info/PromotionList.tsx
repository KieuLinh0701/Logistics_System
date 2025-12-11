import React, { useEffect, useState } from "react";
import { Typography, Card, Row, Col, message, Spin, Pagination, Divider } from "antd";
import {
  CalendarOutlined,
  DollarOutlined,
  PercentageOutlined,
  FireOutlined,
  TagOutlined
} from "@ant-design/icons";
import promotionImage from "../../../assets/images/promotion.jpg";
import HeaderHome from "../../../components/common/HeaderHome";
import FooterHome from "../../../components/common/FooterHome";
import type { Promotion } from "../../../types/promotion";
import "./PromotionList.css";
import promotionApi from "../../../api/promotionApi";
import { getDiscountText } from "../../../utils/promotionUtils";

const { Title, Text, Paragraph } = Typography;

const PromotionList: React.FC = () => {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [selectedPromotion, setSelectedPromotion] = useState<Promotion | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [limit] = useState(5);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    const fetchPromotions = async () => {
      try {
        const result = await promotionApi.getActivePromotions({ page: currentPage, limit: limit });
        if (result.success) {
          const promoList = result.data?.list || [];
          setPromotions(promoList);
          setTotal(result.data?.pagination.total || 0);

          if (promoList.length > 0) {
            setSelectedPromotion(prev =>
              prev && promoList.find(p => p.id === prev.id) ? prev : promoList[0]
            );
          } else {
            setSelectedPromotion(null);
          }
        } else {
          message.error(result.message || "Không thể tải thông tin khuyến mãi")
        }
      } catch (error) {
        console.log("Lỗi tải thông tin khuyến mãi", error);
        message.error("Không thể tải thông tin khuyến mãi");
      } finally {
        setLoading(false);
      }
    };

    fetchPromotions();
  }, [currentPage, limit]);

  const handlePromotionSelect = (promotion: Promotion) => {
    setSelectedPromotion(promotion);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const formatDate = (date: Date) => {
    return new Date(date).toLocaleDateString('vi-VN');
  };

  if (loading) {
    return (
      <div>
        <HeaderHome />
        <div className="promotion-list-loading">
          <Spin size="large" />
          <div className="promotion-list-loading-text">Đang tải thông tin khuyến mãi...</div>
        </div>
        <FooterHome />
      </div>
    );
  }

  return (
    <div className="promotion-list-page">
      <HeaderHome />

      {/* Hero Section */}
      <div
        className="promotion-list-hero"
        style={{
          backgroundImage: `url(${promotionImage})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
        }}
      >
        <div className="promotion-list-hero-overlay">
          <div className="promotion-list-hero-content">
            <Title level={2} className="promotion-list-hero-title">
              Khuyến Mãi & Ưu Đãi
            </Title>
            <Paragraph className="promotion-list-hero-subtitle">
              Khám phá các chương trình khuyến mãi hấp dẫn từ UTE Logistics
            </Paragraph>
          </div>
        </div>
      </div>

      <div className="promotion-list-container">
        {/* Header */}
        <div className="promotion-list-header">
          <Title level={2}>Chương trình khuyến mãi</Title>
        </div>

        <Row gutter={[32, 32]}>
          {/* Left Column - Promotion List */}
          <Col xs={24} lg={10}>
            <Card
              className="promotion-list-card"
              title={
                <div className="promotion-list-card-title">
                  <FireOutlined className="promotion-list-card-icon" />
                  <span>Danh sách khuyến mãi</span>
                </div>
              }
            >
              {promotions.length === 0 ? (
                <div className="promotion-list-empty">
                  <Text type="secondary">Hiện tại không có khuyến mãi nào</Text>
                </div>
              ) : (
                <>
                  <div className="promotion-list-items">
                    {promotions.map((promotion) => (
                      <div
                        key={promotion.id}
                        className={`promotion-list-item ${selectedPromotion?.id === promotion.id ? 'promotion-list-item-active' : ''
                          }`}
                        onClick={() => handlePromotionSelect(promotion)}
                      >
                        <div className="promotion-item-header">
                          <div className="promotion-item-code">
                            <Text strong className="promotion-code-text">
                              {promotion.title}
                            </Text>
                          </div>
                          <div className="promotion-item-discount">
                            <Text strong className="promotion-discount-text">
                              {getDiscountText(promotion.discountType, promotion.discountValue)}
                            </Text>
                          </div>
                        </div>

                        <Paragraph className="promotion-item-title" ellipsis={{ rows: 2 }}>
                          {promotion.code}
                        </Paragraph>

                        <div className="promotion-item-footer">
                          <div className="promotion-item-date">
                            <CalendarOutlined />
                            <Text type="secondary">
                              {formatDate(promotion.startDate)} - {formatDate(promotion.endDate)}
                            </Text>
                          </div>
                          <div className="promotion-item-usage">
                            <Text type="secondary">
                              Đã dùng: {promotion.usedCount} /
                              {promotion.usageLimit !== null
                                ? ` ${promotion.usageLimit}`
                                : ' ∞'}
                            </Text>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Pagination */}
                  <div className="promotion-list-pagination">
                    <Pagination
                      current={currentPage}
                      pageSize={limit}
                      total={total}
                      onChange={handlePageChange}
                      showSizeChanger={false}
                      showQuickJumper
                    />
                  </div>
                </>
              )}
            </Card>
          </Col>

          {/* Right Column - Promotion Detail */}
          <Col xs={24} lg={14}>
            <Card
              className="promotion-detail-card"
              title={
                <div className="promotion-detail-card-title">
                  <TagOutlined className="promotion-detail-card-icon" />
                  <span>Chi tiết khuyến mãi</span>
                </div>
              }
            >
              {selectedPromotion ? (
                <div className="promotion-detail-content">
                  {/* Promotion Header */}
                  <div className="promotion-detail-header">
                    <div className="promotion-detail-title-section">
                      <Title level={3} className="promotion-detail-title">
                        {selectedPromotion.title}
                      </Title>
                      <div className="promotion-detail-code">
                        <Text type="secondary">Mã: {selectedPromotion.code}</Text>
                      </div>
                    </div>
                    <div className="promotion-detail-discount-badge">
                      <DollarOutlined className="promotion-discount-icon" />
                      <Text strong className="promotion-discount-value">
                        {getDiscountText(selectedPromotion.discountType, selectedPromotion.discountValue)}
                      </Text>
                    </div>
                  </div>

                  <Divider />

                  {/* Promotion Description */}
                  <div className="promotion-detail-section">
                    <Title level={5} className="promotion-detail-section-title">
                      Mô tả khuyến mãi
                    </Title>
                    <Paragraph className="promotion-detail-description">
                      {selectedPromotion.description}
                    </Paragraph>
                  </div>

                  {/* Promotion Details */}
                  <Row gutter={[16, 16]} className="promotion-detail-section">
                    <Col xs={24} md={12}>
                      <div className="promotion-detail-info-item">
                        <CalendarOutlined className="promotion-detail-info-icon" />
                        <div className="promotion-detail-info-content">
                          <Text className="promotion-detail-info-label">Ngày bắt đầu</Text>
                          <Text strong className="promotion-detail-info-value">
                            {formatDate(selectedPromotion.startDate)}
                          </Text>
                        </div>
                      </div>
                    </Col>
                    <Col xs={24} md={12}>
                      <div className="promotion-detail-info-item">
                        <CalendarOutlined className="promotion-detail-info-icon" />
                        <div className="promotion-detail-info-content">
                          <Text className="promotion-detail-info-label">Ngày kết thúc</Text>
                          <Text strong className="promotion-detail-info-value">
                            {formatDate(selectedPromotion.endDate)}
                          </Text>
                        </div>
                      </div>
                    </Col>
                    <Col xs={24} md={12}>
                      <div className="promotion-detail-info-item">
                        <DollarOutlined className="promotion-detail-info-icon" />
                        <div className="promotion-detail-info-content">
                          <Text className="promotion-detail-info-label">Giá trị đơn tối thiểu</Text>
                          <Text strong className="promotion-detail-info-value">
                            {selectedPromotion.minOrderValue?.toLocaleString() ?? '0'} VNĐ
                          </Text>
                        </div>
                      </div>
                    </Col>
                    <Col xs={24} md={12}>
                      <div className="promotion-detail-info-item">
                        <PercentageOutlined className="promotion-detail-info-icon" />
                        <div className="promotion-detail-info-content">
                          <Text className="promotion-detail-info-label">Giảm tối đa</Text>
                          <Text strong className="promotion-detail-info-value">
                            {selectedPromotion.maxDiscountAmount?.toLocaleString() ?? '0'} VNĐ
                          </Text>
                        </div>
                      </div>
                    </Col>
                    <Col xs={24} md={12}>
                      <div className="promotion-detail-info-item">
                        <TagOutlined className="promotion-detail-info-icon" />
                        <div className="promotion-detail-info-content">
                          <Text className="promotion-detail-info-label">Số lượt sử dụng</Text>
                          <Text strong className="promotion-detail-info-value">
                            {selectedPromotion.usedCount} /
                            {selectedPromotion.usageLimit !== null
                              ? ` ${selectedPromotion.usageLimit}`
                              : ' ∞'}
                          </Text>
                        </div>
                      </div>
                    </Col>
                  </Row>

                  {/* Usage Instructions */}
                  <div className="promotion-detail-section">
                    <Title level={5} className="promotion-detail-section-title">
                      Hướng dẫn sử dụng
                    </Title>
                    <div className="promotion-usage-steps">
                      <div className="promotion-usage-step">
                        <div className="promotion-step-number">1</div>
                        <Text>Nhập mã <strong>{selectedPromotion.code}</strong> tại bước thanh toán</Text>
                      </div>
                      <div className="promotion-usage-step">
                        <div className="promotion-step-number">2</div>
                        <Text>
                          Đảm bảo giá trị đơn hàng đạt tối thiểu {selectedPromotion.minOrderValue?.toLocaleString() ?? '0'} VNĐ
                        </Text>
                      </div>
                      <div className="promotion-usage-step">
                        <div className="promotion-step-number">3</div>
                        <Text>Hệ thống sẽ tự động áp dụng khuyến mãi</Text>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="promotion-detail-empty">
                  <TagOutlined className="promotion-detail-empty-icon" />
                  <Text type="secondary">
                    {promotions.length === 0 ? "Hiện tại không có khuyến mãi nào" : "Chọn một khuyến mãi để xem chi tiết"}
                  </Text>
                </div>
              )}
            </Card>
          </Col>
        </Row>
      </div>

      <FooterHome />
    </div>
  );
};

export default PromotionList;
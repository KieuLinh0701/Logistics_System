import React, { useState, useEffect } from "react";
import { Card, Row, Col, Typography, Button, Spin } from "antd";
import {
    ArrowRightOutlined,
    UserAddOutlined,
    FileAddOutlined,
    InboxOutlined,
    EyeOutlined,
    TruckOutlined,
    DollarCircleOutlined,
    GiftOutlined,
    SyncOutlined
} from "@ant-design/icons";
import "./ServiceTypes.css";
import servicesHeroImage from "../../assets/images/serviceTypes.jpg";
import HeaderHome from "../../components/common/HeaderHome";
import FooterHome from "../../components/common/FooterHome";
import type { ServiceType } from "../../types/serviceType";
import { useNavigate } from "react-router-dom";
import serviceTypeApi from "../../api/serviceTypeApi";

const { Title, Paragraph } = Typography;
const { Meta } = Card;

const ServiceTypes: React.FC = () => {
    const navigate = useNavigate();
    const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchServiceTypes = async () => {
        try {
            setLoading(true);
            const response = await serviceTypeApi.getActiveServiceTypes();
            if (response.success && response.data) {
                setServiceTypes(response.data);
            }
        } catch (error) {
            console.error("Error fetching Service types:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleShippingRates = () => {
        navigate("/info/shipping-rates");
    };

    useEffect(() => {
        fetchServiceTypes();
    }, []);

    const freeServices = [
        {
            icon: <GiftOutlined />,
            title: "MIỄN PHÍ LẤY HÀNG",
            description: "UTE Logistics sẽ không thu phí lấy hàng cho các đơn hàng nhân viên giao nhận đến tận nơi để nhận hàng."
        },
        {
            icon: <SyncOutlined />,
            title: "MIỄN PHÍ GIAO LẠI",
            description: "Trong trường hợp giao hàng lần đầu không thành công, UTE Logistics sẽ miễn phí giao lại 2 lần tiếp theo trước khi hoàn trả hàng cho người gửi."
        }
    ];

    const processSteps = [
        {
            icon: <UserAddOutlined />,
            title: "ĐĂNG NHẬP/ ĐĂNG KÝ",
            description: "Đăng nhập hoặc tạo tài khoản mới trên website UTE Logistics để bắt đầu."
        },
        {
            icon: <FileAddOutlined />,
            title: "TẠO ĐƠN HÀNG",
            description: "Tạo đơn hàng trên website UTE Logistics, hoặc ghé các điểm gửi hàng của UTE Logistics trên toàn quốc."
        },
        {
            icon: <InboxOutlined />,
            title: "LẤY HÀNG",
            description: "Bàn giao hàng cần gửi cho tài xế UTE Logistics tại địa chỉ người gửi cung cấp."
        },
        {
            icon: <EyeOutlined />,
            title: "THEO DÕI TÌNH TRẠNG ĐƠN HÀNG",
            description: "Người gửi quản lý và theo dõi tình trạng đơn hàng thông qua website UTE Logistics."
        },
        {
            icon: <TruckOutlined />,
            title: "GIAO HÀNG",
            description: "UTE Logistics giao hàng cho người nhận, thu tiền hộ (COD) theo yêu cầu của người gửi."
        },
        {
            icon: <DollarCircleOutlined />,
            title: "NHẬN TIỀN THU HỘ",
            description: "UTE Logistics hoàn trả tiền thu hộ cho người gửi thông qua tài khoản ngân hàng xuyên suốt các ngày trong tuần."
        }
    ];

    return (
        <>
            <HeaderHome />

            <div className="service-types-page">
                <div
                    className="service-types-hero"
                    style={{
                        backgroundImage: `url(${servicesHeroImage})`,
                        backgroundSize: "cover",
                        backgroundPosition: "center",
                        backgroundRepeat: "no-repeat",
                    }}
                >
                    <div className="service-types-hero-overlay">
                        <div className="service-types-hero-content">
                            <Title level={2} className="service-types-hero-title">
                                Dịch Vụ Vận Chuyển
                            </Title>
                            <Paragraph className="service-types-hero-subtitle">
                                Khám phá các dịch vụ vận chuyển chuyên nghiệp với nhiều ưu đãi hấp dẫn
                            </Paragraph>
                        </div>
                    </div>
                </div>

                <div className="service-types-container">
                    {/* Services Section */}
                    <div className="services-section">
                        <div className="services-container-inner">
                            <div className="section-header">
                                <Title level={2} className="section-title">
                                    Các Loại Dịch Vụ Vận Chuyển
                                </Title>
                                <Paragraph className="section-subtitle">
                                    Đa dạng dịch vụ đáp ứng mọi nhu cầu vận chuyển của bạn
                                </Paragraph>
                            </div>

                            {loading ? (
                                <div className="loading-container">
                                    <Spin size="large" />
                                </div>
                            ) : serviceTypes.length > 0 ? (
                                <div className="services-grid">
                                    {serviceTypes.map((service) => (
                                        <div key={service.id} className="service-card">
                                            <Title level={3} className="service-name">
                                                {service.name}
                                            </Title>
                                            <div className="delivery-info">
                                                <span className="delivery-label">Thời gian giao hàng <span className="delivery-time">{service.deliveryTime}</span></span>
                                            </div>
                                            <Paragraph className="service-description">
                                                {service.description}
                                            </Paragraph>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="no-services">
                                    <Paragraph>Hiện không có dịch vụ nào.</Paragraph>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Price Check Section */}
                    <div className="price-check-section">
                        <Card className="price-check-card">
                            <Row align="middle" justify="space-between">
                                <Col xs={24} md={16}>
                                    <Title level={2} className="price-check-title">
                                        Tra Cứu Bảng Giá
                                    </Title>
                                    <Paragraph className="price-check-description">
                                        Kiểm tra ngay bảng giá vận chuyển cập nhật mới nhất để lựa chọn dịch vụ phù hợp nhất với nhu cầu của bạn.
                                    </Paragraph>
                                </Col>
                                <Col xs={24} md={8} className="price-check-action">
                                    <Button
                                        type="primary"
                                        size="large"
                                        onClick={handleShippingRates}
                                        className="price-check-btn"
                                    >
                                        Tra Cứu Ngay <ArrowRightOutlined />
                                    </Button>
                                </Col>
                            </Row>
                        </Card>
                    </div>

                    {/* Free Services Section */}
                    <div className="free-services-section">
                        <div className="section-header">
                            <Title level={2} className="section-title">
                                Dịch Vụ Miễn Phí
                            </Title>
                            <Paragraph className="section-subtitle">
                                Những ưu đãi đặc biệt dành cho khách hàng của chúng tôi
                            </Paragraph>
                        </div>

                        <div className="free-services-grid">
                            {freeServices.map((service, index) => (
                                <Card
                                    key={index}
                                    className="free-service-card"
                                    hoverable
                                    bordered={false}
                                >
                                    <div className="free-service-icon">
                                        {service.icon}
                                    </div>
                                    <Meta
                                        title={service.title}
                                        description={service.description}
                                    />
                                </Card>
                            ))}
                        </div>
                    </div>

                    {/* Process Section */}
                    <div className="process-section">
                        <div className="section-header">
                            <Title level={2} className="section-title">
                                Quy Trình Giao Nhận
                            </Title>
                            <Paragraph className="section-subtitle">
                                Quy trình đơn giản, minh bạch và hiệu quả
                            </Paragraph>
                        </div>

                        <div className="process-steps-container">
                            <div className="process-steps-line"></div>
                            <div className="process-steps-grid">
                                {processSteps.map((step, index) => (
                                    <div key={index} className="process-step-item">
                                        <div className="process-step-icon">
                                            {step.icon}
                                        </div>
                                        <div className="process-step-content">
                                            <Title level={4} className="process-step-title">
                                                {step.title}
                                            </Title>
                                            <Paragraph className="process-step-description">
                                                {step.description}
                                            </Paragraph>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <FooterHome />
        </>
    );
};

export default ServiceTypes;
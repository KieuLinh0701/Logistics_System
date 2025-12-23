import React, { useEffect, useState } from "react";
import { Typography, message, Form, Button, Input, Spin } from "antd";
import dayjs from "dayjs";
import companyInfoImage from "../../../assets/images/orderTracking.jpg";
import HeaderHome from "../../../components/common/HeaderHome";
import FooterHome from "../../../components/common/FooterHome";
import "./OrderTracking.css";
import type { OrderHistory } from "../../../types/orderHistory";
import { useParams, useNavigate } from "react-router-dom";
import orderApi from "../../../api/orderApi";
import { getOrderHistoryActionText } from "../../../utils/orderHistoryUtils";

const { Title, Paragraph } = Typography;

const OrderTracking: React.FC = () => {
    const [form] = Form.useForm();
    const { trackingNumber: trackingNumberFromUrl } = useParams();
    const navigate = useNavigate();
    const [orderHistories, setOrderHistories] = useState<OrderHistory[] | []>([]);
    const [loading, setLoading] = useState(false);

    const fetchOrderTracking = async (trackingNumber?: string) => {
        const tracking = trackingNumber || form.getFieldValue("trackingNumber");
        if (!tracking) {
            message.warning("Vui lòng nhập mã vận đơn");
            return;
        }

        try {
            setLoading(true);
            const result = await orderApi.getPublicOrderByTrackingNumber(tracking);
            if (result.success) {
                const sortedHistories = (result.data || []).sort(
                    (a, b) => new Date(b.actionTime).getTime() - new Date(a.actionTime).getTime()
                );
                setOrderHistories(sortedHistories);

                navigate(`/tracking/order-tracking/${tracking}`, { replace: true });
            } else {
                setOrderHistories([]);
                message.info("Không tìm thấy vận đơn");
            }
        } catch (error) {
            message.error("Không thể tải thông tin vận đơn");
            setOrderHistories([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (trackingNumberFromUrl) {
            form.setFieldsValue({ trackingNumber: trackingNumberFromUrl });
            fetchOrderTracking(trackingNumberFromUrl);
        }
    }, [trackingNumberFromUrl]);

    return (
        <div>
            <HeaderHome />
            <div className="order-tracking-page">
                <div
                    className="order-tracking-hero"
                    style={{
                        backgroundImage: `url(${companyInfoImage})`,
                        backgroundSize: "cover",
                        backgroundPosition: "center",
                        backgroundRepeat: "no-repeat",
                    }}
                >
                    <div className="order-tracking-hero-overlay">
                        <div className="order-tracking-hero-content">
                            <Title level={2} className="order-tracking-hero-title">
                                Tra cứu vận đơn
                            </Title>
                            <Paragraph className="order-tracking-hero-subtitle">
                                Theo dõi hành trình đơn hàng của bạn một cách chi tiết và trực quan
                            </Paragraph>
                        </div>
                    </div>
                </div>

                <div className="order-tracking-container">
                    <div className="order-tracking-section">
                        <Title level={2} className="order-tracking-title">
                            Tra cứu vận đơn
                        </Title>

                        <Form form={form} layout="vertical" className="order-tracking-form">
                            <Form.Item
                                name="trackingNumber"
                                label={<span className="order-tracking-form-label">Mã vận đơn</span>}
                                rules={[{ required: true, message: "Vui lòng nhập mã vận đơn!" }]}
                            >
                                <Input
                                    className="order-tracking-input"
                                    placeholder="Nhập mã đơn hàng của bạn..."
                                    size="large"
                                />
                            </Form.Item>

                            <Form.Item>
                                <Button
                                    type="primary"
                                    className="order-tracking-btn"
                                    onClick={() => fetchOrderTracking()}
                                    loading={loading}
                                    size="large"
                                >
                                    Tra cứu
                                </Button>
                            </Form.Item>
                        </Form>

                        {loading ? (
                            <div className="order-tracking-loading">
                                <Spin size="large" />
                                <Paragraph>Đang tải thông tin vận đơn...</Paragraph>
                            </div>
                        ) : (
                            orderHistories.length > 0 && (
                                <div className="order-tracking-result">
                                    <Title level={3} className="order-tracking-result-title">
                                        Lịch sử đơn hàng
                                    </Title>

                                    <div className="order-tracking-timeline">
                                        {orderHistories.map((item, index) => (
                                            <div
                                                key={index}
                                                className="order-tracking-timeline-item"
                                            >
                                                <div className="timeline-dot-container">
                                                    <div className="timeline-dot"></div>
                                                    {index !== orderHistories.length - 1 && (
                                                        <div className="timeline-arrow">
                                                            <div className="arrow-line"></div>
                                                            <div className="arrow-head"></div>
                                                        </div>
                                                    )}
                                                </div>

                                                <div className="timeline-content">
                                                    <div className="timeline-action">
                                                        {getOrderHistoryActionText(item)}
                                                    </div>
                                                    <div className="timeline-time">
                                                        {dayjs(item.actionTime).format("DD/MM/YYYY HH:mm:ss")}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )
                        )}
                    </div>
                </div>
            </div>
            <FooterHome />
        </div>
    );
};

export default OrderTracking;
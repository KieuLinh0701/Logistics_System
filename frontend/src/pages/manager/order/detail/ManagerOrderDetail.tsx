import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { message, Descriptions, Typography, Timeline } from "antd";

import Header from "./components/Header";
import OrderSenderRecipient from "./components/SenderRecipientInfo";
import OrderInfo from "./components/OrderInfo";
import OrderProducts from "./components/ProductsInfo";
import OrderPayment from "./components/PaymentInfo";
import OrderActions from "./components/Actions";
import OrderHistoryCard from "./components/OrderHistoryCard";
import type { Order, OrderFulfillmentSummary } from "../../../../types/order";
import "./ManagerOrderDetail.css";
import orderApi from "../../../../api/orderApi";
import ConfirmCancelModal from "./components/ConfirmCancelModal";
import { canAtOriginOfficeManagerOrder, translateOrderStatus, canCancelManagerOrder, canConfirmManagerOrder, canEditManagerOrder, canPrintManagerOrder, translatePickupAttemptStatus, translatePickupFailReason, type OrderCreatorType, type OrderPickupType, type OrderStatus } from "../../../../utils/orderUtils";
import OfficeInfo from "./components/OfficeInfo";
import ConfirmModal from "../../../common/ConfirmModal";


const { Title } = Typography;

const UserOrderDetail: React.FC = () => {
    const { trackingNumber } = useParams();

    const navigate = useNavigate();

    const [order, setOrder] = useState<Order | null>(null);
    const [loading, setLoading] = useState(false);
    const [loadingView, setLoadingView] = useState(true);

    const [cancelModalOpen, setCancelModalOpen] = useState(false);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);
    const [confirmModalOpen, setConfirmModalOpen] = useState(false);


    const [summary, setSummary] = useState<OrderFulfillmentSummary | null>(null);
    const [summaryLoading, setSummaryLoading] = useState(false);

    const fetchSummary = async (orderId?: number) => {
        if (!orderId) {
            setSummary(null);
            return;
        }
        setSummaryLoading(true);
        try {
            const res = await orderApi.getFulfillmentSummary(orderId);
            if (res.success && res.data) {
                setSummary(res.data);
            } else {
                setSummary(null);
            }
        } catch {
            setSummary(null);
        } finally {
            setSummaryLoading(false);
        }
    };

    useEffect(() => {
        if (order?.id) fetchSummary(order.id);
    }, [order?.id]);

    const fetchOrder = async () => {
        setLoadingView(true);
        try {
            let result;

            if (trackingNumber) {
                result = await orderApi.getManagerOrderByTrackingNumber(trackingNumber);
            } else {
                return;
            }

            if (result.success) {
                setOrder(result.data);
            } else {
                message.error(result.message);
            }

        } catch (error: any) {
            message.error(error.message || "Lỗi tải đơn hàng");
        } finally {
            setLoadingView(false);
        }
    };

    useEffect(() => {
        fetchOrder();
    }, [trackingNumber]);

    const handleEditOrder = () => {
        if (!order?.id) return;

        if (order?.trackingNumber) {
            navigate(`/orders/tracking/${order.trackingNumber}/edit`);
        }
    };

    const handleCancelOrder = () => {
        setCancelModalOpen(true);
    };

    const confirmCancelOrder = async () => {
        setLoading(true);
        try {
            if (!order?.id) return;
            const result = await orderApi.cancelManagerOrder(order.id);
            if (result.success && result.data) {
                message.success("Hủy đơn hàng thành công");
                fetchOrder();
            } else {
                message.error(result.message || "Hủy đơn thất bại");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi hủy đơn hàng");
        } finally {
            setLoading(false);
            setCancelModalOpen(false);
        }
    };

    const handlePrintOrder = async () => {
        if (!order) return;
        navigate(`/orders/print?orderIds=${order.id}`);
    };

    const handleAtOriginOfficeOrder = () => {
        setModalConfirmOpen(true);
    };

    const confirmAtOriginOfficeOrder = async () => {
        setModalConfirmOpen(false);

        try {
            setLoading(true);
            if (!order) return;

            const result = await orderApi.setManagerOrderAtOriginOffice(order.id);

            if (result.success) {
                message.success(result.message || "Đơn hàng đã bàn giao cho bưu cục xuất phát thành công.");
                fetchOrder();
            } else {
                message.error(result.message || "Có lỗi khi bàn giao đơn hàng cho bưu cục xuất phát!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi khi bàn giao đơn hàng cho bưu cục xuất phát!");
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = () => {
        setConfirmModalOpen(true);
    };

    const confirmConfirmOrder = async () => {
        if (!order) return;

        setConfirmModalOpen(false);

        try {
            setLoading(true);

            const result = await orderApi.confirmManagerOrder(order.id);

            if (result.success) {
                message.success(result.message || "Xác nhận đơn hàng thành công.");
                fetchOrder();
            } else {
                message.error(result.message || "Có lỗi khi xác nhận đơn hàng!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi xác nhận đơn hàng!");
        } finally {
            setLoading(false);
        }
    };

    if (loadingView || !order) {
        return <div>Đang tải chi tiết đơn hàng...</div>;
    }

    const canEdit = canEditManagerOrder(order.status);
    const canCancel = canCancelManagerOrder(order.status as OrderStatus, order.createdByType as OrderCreatorType);
    const canPrint = canPrintManagerOrder(order.status);
    const canSetAtOriginOffice = canAtOriginOfficeManagerOrder(order.status);
    const canConfirm = canConfirmManagerOrder(order.status as OrderStatus, order.pickupType as OrderPickupType);

    return (
        <div className="order-detail container">
            <Header
                trackingNumber={order.trackingNumber!}
            />
            <OrderSenderRecipient
                sender={{
                    name: order.senderName,
                    phone: order.senderPhone,
                    detail: order.senderDetail,
                    wardCode: order.senderWardCode,
                    cityCode: order.senderCityCode,
                }}
                recipient={{
                    name: order.recipientAddress.name,
                    phone: order.recipientAddress.phoneNumber,
                    detail: order.recipientAddress.detail,
                    wardCode: order.recipientAddress.wardCode,
                    cityCode: order.recipientAddress.cityCode,
                }}
            />
            <OrderInfo order={order} />
            <OfficeInfo
                fromOffice={order.fromOffice}
                toOffice={order.toOffice} />
            <OrderProducts products={order.orderProducts || []} />
            <OrderHistoryCard histories={order.orderHistories} />
            <div className="order-detail-card">
                <Title level={5} className="order-detail-card-title order-detail-card-title-main">
                    Lịch sử lấy hàng
                </Title>
                <Timeline
                    items={(order.pickupAttempts || []).map((attempt) => ({
                        color: attempt.status === "SUCCESS" ? "green" : "red",
                        children: (
                            <div>
                                <div>
                                    Lần thử #{attempt.attemptNumber} - {translatePickupAttemptStatus(attempt.status)}
                                </div>
                                <div>
                                    {attempt.failReason ? translatePickupFailReason(attempt.failReason) : ""}
                                    {attempt.note ? ` - ${attempt.note}` : ""}
                                </div>
                                <div>
                                    {attempt.attemptedAt ? new Date(attempt.attemptedAt).toLocaleString("vi-VN") : ""}
                                    {attempt.shipperName ? ` - ${attempt.shipperName}` : ""}
                                </div>
                            </div>
                        ),
                    }))}
                />
            </div>
            {/* <FeedbackCard orderId={order.id} orderStatus={order.status} /> */}
            <OrderPayment order={order} />
            <div className="order-detail-card">
            <Title level={5} className="order-detail-card-title order-detail-card-title-main">
                Kết quả giao hàng
            </Title>

            <Descriptions column={2} size="small">

                <Descriptions.Item label="Trạng thái đơn hàng">
                    {order.status
                        ? translateOrderStatus(order.status)
                        : "Không có dữ liệu"}
                </Descriptions.Item>

                <Descriptions.Item label="Sản phẩm đã giao / Tổng sản phẩm">
                    {summaryLoading
                        ? "Đang tải..."
                        : summary
                            ? `${summary.deliveredItems} / ${summary.totalItems}`
                            : "Không có dữ liệu"}
                </Descriptions.Item>

                <Descriptions.Item label="Sản phẩm hoàn trả">
                    {summaryLoading
                        ? "Đang tải..."
                        : (summary?.returnedItems ?? "Không có dữ liệu")}
                </Descriptions.Item>

                <Descriptions.Item label="COD đã thu / COD dự kiến">
                    {summaryLoading
                        ? "Đang tải..."
                        : summary
                            ? `${summary.collectedCOD.toLocaleString()} / ${summary.expectedCOD.toLocaleString()} VNĐ`
                            : "Không có dữ liệu"}
                </Descriptions.Item>

                <Descriptions.Item label="Giá trị hoàn trả">
                    {summaryLoading
                        ? "Đang tải..."
                        : summary
                            ? `${summary.returnedValue.toLocaleString()} VNĐ`
                            : "Không có dữ liệu"}
                </Descriptions.Item>

            </Descriptions>
        </div>
            <OrderActions
                canEdit={canEdit}
                canCancel={canCancel}
                canPrint={canPrint}
                canSetAtOriginOffice={canSetAtOriginOffice}
                canConfirm={canConfirm}
                onEdit={handleEditOrder}
                onCancel={handleCancelOrder}
                onPrint={handlePrintOrder}
                onConfirm={handleConfirm}
                onSetAtOriginOffice={handleAtOriginOfficeOrder}
            />

            <ConfirmCancelModal
                open={cancelModalOpen}
                onOk={confirmCancelOrder}
                onCancel={() => setCancelModalOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận nhận hàng'
                message='Bạn có chắc rằng bạn đã nhận đơn hàng này tại bưu cục để chuyển giao cho đơn vị vận chuyển không?'
                open={modalConfirmOpen}
                onOk={confirmAtOriginOfficeOrder}
                onCancel={() => setModalConfirmOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title="Xác nhận đơn hàng"
                message="Bạn có chắc muốn xác nhận đơn hàng này để bưu cục tiếp nhận và xử lý không?"
                open={confirmModalOpen}
                onOk={confirmConfirmOrder}
                onCancel={() => setConfirmModalOpen(false)}
                loading={loading}
            />

        </div>
    );
};

export default UserOrderDetail;
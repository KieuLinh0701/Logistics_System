import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { message } from "antd";

import Header from "./components/Header";
import OrderSenderRecipient from "./components/SenderRecipientInfo";
import OrderInfo from "./components/OrderInfo";
import OrderProducts from "./components/ProductsInfo";
import OrderPayment from "./components/PaymentInfo";
import OrderActions from "./components/Actions";
import OrderHistoryCard from "./components/OrderHistoryCard";
import type { Order } from "../../../../types/order";
import "./ManagerOrderDetail.css";
import orderApi from "../../../../api/orderApi";
import ConfirmCancelModal from "./components/ConfirmCancelModal";
import { canAtOriginOfficeManagerOrder, canCancelManagerOrder, canConfirmManagerOrder, canEditManagerOrder, canPrintManagerOrder, type OrderCreatorType, type OrderPickupType, type OrderStatus } from "../../../../utils/orderUtils";
import OfficeInfo from "./components/OfficeInfo";
import ConfirmModal from "../../../common/ConfirmModal";

const UserOrderDetail: React.FC = () => {
    const { trackingNumber } = useParams();

    const navigate = useNavigate();

    const [order, setOrder] = useState<Order | null>(null);
    const [loading, setLoading] = useState(true);
    const [loadingView, setLoadingView] = useState(true);

    const [cancelModalOpen, setCancelModalOpen] = useState(false);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);
    const [confirmModalOpen, setConfirmModalOpen] = useState(false);

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
            {/* <FeedbackCard orderId={order.id} orderStatus={order.status} /> */}
            <OrderPayment order={order} />
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
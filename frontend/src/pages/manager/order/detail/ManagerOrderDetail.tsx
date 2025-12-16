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
import { canAtOriginOfficeManagerOrder, canCancelUserOrder, canEditUserOrder, canPrintUserOrder } from "../../../../utils/orderUtils";
import OfficeInfo from "./components/OfficeInfo";
import ConfirmModal from "../../../common/ConfirmModal";

const UserOrderDetail: React.FC = () => {
    const { trackingNumber } = useParams();

    const navigate = useNavigate();

    const [order, setOrder] = useState<Order | null>(null);
    const [loading, setLoading] = useState(true);

    const [cancelModalOpen, setCancelModalOpen] = useState(false);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);

    const fetchOrder = async () => {
        setLoading(true);
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

        } catch (e) {
            message.error("Lỗi tải đơn hàng");
        } finally {
            setLoading(false);
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
            const result = await orderApi.cancelUserOrder(order.id);
            if (result.success && result.data) {
                message.success("Hủy đơn hàng thành công");
                fetchOrder();
            } else {
                message.error(result.message || "Hủy đơn thất bại");
            }
        } catch (error: any) {
            message.error("Lỗi server khi hủy đơn hàng");
            console.log("Lỗi server khi hủy đơn hàng: ", error.message)
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

    if (loading || !order) {
        return <div>Đang tải chi tiết đơn hàng...</div>;
    }

    const canEdit = canEditUserOrder(order.status);
    const canCancel = canCancelUserOrder(order.status);
    const canPrint = canPrintUserOrder(order.status);
    const canSetAtOriginOffice = canAtOriginOfficeManagerOrder(order.status);

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
                onEdit={handleEditOrder}
                onCancel={handleCancelOrder}
                onPrint={handlePrintOrder}
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
                message='Vui lòng xác nhận rằng bạn đã nhận đơn hàng tại bưu cục để chuyển giao cho đơn vị vận chuyển.'
                open={modalConfirmOpen}
                onOk={confirmAtOriginOfficeOrder}
                onCancel={() => setModalConfirmOpen(false)}
                loading={loading}
            />

        </div>
    );
};

export default UserOrderDetail;
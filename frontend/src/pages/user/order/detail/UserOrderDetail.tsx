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
import "./UserOrderDetail.css";
import orderApi from "../../../../api/orderApi";
import ConfirmCancelModal from "./components/ConfirmCancelModal";
import ConfirmPublicModal from "./components/ConfirmPublicModal";
import { canCancelUserOrder, canDeleteUserOrder, canEditUserOrder, canPrintUserOrder, canPublicUserOrder, canReadyUserOrder } from "../../../../utils/orderUtils";
import ConfirmDeleteModal from "./components/ConfirmDeleteModal";
import AddEditModal from "../request/components/AddEditModal";
import FromOfficeInfo from "./components/FromOfficeInfo";
import ConfirmModal from "../../../common/ConfirmModal";
import { canCreateUserShippingRequestFromOrderDetail } from "../../../../utils/shippingRequestUtils";
import userApi from "../../../../api/userApi";

const UserOrderDetail: React.FC = () => {
    const { trackingNumber, orderId } = useParams();

    const navigate = useNavigate();

    const [order, setOrder] = useState<Order | null>(null);
    const [loading, setLoading] = useState(false);
    const [loadingView, setLoadingView] = useState(false);

    const [cancelModalOpen, setCancelModalOpen] = useState(false);
    const [publicModalOpen, setPublicModalOpen] = useState(false);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [requestModalOpen, setRequestModalOpen] = useState(false);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);

    const [userLocked, setUserLocked] = useState<Boolean>(false);

    const fetchOrder = async () => {
        setLoadingView(true);
        try {
            let result;

            if (orderId) {
                result = await orderApi.getUserOrderById(Number(orderId));
            } else if (trackingNumber) {
                result = await orderApi.getUserOrderByTrackingNumber(trackingNumber);
            } else {
                return;
            }

            if (result.success) {
                setOrder(result.data);
            } else {
                message.error(result.message);
            }

            const lockRes = await userApi.checkUserLocked();
            if (lockRes.success && lockRes.data != null) {
                setUserLocked(lockRes.data);
            } else {
                message.error(lockRes.message || "Lỗi khi kiểm tra trạng thái khóa");
            }

        } catch (e: any) {
            message.error(e.message || "Lỗi tải đơn hàng");
        } finally {
            setLoadingView(false);
        }
    };

    useEffect(() => {
        fetchOrder();
    }, [orderId, trackingNumber]);

    const handleEditOrder = () => {
        if (!order?.id) return;

        if (order?.trackingNumber) {
            navigate(`/orders/tracking/${order.trackingNumber}/edit`);
        } else {
            navigate(`/orders/id/${order.id}/edit`);
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
            message.error(error.message || "Lỗi server khi hủy đơn hàng");
        } finally {
            setLoading(false);
            setCancelModalOpen(false);
        }
    };

    const handlePublicOrder = () => {
        if (userLocked) {
            message.error("Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi chuyển đơn hàng sang xử lý.");
            return;
        }

        setPublicModalOpen(true);
    };

    const confirmPublicOrder = async () => {
        setLoading(true);
        try {
            if (!order) return;
            const result = await orderApi.publicUserOrder(order.id);

            if (result.success && result.data) {
                message.success(result.message || "Đã chuyển đơn hàng sang xử lý thành công");

                navigate(`/orders/tracking/${result.data}`, { replace: true });
            } else {
                message.error(result.message || "Chuyển trạng thái thất bại");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi chuyển trạng thái đơn hàng");
        } finally {
            setPublicModalOpen(false);
            setLoading(false);
        }
    };

    const handleDeleteOrder = () => {
        setDeleteModalOpen(true);
    };

    const confirmDeleteOrder = async () => {
        setLoading(true);
        try {
            if (!order) return;
            const result = await orderApi.deleteUserOrder(order.id);

            if (result.success && result.data) {
                message.success(result.message || "Xóa đơn hàng thành công");
                navigate(-1);
            } else {
                message.error(result.message || "Xóa đơn hàng thất bại");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi xóa đơn hàng");
        } finally {
            setDeleteModalOpen(false);
            setLoading(false);
        }
    };

    const handlePrintOrder = async () => {
        if (!order) return;
        navigate(`/orders/print?orderIds=${order.id}`);
    };

    const handleCreateShippingRequest = () => {
        setRequestModalOpen(true);
    };

    const handleReadyOrder = () => {
        setModalConfirmOpen(true);
    };

    const confirmReadyOrder = async () => {
        setModalConfirmOpen(false);

        try {
            if (!order) return;
            setLoading(true);

            const result = await orderApi.setUserOrderReadyForPickup(order.id);

            if (result.success) {
                message.success(result.message || "Chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy' thành công.");
                fetchOrder();
            } else {
                message.error(result.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy'!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy'!");
        } finally {
            setLoading(false);
        }
    };


    if (loadingView || !order) {
        return <div>Đang tải chi tiết đơn hàng...</div>;
    }

    const canEdit = canEditUserOrder(order.status);
    const canPublic = canPublicUserOrder(order.status);
    const canCancel = canCancelUserOrder(order.status);
    const canPrint = canPrintUserOrder(order.status);
    const canDelete = canDeleteUserOrder(order.status);
    const canReady = canReadyUserOrder(order.status);
    const canRequets = canCreateUserShippingRequestFromOrderDetail(order.status);

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
            {order.pickupType === "AT_OFFICE" &&
                <FromOfficeInfo office={order.fromOffice} />
            }
            <OrderInfo order={order} />
            <OrderProducts products={order.orderProducts || []} />
            <OrderHistoryCard histories={order.orderHistories} />
            <OrderPayment order={order} />
            <OrderActions
                canPublic={canPublic}
                canEdit={canEdit}
                canCancel={canCancel}
                canPrint={canPrint}
                canDelete={canDelete}
                canRequest={canRequets}
                canReady={canReady}
                onPublic={handlePublicOrder}
                onEdit={handleEditOrder}
                onCancel={handleCancelOrder}
                onPrint={handlePrintOrder}
                onDelete={handleDeleteOrder}
                onReady={handleReadyOrder}
                onCreateRequest={handleCreateShippingRequest}
            />

            <ConfirmCancelModal
                open={cancelModalOpen}
                onOk={confirmCancelOrder}
                onCancel={() => setCancelModalOpen(false)}
                loading={loading}
            />

            <ConfirmPublicModal
                open={publicModalOpen}
                onOk={confirmPublicOrder}
                onCancel={() => setPublicModalOpen(false)}
                loading={loading}
            />

            <ConfirmDeleteModal
                open={deleteModalOpen}
                onOk={confirmDeleteOrder}
                onCancel={() => setDeleteModalOpen(false)}
                loading={loading}
            />

            <AddEditModal
                open={requestModalOpen}
                mode="create"
                request={{ orderTrackingNumber: order.trackingNumber }}
                onSuccess={() => fetchOrder()}
                onCancel={() => setRequestModalOpen(false)}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng đã sẵn sàng'
                message='Bạn có chắc chắn đơn hàng này đã sẵn sàng để bàn giao cho đơn vị vận chuyển không?'
                open={modalConfirmOpen}
                onOk={confirmReadyOrder}
                onCancel={() => setModalConfirmOpen(false)}
                loading={loading}
            />

        </div>
    );
};

export default UserOrderDetail;
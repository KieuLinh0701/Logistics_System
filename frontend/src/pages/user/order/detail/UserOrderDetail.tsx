import React, {useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {Descriptions, message, Table, Typography} from "antd";

import Header from "./components/Header";
import OrderSenderRecipient from "./components/SenderRecipientInfo";
import OrderInfo from "./components/OrderInfo";
import OrderProducts from "./components/ProductsInfo";
import OrderPayment from "./components/PaymentInfo";
import OrderActions from "./components/Actions";
import OrderHistoryCard from "./components/OrderHistoryCard";
import type {Order, OrderFulfillmentSummary} from "../../../../types/order";
import "./UserOrderDetail.css";
import orderApi from "../../../../api/orderApi";
import ConfirmCancelModal from "./components/ConfirmCancelModal";
import ConfirmPublicModal from "./components/ConfirmPublicModal";
import {
    canCancelUserOrder,
    canDeleteUserOrder,
    canEditUserOrder,
    canPrintUserOrder,
    canPublicUserOrder,
    canReadyUserOrder,
    canTransitToOfficeUserOrder
} from "../../../../utils/orderUtils";
import ConfirmDeleteModal from "./components/ConfirmDeleteModal";
import AddEditModal from "../request/components/AddEditModal";
import FromOfficeInfo from "./components/FromOfficeInfo";
import ConfirmModal from "../../../common/ConfirmModal";
import {canCreateUserShippingRequestFromOrderDetail} from "../../../../utils/shippingRequestUtils";
import userApi from "../../../../api/userApi";
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";


const {Title} = Typography;

const UserOrderDetail: React.FC = () => {
    const {trackingNumber, orderId} = useParams();

    const navigate = useNavigate();

    const [order, setOrder] = useState<Order | null>(null);
    const [loading, setLoading] = useState(false);
    const [loadingView, setLoadingView] = useState(false);

    const [cancelModalOpen, setCancelModalOpen] = useState(false);
    const [publicModalOpen, setPublicModalOpen] = useState(false);
    const [transitModalOpen, setTransitModalOpen] = useState(false);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [requestModalOpen, setRequestModalOpen] = useState(false);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);

    const [userLocked, setUserLocked] = useState<boolean>(false);


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
            if (result.success) {
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

            if (result.success) {
                message.success("Đã chuyển đơn hàng sang xử lý thành công");

                navigate(`/orders/tracking/${result.data}`, {replace: true});
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

            if (result.success) {
                message.success("Xóa đơn hàng thành công");
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

    const handleTransitionToOffice = () => {
        setTransitModalOpen(true);
    };

    const confirmReadyOrder = async () => {
        setModalConfirmOpen(false);

        try {
            if (!order) return;
            setLoading(true);

            const result = await orderApi.setUserOrderReadyForPickup(order.id);

            if (result.success) {
                message.success("Chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy' thành công.");
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

    const confirmTransitToOfficeOrder = async () => {
        setTransitModalOpen(false);

        try {
            if (!order) return;
            setLoading(true);

            const result = await orderApi.setUserOrderTransitToOffice(order.id);

            if (result.success) {
                message.success("Chuyển đơn hàng sang trạng thái 'Đang chuyển về bưu cục' thành công.");
                fetchOrder();
            } else {
                message.error(result.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Đang chuyển về bưu cục'!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Đang chuyển về bưu cục'!");
        } finally {
            setLoading(false);
        }
    };


    if (loadingView || !order) {
        return <div>Đang tải chi tiết đơn hàng...</div>;
    }

    const canEdit = canEditUserOrder(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_EDIT']);
    const canPublic = canPublicUserOrder(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PROCESS']);
    const canCancel = canCancelUserOrder(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_CANCEL']);
    const canPrint = canPrintUserOrder(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PRINT_SINGLE']);
    const canDelete = canDeleteUserOrder(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_DELETE_DRAFT']);
    const canReady = canReadyUserOrder(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_READY']);
    const canRequets = canCreateUserShippingRequestFromOrderDetail(order.status) && hasPermissionGroup(['GROUP_USER', 'USER_SUPPORT_CREATE']);
    const canTransitToOffice =canTransitToOfficeUserOrder(order.status) && order.pickupType === "AT_OFFICE" && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_TRANSIT_TO_OFFICE']);

    const productColumns = [
        {
            title: "Tên sản phẩm",
            dataIndex: "productName",
            key: "productName",
        },
        {
            title: "Số lượng đặt",
            dataIndex: "quantity",
            key: "quantity",
        },
        {
            title: "Số lượng đã giao",
            dataIndex: "deliveredQuantity",
            key: "deliveredQuantity",
            render: (value: number | undefined) => (
                <span className="custom-table-content-strong">
                    {value ?? 0}
                </span>
            )
        },
        {
            title: "Số lượng hoàn",
            dataIndex: "returnedQuantity",
            key: "returnedQuantity",
            render: (value: number | undefined) => (
                <span className="custom-table-content-error">
                    {value ?? 0}
                </span>
            )
        },
    ];

    return (
        <div className="order-detail container">
            <Header
                trackingNumber={order.trackingNumber!}
            />
            <OrderSenderRecipient
                sender={{
                    name: order.senderName,
                    phone: order.senderPhone,
                    fullAddress: order.senderFullAddress,
                }}
                recipient={{
                    name: order.recipientName,
                    phone: order.recipientPhone,
                    fullAddress: order.recipientFullAddress,
                }}
            />
            {order.pickupType === "AT_OFFICE" &&
                <FromOfficeInfo office={order.fromOffice}/>
            }
            {order.currentOffice && (
                <div className="order-detail-card">
                    <Title level={5} className="order-detail-card-title order-detail-card-title-main">
                        Bưu cục hiện tại
                    </Title>
                    <Descriptions column={2} size="small" bordered>
                        <Descriptions.Item label="Tên">{order.currentOffice.name || "—"}</Descriptions.Item>
                        <Descriptions.Item label="Địa chỉ">
                            {[
                                order.currentOffice.detail,
                                order.currentOffice.wardCode,
                                order.currentOffice.cityCode,
                            ].filter(Boolean).join(", ") || "—"}
                        </Descriptions.Item>
                        <Descriptions.Item label="Điện thoại">{order.currentOffice.phoneNumber || "—"}</Descriptions.Item>
                        <Descriptions.Item label="Email">{order.currentOffice.email || "—"}</Descriptions.Item>
                    </Descriptions>
                </div>
            )}
            <OrderInfo order={order}/>
            <OrderProducts products={order.orderProducts || []}/>

            {order.orderProducts.length !== 0 && (
                <div className="order-detail-card">
                    <Title level={5} className="order-detail-card-title order-detail-card-title-main">
                        Chi tiết giao sản phẩm
                    </Title>
                    <div className="table-container">
                        <Table
                            rowKey={(record) => record.id || record.productId}
                            columns={productColumns}
                            dataSource={order.orderProducts || []}
                            pagination={false}
                            className="list-page-table"
                        />
                    </div>
                </div>
            )}
            <OrderHistoryCard histories={order.orderHistories}/>
            <OrderPayment order={order}/>
            <OrderActions
                canPublic={canPublic}
                canEdit={canEdit}
                canCancel={canCancel}
                canPrint={canPrint}
                canDelete={canDelete}
                canRequest={canRequets}
                canReady={canReady}
                canTransitToOffice={canTransitToOffice}
                onPublic={handlePublicOrder}
                onEdit={handleEditOrder}
                onCancel={handleCancelOrder}
                onPrint={handlePrintOrder}
                onDelete={handleDeleteOrder}
                onReady={handleReadyOrder}
                onTransitToOffice={handleTransitionToOffice}
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
                request={{orderTrackingNumber: order.trackingNumber}}
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

            <ConfirmModal
                title='Xác nhận đơn hàng đang chuyển về bưu cục'
                message='Bạn có chắc chắn đơn hàng này đang chuyển về bưu cục đã chọn trước đó không?'
                open={transitModalOpen}
                onOk={confirmTransitToOfficeOrder}
                onCancel={() => setTransitModalOpen(false)}
                loading={loading}
            />

        </div>
    );
};

export default UserOrderDetail;
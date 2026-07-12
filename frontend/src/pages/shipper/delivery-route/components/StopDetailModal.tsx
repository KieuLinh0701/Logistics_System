import React from "react";
import {Button, Modal, Space, Tag, Typography} from "antd";
import {EnvironmentOutlined} from "@ant-design/icons";

const { Text } = Typography;

interface DeliveryStop {
    id: number;
    trackingNumber: string;
    recipientName: string;
    recipientPhone: string;
    recipientAddress: string;
    recipientFullAddress?: string;
    recipientLatitude?: number | null;
    recipientLongitude?: number | null;
    senderName?: string;
    senderPhone?: string;
    senderAddress?: string;
    senderFullAddress?: string;
    senderLatitude?: number | null;
    senderLongitude?: number | null;
    contactName?: string;
    contactPhone?: string;
    contactAddress?: string;
    codAmount: number;
    priority: string;
    serviceType: string;
    status: string;
    orderStatus?: string;
    stopSequence?: number;
    etaTime?: string;
    latitude?: number;
    longitude?: number;
    stopType?: string;
    shipmentOrderScannedAt?: string | null;
}

interface StopDetailModalProps {
    open: boolean;
    stop: DeliveryStop | null;
    onClose: () => void;
    onFocusOnMap: (stop: DeliveryStop) => void;
    getStatusColor: (status: string) => string;
    getStatusText: (status: string) => string;
}

const StopDetailModal: React.FC<StopDetailModalProps> = ({
    open,
    stop,
    onClose,
    onFocusOnMap,
    getStatusColor,
    getStatusText,
}) => {
    if (!stop) return null;

    const isPickup = stop.stopType === "PICKUP";
    const isReturn = stop.stopType === "RETURN_TO_OFFICE";

    const name = isPickup
        ? (stop.senderName || stop.recipientName)
        : isReturn
        ? stop.senderName
        : stop.recipientName;

    const phone = isPickup
        ? (stop.senderPhone || stop.recipientPhone)
        : isReturn
        ? stop.senderPhone
        : stop.recipientPhone;

    const address = isPickup
        ? (stop.senderAddress || stop.recipientAddress)
        : isReturn
        ? stop.senderAddress
        : stop.recipientAddress;

    const title = isPickup
        ? "Chi tiết điểm lấy hàng"
        : isReturn
        ? "Chi tiết điểm hoàn trả"
        : "Chi tiết điểm giao hàng";

    const statusKey = isPickup
        ? stop.status
        : (stop.orderStatus || stop.status);

    // DELIVERY stop đã claim vào shipment PENDING nhưng shipper chưa quét QR
    // → hiển thị "Chờ quét lên xe" thay vì raw enum AT_DEST_OFFICE.
    const isAwaitingDeliveryScan =
        !isPickup && !isReturn
        && !stop.shipmentOrderScannedAt
        && (statusKey || "").toString().toUpperCase() === "AT_DEST_OFFICE";
    const displayedStatus = isAwaitingDeliveryScan ? "AT_DEST_OFFICE" : statusKey;
    const displayedColor = isAwaitingDeliveryScan
        ? "gold"
        : getStatusColor(displayedStatus);
    const displayedText = isAwaitingDeliveryScan
        ? "Chờ lên xe"
        : getStatusText(displayedStatus);

    return (
        <Modal
            title={title}
            open={open}
            onCancel={onClose}
            footer={null}
            width={600}
        >
            <Space direction="vertical" size="large" style={{ width: "100%" }}>
                <div>
                    <Text strong>Mã đơn hàng: </Text>
                    <Text>{stop.trackingNumber}</Text>
                </div>
                <div>
                    <Text strong>{isPickup ? "Người gửi: " : isReturn ? "Người gửi (Shop): " : "Người nhận: "}</Text>
                    <Text>{name}</Text>
                </div>
                <div>
                    <Text strong>SĐT: </Text>
                    <Text>{phone}</Text>
                </div>
                <div>
                    <Text strong>Địa chỉ: </Text>
                    <Text>{address}</Text>
                </div>
                {!isReturn && (
                    <div>
                        <Text strong>COD: </Text>
                        <Text style={{ color: "#f50" }}>{stop.codAmount.toLocaleString()}đ</Text>
                    </div>
                )}
                <div>
                    <Text strong>Dịch vụ: </Text>
                    <Text>{stop.serviceType}</Text>
                </div>
                <div>
                    <Text strong>Trạng thái: </Text>
                    <Tag color={displayedColor}>{displayedText}</Tag>
                </div>
                <Button
                    type="primary"
                    className="primary-button"
                    block
                    icon={<EnvironmentOutlined />}
                    onClick={() => onFocusOnMap(stop)}
                >
                    Xem trên bản đồ
                </Button>
            </Space>
        </Modal>
    );
};

export default StopDetailModal;

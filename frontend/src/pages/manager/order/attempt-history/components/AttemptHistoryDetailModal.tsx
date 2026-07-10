import React from "react";
import {Button, Descriptions, Image, Modal, Tooltip, Typography} from "antd";
import dayjs from "dayjs";
import {useNavigate} from "react-router-dom";
import type {AttemptHistoryItem} from "../../../../../types/attemptHistory";
import {
    translateAttemptCategory,
    translateAttemptStatus,
    translateFailReason,
} from "../../../../../utils/attemptHistoryUtils";

const {Text} = Typography;

interface Props {
    item: AttemptHistoryItem | null;
    open: boolean;
    onClose: () => void;
}

const AttemptHistoryDetailModal: React.FC<Props> = ({item, open, onClose}) => {
    const navigate = useNavigate();

    if (!item) return null;

    const status = item.pickupStatus || item.deliveryStatus;

    const handleViewOrder = () => {
        if (item.trackingNumber) {
            navigate(`/orders/tracking/${item.trackingNumber}`);
        }
    };

    return (
        <Modal
            open={open}
            onCancel={onClose}
            width={780}
            centered
            title={
                <span className="modal-title">
                    Chi tiết xử lý đơn {item.trackingNumber || ""}
                </span>
            }
            footer={[
                <Button key="close" onClick={onClose}>
                    Đóng
                </Button>,
            ]}
            className="modal-hide-scrollbar"
        >
            <Descriptions bordered column={1} size="middle">
                <Descriptions.Item label="Mã đơn hàng">
                    {item.trackingNumber ? (
                        <Tooltip title="Click để xem chi tiết đơn hàng">
                            <span className="navigate-link-default" onClick={handleViewOrder}>
                                {item.trackingNumber}
                            </span>
                        </Tooltip>
                    ) : (
                        <Text className="text-muted">N/A</Text>
                    )}
                </Descriptions.Item>

                <Descriptions.Item label="Loại xử lý">
                    <Text>{translateAttemptCategory(item.attemptCategory)}</Text>
                </Descriptions.Item>

                <Descriptions.Item label="Lần thực hiện">
                    {item.attemptNumber == null ? (
                        <Text className="text-muted">N/A</Text>
                    ) : (
                        <Text>{item.attemptNumber}</Text>
                    )}
                </Descriptions.Item>

                <Descriptions.Item label="Kết quả">
                    <Text>{translateAttemptStatus(status)}</Text>
                </Descriptions.Item>

                <Descriptions.Item label="Lý do thất bại">
                    {item.failReason ? (
                        <Text>{translateFailReason(item.failReason, item.attemptCategory)}</Text>
                    ) : (
                        <Text className="text-muted">N/A</Text>
                    )}
                </Descriptions.Item>

                <Descriptions.Item label="Ghi chú">
                    {item.note ? (
                        <div className="incident-report-detail-modal-desc-background">
                            <Text>{item.note}</Text>
                        </div>
                    ) : (
                        <Text className="text-muted">N/A</Text>
                    )}
                </Descriptions.Item>

                <Descriptions.Item label="Ảnh minh chứng">
                    {item.proofImageUrl ? (
                        <div>
                            <Image
                                src={item.proofImageUrl}
                                width={120}
                                height={120}
                                style={{objectFit: "cover", borderRadius: 6}}
                            />
                        </div>
                    ) : (
                        <Text className="text-muted">Không có ảnh</Text>
                    )}
                </Descriptions.Item>

                <Descriptions.Item label="Shipper">
                    {item.shipperName ? (
                        <div>
                            <Text strong>{item.shipperName}</Text>
                            <br/>
                            {item.shipperPhone && (
                                <Text className="text-muted">{item.shipperPhone}</Text>
                            )}
                        </div>
                    ) : (
                        <Text className="text-muted">N/A</Text>
                    )}
                </Descriptions.Item>

                <Descriptions.Item label="Thời gian thực hiện">
                    {item.attemptedAt ? (
                        <Text>{dayjs(item.attemptedAt).format("HH:mm:ss DD/MM/YYYY")}</Text>
                    ) : (
                        <Text className="text-muted">N/A</Text>
                    )}
                </Descriptions.Item>
            </Descriptions>
        </Modal>
    );
};

export default AttemptHistoryDetailModal;

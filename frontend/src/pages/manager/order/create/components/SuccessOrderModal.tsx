import React from "react";
import { Modal, Button } from "antd";
import { useNavigate } from "react-router-dom";
import { CheckCircleFilled, FileTextOutlined, OrderedListOutlined, EyeOutlined } from "@ant-design/icons";

interface Props {
    open: boolean;
    trackingNumber: string | null;
    onClose: () => void;
    onCreateNew: () => void;
}

const SuccessOrderModal: React.FC<Props> = ({ open, trackingNumber, onClose, onCreateNew }) => {
    const navigate = useNavigate();

    const getTitle = () => {
        return "Tạo đơn hàng thành công!";
    };

    const getDescription = () => {
        return "Đơn hàng đã được tạo thành công và đã nhập bưu cục gửi.";
    };

    const handleCreateNew = () => {
        if (onCreateNew) {
            onCreateNew();
        }
        onClose();
        navigate("/orders/create", { replace: true });
    };

    const handleViewList = () => {
        navigate("/orders/list", { replace: true });
        onClose();
    };

    const handleViewDetail = () => {
        navigate(`/orders/tracking/${trackingNumber}`, { replace: true });
        onClose();
    };

    return (
        <Modal
            open={open}
            closable={false}
            maskClosable={false}
            footer={null}
            onCancel={onClose}
            centered
            width={500}
            className="create-order-success-order-modal"
        >
            <div className="create-order-success-order-modal modal-content">
                {/* Success Icon */}
                <div className="create-order-success-order-modal success-icon">
                    <CheckCircleFilled />
                </div>

                {/* Title */}
                <h2 className="create-order-success-order-modal modal-title">
                    {getTitle()}
                </h2>

                {/* Description */}
                <p className="create-order-success-order-modal modal-description">
                    {getDescription()}
                </p>


                {/* Tracking Number */}
                {trackingNumber !== null &&
                    <div className="create-order-success-order-modal tracking-section">
                        <p className="create-order-success-order-modal tracking-label">Mã đơn hàng:</p>
                        <div className="create-order-success-order-modal tracking-number">
                            {trackingNumber}
                        </div>
                        <p className="create-order-success-order-modal tracking-note">
                            Vui lòng ghi nhớ mã này để tra cứu trạng thái đơn hàng
                        </p>
                    </div>
                }

                {/* Divider */}
                <div className="create-order-success-order-modal modal-divider" />

                {/* Action Buttons */}
                <div className="create-order-success-order-modal action-buttons">
                    <p className="create-order-success-order-modal action-title">
                        Chọn một hành động để tiếp tục:
                    </p>

                    <div className="create-order-success-order-modal button-group">
                        <Button
                            type="primary"
                            icon={<FileTextOutlined />}
                            onClick={handleCreateNew}
                            className="create-order-success-order-modal btn-create-new"
                        >
                            Tạo đơn mới
                        </Button>

                        <Button
                            icon={<OrderedListOutlined />}
                            onClick={handleViewList}
                            className="create-order-success-order-modal btn-view-list"
                        >
                            Danh sách đơn
                        </Button>

                        <Button
                            icon={<EyeOutlined />}
                            onClick={handleViewDetail}
                            className="create-order-success-order-modal btn-view-detail"
                        >
                            Xem chi tiết
                        </Button>
                    </div>
                </div>
            </div>
        </Modal>
    );
};

export default SuccessOrderModal;
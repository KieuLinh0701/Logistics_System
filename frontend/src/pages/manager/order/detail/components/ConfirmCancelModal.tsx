import { Modal } from "antd";

interface Props {
    open: boolean;
    onOk: () => void;
    onCancel: () => void;
    loading: boolean;
}

const ConfirmCancelModal: React.FC<Props> = ({ open, onOk, onCancel, loading }) => {
    return (
        <Modal
            open={open}
            title={<span className="modal-title">Xác nhận hủy đơn hàng</span>}
            okText="Hủy"
            cancelText="Không"
            centered
            closeIcon
            okButtonProps={{
                className: "modal-ok-button",
                loading
            }}
            cancelButtonProps={{ className: "modal-cancel-button" }}
            onOk={onOk}
            onCancel={onCancel}
        >
            <span className="order-detail-confirm-modal-content">
                Bạn có chắc chắn muốn hủy đơn hàng này không?
            </span>
        </Modal>
    );
};

export default ConfirmCancelModal;
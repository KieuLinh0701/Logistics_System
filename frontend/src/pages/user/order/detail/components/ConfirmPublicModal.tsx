import { Modal } from "antd";

interface Props {
    open: boolean;
    onOk: () => void;
    onCancel: () => void;
    loading: boolean;
}

const ConfirmPublicModal: React.FC<Props> = ({ open, onOk, onCancel, loading }) => {
    return (
        <Modal
            open={open}
            title={<span className="modal-title">Xác nhận chuyển đơn hàng sang xử lý</span>}
            okText="Có"
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
                Bạn có chắc chắn muốn chuyển đơn hàng này sang trạng thái xử lý không?
            </span>
        </Modal>
    );
};

export default ConfirmPublicModal;
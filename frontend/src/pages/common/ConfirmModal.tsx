import { Modal } from "antd";

interface Props {
    open: boolean;
    title: string;
    message: string;
    onOk: () => void;
    onCancel: () => void;
    loading: boolean;
}

const ConfirmModal: React.FC<Props> = ({ open, title, message, onOk, onCancel, loading }) => {
    return (
        <Modal
            open={open}
            title={<span className="modal-title">{title}</span>}
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
                {message}
            </span>
        </Modal>
    );
};

export default ConfirmModal;
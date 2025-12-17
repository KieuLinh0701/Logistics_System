import React, { useEffect, useState } from "react";
import { Modal, InputNumber, Typography, Alert } from "antd";

const { Text } = Typography;

interface Props {
    visible: boolean;
    settlementCode?: string;
    remainAmount: number;
    loading?: boolean;
    onCancel: () => void;
    onSubmit: (amount: number) => void;
}

const PaymentModal: React.FC<Props> = ({
    visible,
    settlementCode,
    remainAmount,
    loading = false,
    onCancel,
    onSubmit,
}) => {
    const [amount, setAmount] = useState<number>(0);

    useEffect(() => {
        if (visible) {
            setAmount(remainAmount);
        }
    }, [visible, remainAmount]);

    return (
        <Modal
            title={<span className='modal-title'>Thanh toán đối soát #{settlementCode}</span>}
            open={visible}
            onCancel={onCancel}
            onOk={() => onSubmit(amount)}
            okText="Thanh toán"
            cancelText="Hủy"
            okButtonProps={{
                disabled: amount <= 0 || amount > remainAmount,
                className: "modal-ok-button",
                loading: loading
            }}
            cancelButtonProps={{
                className: "modal-cancel-button"
            }}
            className="modal-hide-scrollbar"
        >

            <div className="manager-batchs-payment-modal-amount-contain">
                <Text>Số tiền còn phải thanh toán:</Text>
                <div>
                    <Text strong className="custom-table-content-strong manager-batchs-payment-modal-amount-text">
                        {remainAmount.toLocaleString()} VND
                    </Text>
                </div>
            </div>

            <div>
                <Text>Nhập số tiền thanh toán</Text>
                <InputNumber
                    className="modal-custom-input-number"
                    min={1}
                    value={amount}
                    formatter={(value) =>
                        `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")
                    }
                    parser={(value) => Number(value?.replace(/,/g, ""))}
                    onChange={(val) => setAmount(val || 0)}
                />
            </div>

            {amount > remainAmount && (
                <Text type="danger">
                    Số tiền thanh toán không được vượt quá số tiền còn lại
                </Text>
            )}
        </Modal>
    );
};

export default PaymentModal;
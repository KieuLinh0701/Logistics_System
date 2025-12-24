import React, { useEffect, useState } from "react";
import { Modal, InputNumber, Typography } from "antd";

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

    const showExceedAlert = amount > remainAmount;
    const showMinAlert = amount > 0 && amount < 10000;
    const nextRemain = remainAmount - amount;
    const showNextMinAlert = nextRemain > 0 && nextRemain < 10000;

    return (
        <Modal
            title={<span className='modal-title'>Thanh toán đối soát #{settlementCode}</span>}
            open={visible}
            onCancel={onCancel}
            onOk={() => onSubmit(amount)}
            okText="Thanh toán"
            cancelText="Hủy"
            okButtonProps={{
                disabled: amount < 10000 || amount > remainAmount,
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

            <div style={{ marginTop: 10 }}>
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

            {showExceedAlert && (
                <Text type="danger">
                    Số tiền thanh toán không được vượt quá số tiền còn lại
                </Text>
            )}

            {showMinAlert && (
                <Text type="danger">
                    Số tiền thanh toán tối thiểu là 10,000 VND
                </Text>
            )}

            {showNextMinAlert && (
                <Text type="warning">
                    Lưu ý: Sau khi thanh toán {amount.toLocaleString()} VND, số dư còn lại {nextRemain.toLocaleString()} VND dưới mức tối thiểu 10,000 VND.
                    Để tránh nợ xấu, vui lòng gộp với các phiên đối soát khác cho các lần thanh toán tiếp theo.
                </Text>
            )}

        </Modal>
    );
};

export default PaymentModal;
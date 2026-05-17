import React from "react";
import { Button, Tooltip } from "antd";
import {
    CreditCardOutlined,
    WalletOutlined,
    ClockCircleOutlined,
    PayCircleOutlined,
    CalendarOutlined,
    QuestionCircleOutlined,
} from "@ant-design/icons";
import Title from "antd/es/typography/Title";

interface Props {
    received: number;
    pending: number;
    debt: number;
    nextSettlementDate?: string;
    onPayment: () => void;
}

const SettlementSummary: React.FC<Props> = ({ received, pending, debt, nextSettlementDate, onPayment }) => {
    const stats = [
        {
            key: "received",
            icon: <WalletOutlined />,
            color: "#1C3D90",
            amount: received.toLocaleString(),
            suffix: "₫",
            label: "ĐÃ NHẬN",
            tooltip: "Tổng tiền hệ thống đã chuyển cho bạn",
        },
        {
            key: "pending",
            icon: <ClockCircleOutlined />,
            color: "#fa8c16",
            amount: pending.toLocaleString(),
            suffix: "₫",
            label: "SẮP NHẬN",
            tooltip: "Tiền đang trong quá trình xử lý",
        },
        {
            key: "debt",
            icon: <PayCircleOutlined />,
            color: "#f5222d",
            amount: debt.toLocaleString(),
            suffix: "₫",
            label: "CÒN NỢ",
            tooltip: "Số tiền bạn cần thanh toán cho hệ thống",
        },
        ...(nextSettlementDate
            ? [{
                key: "nextDate",
                icon: <CalendarOutlined />,
                color: "#52c41a",
                amount: nextSettlementDate,
                suffix: "",
                label: "NGÀY ĐỐI SOÁT TIẾP THEO",
                tooltip: "Ngày hệ thống sẽ tự động đối soát tiếp theo",
            }]
            : []),
    ];

    return (
        <div className="settlement-summary-wrap">
            <div className="list-page-header">
                <Title level={3} className="list-page-title-main">Thống kê đối soát</Title>
                {debt > 0 && (
                    <Button
                        className="primary-button"
                        icon={<CreditCardOutlined />}
                        onClick={onPayment}
                    >
                        Thanh toán {debt.toLocaleString()} đ
                    </Button>
                )}
            </div>

            <div className="settlement-summary-cards">
                {stats.map((stat, idx) => (
                    <React.Fragment key={stat.key}>
                        {idx > 0 && <div className="ss-divider" />}
                        <Tooltip title={stat.tooltip} placement="top" color="#1C3D90">
                            <div className="settlement-summary-card">
                                <div className="ss-icon" style={{ color: stat.color }}>
                                    {stat.icon}
                                </div>
                                <div>
                                    <p className="ss-amount" style={{ color: stat.color }}>
                                        {stat.amount}
                                        {stat.suffix && (
                                            <span className="ss-suffix">{stat.suffix}</span>
                                        )}
                                    </p>
                                    <p className="ss-label">
                                        {stat.label}
                                        <QuestionCircleOutlined className="ss-help" />
                                    </p>
                                </div>
                            </div>
                        </Tooltip>
                    </React.Fragment>
                ))}
            </div>
        </div>
    );
};

export default SettlementSummary;
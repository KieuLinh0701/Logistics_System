import React from "react";
import {Button, Tooltip} from "antd";
import {
    CreditCardOutlined,
    WalletOutlined,
    ClockCircleOutlined,
    PayCircleOutlined,
    CalendarOutlined,
    QuestionCircleOutlined,
} from "@ant-design/icons";
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";

interface Props {
    received: number;
    pending: number;
    debt: number;
    nextSettlementDate?: string;
    onPayment: () => void;
}

const showStats = hasPermissionGroup(['GROUP_USER', 'USER_COD_STATISTICS']);
const showPayment = hasPermissionGroup(['GROUP_USER', 'USER_COD_PAYMENT']);

const getTitle = () => {
    if (!showStats) return "Thanh toán công nợ";
    return "Thống kê đối soát";
};

const SettlementSummary: React.FC<Props> = ({received, pending, debt, nextSettlementDate, onPayment}) => {
    const stats = [
        {
            key: "received",
            icon: <WalletOutlined/>,
            color: "#1C3D90",
            value: received.toLocaleString(),
            suffix: "₫",
            label: "Đã nhận",
            tooltip: "Tổng tiền hệ thống đã chuyển cho bạn",
        },
        {
            key: "pending",
            icon: <ClockCircleOutlined/>,
            color: "#fa8c16",
            value: pending.toLocaleString(),
            suffix: "₫",
            label: "Sắp nhận",
            tooltip: "Tiền đang trong quá trình xử lý",
        },
        {
            key: "debt",
            icon: <PayCircleOutlined/>,
            color: "#f5222d",
            value: debt.toLocaleString(),
            suffix: "₫",
            label: "Còn nợ",
            tooltip: "Số tiền bạn cần thanh toán cho hệ thống",
        },
        ...(nextSettlementDate
            ? [{
                key: "nextDate",
                icon: <CalendarOutlined/>,
                color: "#52c41a",
                value: nextSettlementDate,
                suffix: "",
                label: "Ngày đối soát tiếp theo",
                tooltip: "Ngày hệ thống sẽ tự động đối soát tiếp theo",
            }]
            : []),
    ];

    return (
        <div className="settlement-summary-overview">
            <div className="settlement-summary-header">
                <h3 className="settlement-summary-title">{getTitle()}</h3>
                {showPayment && (
                    <Button
                        className="primary-button"
                        icon={<CreditCardOutlined/>}
                        onClick={onPayment}
                        disabled={debt <= 0}
                    >
                        Thanh toán {debt.toLocaleString()} đ
                    </Button>
                )}
            </div>

            {showStats && (
                <div className={`settlement-summary-stats${nextSettlementDate ? " has-date" : ""}`}>
                    {stats.map((stat) => (
                        <Tooltip
                            key={stat.key}
                            title={stat.tooltip}
                            placement="top"
                            color="#1C3D90"
                        >
                            <div className="settlement-summary-stat">
                                <div className="settlement-summary-stat-icon">
                                    {stat.icon}
                                </div>
                                <div className="settlement-summary-stat-content">
                                    <div className="settlement-summary-stat-value">
                                        {stat.value}
                                        {stat.suffix && (
                                            <span className="settlement-summary-stat-suffix">{stat.suffix}</span>
                                        )}
                                    </div>
                                    <div className="settlement-summary-stat-label">
                                        {stat.label}
                                        <QuestionCircleOutlined className="settlement-summary-stat-tooltip-icon"/>
                                    </div>
                                </div>
                            </div>
                        </Tooltip>
                    ))}
                </div>
            )}
        </div>
    );
}

export default SettlementSummary;
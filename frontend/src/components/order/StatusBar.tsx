import React from "react";
import {ORDER_STATUS, translateOrderStatus} from "../../utils/orderUtils.ts";
import type {StatusCount} from "../../types/order.ts";
import {getUserRole} from "../../utils/authUtils.ts";

interface Props {
    statusCounts: StatusCount[];
    activeStatus: string;
    onStatusChange: (status: string) => void;
}

const isManager = getUserRole() === "manager";

const STATUS_ORDER = ["ALL", ...ORDER_STATUS.filter(s => {
    if (s === "DRAFT") return !isManager;
    return true;
})];

const StatusBar: React.FC<Props> = ({ statusCounts, activeStatus, onStatusChange }) => {
    const countMap = Object.fromEntries(statusCounts.map(({ status, count }) => [status, count]));

    return (
        <div className="status-bar-wrap">
            {STATUS_ORDER.map(key => (
                <div
                    key={key}
                    className={`status-bar-item ${activeStatus === key ? "active" : ""}`}
                    onClick={() => onStatusChange(key)}
                >
          <span className="status-bar-label">
            {key === "ALL" ? "Tất cả" : translateOrderStatus(key)}
          </span>
                    <span className="status-bar-badge">{countMap[key] ?? 0}</span>
                </div>
            ))}
        </div>
    );
};

export default StatusBar;
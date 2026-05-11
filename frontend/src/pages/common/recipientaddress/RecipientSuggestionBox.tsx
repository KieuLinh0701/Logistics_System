import React from "react";
import {Spin, Tag, Button} from "antd";
import {
    ClockCircleOutlined,
    BookOutlined,
} from "@ant-design/icons";
import type {RecipientSuggestionResponse} from "../../../types/recipientAddress.ts";

interface Props {
    loading: boolean;
    suggestion: RecipientSuggestionResponse | null;
    phoneReady: boolean;
    onApply?: () => void;

    addressApplied?: boolean;
}

const RecipientSuggestionBox: React.FC<Props> = ({
                                                     loading,
                                                     suggestion,
                                                     phoneReady,
                                                     onApply,
                                                     addressApplied
                                                 }) => {
    if (!phoneReady) return null;

    if (loading) {
        return (
            <div style={styles.wrapper}>
                <Spin size="small"/>
                <span style={styles.muted}>Đang tìm thông tin người nhận...</span>
            </div>
        );
    }

    if (!suggestion || suggestion.type === "NONE") {
        return (
            <div style={styles.wrapper}>
                Không tìm thấy thông tin. Vui lòng nhập địa chỉ bên dưới.
            </div>
        );
    }

    const {address, recipientStats, type} = suggestion;

    return (
        <div style={styles.box}>
            {!addressApplied && (
                <div style={styles.header}>
                    {type === "SAVED"
                        ? (
                            <Tag icon={<BookOutlined/>} color="default">
                                Địa chỉ đã lưu
                            </Tag>
                        )
                        : (
                            <Tag icon={<ClockCircleOutlined/>} color="default">
                                Từ đơn gần nhất
                            </Tag>
                        )
                    }

                    {onApply && (
                        <Button
                            className="modal-cancel-button"
                            size="small"
                            onClick={onApply}
                        >
                            Dùng địa chỉ này
                        </Button>
                    )}
                </div>
            )}

            {!addressApplied && (
                <>
                    <div style={styles.name}>{address.name}</div>

                    <div style={styles.muted}>
                        {address.fullAddress ||
                            `${address.detail}, ${address.wardName}, ${address.cityName}`}
                    </div>
                </>
            )}

            {recipientStats.latestOrderDate && (
                <div style={{...styles.muted, fontSize: 11, marginTop: 3}}>
                    Đơn gần nhất: {new Date(recipientStats.latestOrderDate).toLocaleDateString("vi-VN")}
                </div>
            )}

            <div
                style={{
                    ...styles.statsRow,
                    borderTop: addressApplied ? "none" : "1px solid #f0f0f0",
                    paddingTop: addressApplied ? 0 : 8,
                    marginTop: addressApplied ? 4 : 8,
                }}
            >
                <span style={styles.totalOrderText}>
                    Tổng đơn: <strong>{recipientStats.totalSystemOrders}</strong>
                </span>

                <span style={styles.successText}>
                    Tỉ lệ thành công: <strong>{recipientStats.successRate}%</strong>
                </span>

                <span style={styles.returnedText}>
                    Tỉ lệ hoàn hàng: <strong>{recipientStats.returnedRate}%</strong>
                </span>
            </div>
        </div>
    );
};

const styles: Record<string, React.CSSProperties> = {
    wrapper: {
        display: "flex",
        alignItems: "center",
        gap: 8,
        borderRadius: 6,
        fontSize: 13,
        color: "#8c8c8c",
    },
    box: {
        padding: "10px 14px",
        borderRadius: 8,
        border: "1px solid #b7d3ff",
    },
    header: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        marginBottom: 6,
    },
    name: {
        fontWeight: 600,
        fontSize: 14,
        color: "#262626",
    },
    muted: {
        fontSize: 12,
        color: "#8c8c8c",
        marginTop: 2,
    },
    statsRow: {
        display: "flex",
        alignItems: "center",
        gap: 10,
        marginTop: 8,
        paddingTop: 8,
        borderTop: "1px solid #f0f0f0",
        flexWrap: "wrap",
    },
    statText: {
        fontSize: 12,
        color: "#595959",
    },
    totalOrderText: {
        fontSize: 12,
        color: "#1677ff",
        background: "#e6f4ff",
        padding: "2px 8px",
        borderRadius: 999,
    },

    successText: {
        fontSize: 12,
        color: "#389e0d",
        background: "#f6ffed",
        padding: "2px 8px",
        borderRadius: 999,
    },

    returnedText: {
        fontSize: 12,
        color: "#cf1322",
        background: "#fff1f0",
        padding: "2px 8px",
        borderRadius: 999,
    },
};

export default RecipientSuggestionBox;
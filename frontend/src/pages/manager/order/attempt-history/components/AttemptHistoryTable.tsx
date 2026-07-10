import React from "react";
import {Image, Table, Tooltip} from "antd";
import type {ColumnsType} from "antd/es/table";
import dayjs from "dayjs";
import type {AttemptHistoryItem} from "../../../../../types/attemptHistory";
import {
    translateAttemptCategory,
    translateAttemptStatus,
    translateFailReason,
} from "../../../../../utils/attemptHistoryUtils";

interface Props {
    items: AttemptHistoryItem[];
    currentPage: number;
    pageSize: number;
    total: number;
    onPageChange: (page: number, pageSize?: number) => void;
    onView: (item: AttemptHistoryItem) => void;
    loading: boolean;
}

const AttemptHistoryTable: React.FC<Props> = ({
                                                   items,
                                                   currentPage,
                                                   pageSize,
                                                   total,
                                                   onPageChange,
                                                   onView,
                                                   loading,
                                               }) => {
    const tableData = items.map((i) => ({
        ...i,
        key: `${i.attemptCategory}-${i.id}`,
    }));

    const columns: ColumnsType<AttemptHistoryItem> = [
        {
            title: "Mã đơn hàng",
            dataIndex: "trackingNumber",
            key: "trackingNumber",
            align: "left",
            width: 170,
            render: (_, record) => {
                if (!record.trackingNumber)
                    return <span className="text-muted">N/A</span>;
                return (
                    <Tooltip title="Click để xem chi tiết attempt">
                        <span
                            className="navigate-link"
                            onClick={() => onView(record)}
                        >
                            {record.trackingNumber}
                        </span>
                    </Tooltip>
                );
            },
        },
        {
            title: "Loại xử lý",
            dataIndex: "attemptCategory",
            key: "attemptCategory",
            align: "left",
            width: 130,
            render: (value: string) => translateAttemptCategory(value),
        },
        {
            title: "Lần",
            dataIndex: "attemptNumber",
            key: "attemptNumber",
            align: "center",
            width: 70,
            render: (n: number | null) => (n == null ? <span className="text-muted">N/A</span> : n),
        },
        {
            title: "Kết quả",
            dataIndex: "status",
            key: "status",
            align: "left",
            width: 120,
            render: (_, record) => {
                const status = record.pickupStatus || record.deliveryStatus;
                return translateAttemptStatus(status);
            },
        },
        {
            title: "Lý do",
            dataIndex: "failReason",
            key: "failReason",
            align: "left",
            render: (_, record) =>
                record.failReason ? (
                    <span>{translateFailReason(record.failReason, record.attemptCategory)}</span>
                ) : (
                    <span className="text-muted">N/A</span>
                ),
        },
        {
            title: "Shipper",
            key: "shipper",
            align: "left",
            width: 200,
            render: (_, record) =>
                record.shipperName ? (
                    <div>
                        <div className="custom-table-content-strong">{record.shipperName}</div>
                        {record.shipperPhone && (
                            <div className="text-muted" style={{fontSize: 12}}>
                                {record.shipperPhone}
                            </div>
                        )}
                    </div>
                ) : (
                    <span className="text-muted">N/A</span>
                ),
        },
        {
            title: "Ảnh",
            dataIndex: "proofImageUrl",
            key: "proofImageUrl",
            align: "center",
            width: 90,
            render: (url: string | null) =>
                url ? (
                    <Image
                        src={url}
                        width={48}
                        height={48}
                        style={{objectFit: "cover", borderRadius: 4}}
                        preview={{mask: "Xem"}}
                    />
                ) : (
                    <span className="text-muted">N/A</span>
                ),
        },
        {
            title: "Thời gian",
            dataIndex: "attemptedAt",
            key: "attemptedAt",
            align: "left",
            width: 170,
            render: (val: string | null) =>
                val ? dayjs(val).format("HH:mm:ss DD/MM/YYYY") : <span className="text-muted">N/A</span>,
        },
    ];

    return (
        <div className="table-container">
            <Table
                columns={columns}
                dataSource={tableData}
                rowKey="key"
                scroll={{x: "max-content"}}
                className="list-page-table"
                loading={loading}
                pagination={{
                    current: currentPage,
                    pageSize,
                    total,
                    onChange: onPageChange,
                }}
                locale={{emptyText: "Không có dữ liệu"}}
            />
        </div>
    );
};

export default AttemptHistoryTable;

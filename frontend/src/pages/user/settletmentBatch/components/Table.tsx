import React from "react";
import dayjs from 'dayjs';
import {Table, Button, Tooltip, message} from "antd";
import type {ColumnsType} from "antd/es/table";
import type {SettlementBatch} from "../../../../types/settlementBatch";
import {translateSettlementBatchStatus} from "../../../../utils/settlementBatchUtils";
import {hasPermissionGroup} from "../../../../utils/authUtils.ts";

interface Props {
    data: SettlementBatch[];
    onDetail: (id: number) => void;
    currentPage: number;
    pageSize: number;
    total: number;
    loading: boolean;
    onPageChange: (page: number, pageSize?: number) => void;
    onSelectionChange?: (settlements: SettlementBatch[]) => void;
    selectedIds?: number[];
    onSelectAllAction?: () => void;
}

const DataTable: React.FC<Props> = ({
                                        data,
                                        onDetail,
                                        currentPage,
                                        pageSize,
                                        total,
                                        onPageChange,
                                        loading,
                                    }) => {
    const tableData = data.map((o) => ({...o, key: String(o.id)}));
    const canViewAction = hasPermissionGroup(['GROUP_USER', 'USER_COD_DETAIL']);

    const handleViewDetail = (id: number) => {
        if (canViewAction) {
            onDetail(id)
        } else {
            message.error("Bạn không có quyền xem chi tiết đối soát này.");
        }
    };

    const columns: ColumnsType<SettlementBatch> = [
        {
            title: "Mã phiên",
            key: "code",
            align: "left",
            render: (_, record) => {
                return (
                    <Tooltip title="Click để xem chi tiết các đối soát trong phiên">
            <span
                className="navigate-link"
                onClick={() => handleViewDetail(record.id!)}>
              {record.code}
            </span>
                    </Tooltip>
                );
            }
        },
        {
            title: "Trạng thái",
            dataIndex: "status",
            key: "status",
            align: "center",
            render: (_, record) => (
                <>
                    <div>{translateSettlementBatchStatus(record.status)}</div>
                </>
            ),
        },
        {
            title: "Hình thức",
            key: "type",
            align: "center",
            render: (_, record) => {
                if (record.balanceAmount === 0) return "Hòa";
                if (record.balanceAmount > 0) return "Hệ thống trả shop";
                return "Shop trả hệ thống";
            },
        },
        {
            title: "Số tiền",
            key: "balanceAmount",
            align: "center",
            render: (_, record) => (
                <span
                    className={record.balanceAmount >= 0 ? "custom-table-content-strong" : "custom-table-content-error"}>
            {record.balanceAmount.toLocaleString()}
        </span>
            )
        },
        {
            title: 'Thời gian',
            key: 'time',
            align: 'left',
            render: (_, record) => {
                const createdAt = record.createdAt
                    ? dayjs(record.createdAt).format('HH:mm:ss DD/MM/YYYY')
                    : null;

                const updatedAt = record.updatedAt
                    ? dayjs(record.updatedAt).format('HH:mm:ss DD/MM/YYYY')
                    : null;

                return (
                    <div>
                        <div>
                            <span className="custom-table-content-strong">
                                 Đối soát:{" "}
                            </span>
                            {createdAt ? createdAt : <span className="text-muted">N/A</span>}
                        </div>

                        <div>
                            <span className="custom-table-content-strong">
                                Cập nhật:{" "}
                            </span>
                            {updatedAt ? updatedAt : <span className="text-muted">N/A</span>}
                        </div>
                    </div>
                );
            }
        },
        ...(canViewAction ? ([{
            key: "action",
            title: "Thao tác",
            align: "left",
            render: (_, record: SettlementBatch) => (
                <Button
                    className="action-button-link"
                    type="link"
                    onClick={() => onDetail(record.id)}
                >
                    Xem
                </Button>
            )
        }] as ColumnsType<SettlementBatch>) : []),
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
            />
        </div>
    );
};

export default DataTable;
import React from "react";
import dayjs from 'dayjs';
import { Table, Button, Dropdown, Tooltip } from "antd";
import { DownOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import { canProcessManagerPaymetSubmissionBatch, translatePaymentSubmissionBatchStatus } from "../../../../utils/paymentSubmissionBatchUtils";
import type { ManagerPaymentSubmissionBatch } from "../../../../types/paymentSubmissionBatch";

interface Props {
  batchs: ManagerPaymentSubmissionBatch[];
  onProcess: (submission: ManagerPaymentSubmissionBatch) => void;
  onDetail: (id: number) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, pageSize?: number) => void;
}

const SubmissionTable: React.FC<Props> = ({
  batchs,
  onProcess,
  onDetail,
  currentPage,
  pageSize,
  total,
  onPageChange,
  loading,
}) => {
  const tableData = batchs.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<ManagerPaymentSubmissionBatch> = [
    {
      title: "Mã phiên",
      key: "code",
      align: "left",
      render: (_, record) => {
        return (
          <Tooltip title="Click để xem chi tiết các đối soát trong phiên">
            <span
              className="navigate-link"
              onClick={() => onDetail(record.id)}
            >
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
      align: "left",
      render: (_, record) => (
        <>
          <div>{translatePaymentSubmissionBatchStatus(record.status)}</div>
        </>
      ),
    },
    {
      title: "Người nộp",
      key: "shipperInfo",
      align: "left",
      render: (_, record) => {
        if (!record.shipper) {
          return <span className="text-muted">Chưa nộp</span>;
        }

        return (
          <>
            <div className="custom-table-content-strong">
              {record.shipper.lastName} {record.shipper.firstName}
            </div>
            <span>{record.shipper.phoneNumber}</span>
          </>
        );
      },
    },
    {
      title: "Tổng hệ thống",
      dataIndex: "totalSystemAmount",
      key: "totalSystemAmount",
      align: "center",
      render: (value, record) => {
        const mismatch = record.totalSystemAmount !== record.totalActualAmount;
        return (
          <span className={mismatch ? "custom-table-content-error" : "custom-table-content-strong"}>
            {value.toLocaleString()}
          </span>
        );
      },
    },
    {
      title: "Tổng thực tế",
      dataIndex: "totalActualAmount",
      key: "totalActualAmount",
      align: "center",
      render: (value, record) => {
        const mismatch = record.totalSystemAmount !== record.totalActualAmount;
        return (
          <span className={mismatch ? "custom-table-content-error" : "custom-table-content-strong"}>
            {value.toLocaleString()}
          </span>
        );
      },
    },
    {
      title: "Số đơn hàng",
      key: "totalOrders",
      dataIndex: "totalOrders",
      align: "center",
    },
    {
      title: "Xác nhận",
      key: "checkedInfo",
      align: "left",
      render: (_, record) => {
        if (!record.checkedBy) {
          return <span className="text-muted">Chưa xác nhận</span>;
        }

        return (
          <>
            <div className="custom-table-content-strong">
              {record.checkedBy.lastName} {record.checkedBy.firstName}
            </div>
            <span>{record.checkedBy.phoneNumber}</span>
          </>
        );
      },
    },
    {
      title: "Ghi chú",
      dataIndex: "notes",
      key: "notes",
      align: "left",
      render: (value) => {
        if (value) {
          return <span className="custom-table-content-limit">{value}</span>;
        } else {
          return <span className="text-muted">N/A</span>;
        }
      },
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

        const checkedAt = record.checkedAt
          ? dayjs(record.checkedAt).format('HH:mm:ss DD/MM/YYYY')
          : null;

        return (
          <div>
            <div>
              <span className="custom-table-content-strong">
                Tạo mới:{" "}
              </span>
              {createdAt ? createdAt : <span className="text-muted">N/A</span>}
            </div>

            <div>
              <span className="custom-table-content-strong">
                Cập nhật:{" "}
              </span>
              {updatedAt ? updatedAt : <span className="text-muted">N/A</span>}
            </div>

            <div>
              <span className="custom-table-content-strong">
                Xác nhận:{" "}
              </span>
              {checkedAt ? checkedAt : <span className="text-muted">N/A</span>}
            </div>
          </div>
        );
      }
    },
    {
      key: "action",
      align: "left",
      render: (_, record: ManagerPaymentSubmissionBatch) => {
        const canProcess = canProcessManagerPaymetSubmissionBatch(record.status);
        const items = [
          {
            key: "detail",
            label: "Xem chi tiết",
            onClick: () => onDetail(record.id),
          },
          {
            key: "process",
            label: "Đối soát",
            disabled: !canProcess,
            onClick: () => onProcess(record),
          },
        ];

        return (
          <Dropdown menu={{ items }} trigger={["click"]}>
            <Button className="dropdown-trigger-button">
              Thao tác <DownOutlined />
            </Button>
          </Dropdown>
        );
      },
    },
  ];

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        scroll={{ x: "max-content" }}
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

export default SubmissionTable;
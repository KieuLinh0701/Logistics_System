import React from "react";
import dayjs from 'dayjs';
import { Table, Button, Space, Tooltip } from "antd";
import { CheckCircleOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import { useNavigate } from "react-router-dom";
import type { ManagerPaymentSubmission } from "../../../../types/paymentSubmission";
import { canProcessManagerPaymetSubmission, translatePaymentSubmissionStatus } from "../../../../utils/paymentSubmissionUtils";

interface Props {
  submissions: ManagerPaymentSubmission[];
  onProcess: (submission: ManagerPaymentSubmission) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, pageSize?: number) => void;
}

const SubmissionTable: React.FC<Props> = ({
  submissions,
  onProcess,
  currentPage,
  pageSize,
  total,
  onPageChange,
  loading,
}) => {
  const navigate = useNavigate();
  const tableData = submissions.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<ManagerPaymentSubmission> = [
    {
      title: "Mã phiên",
      key: "code",
      align: "left",
      render: (_, record) => (
        <div className="custom-table-content-strong">{record.code}</div>
      ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      align: "center",
      render: (_, record) => (
        <>
          <div>{translatePaymentSubmissionStatus(record.status)}</div>
        </>
      ),
    },
    {
      title: "Tiền hệ thống",
      dataIndex: "systemAmount",
      key: "systemAmount",
      align: "center",
      render: (value, record) => {
        const mismatch = record.systemAmount !== record.actualAmount;
        return (
          <span className={mismatch ? "custom-table-content-error" : "custom-table-content-strong"}>
            {value.toLocaleString()}
          </span>
        );
      },
    },
    {
      title: "Tiền thực tế",
      dataIndex: "actualAmount",
      key: "actualAmount",
      align: "center",
      render: (value, record) => {
        const mismatch = record.systemAmount !== record.actualAmount;
        return (
          <span className={mismatch ? "custom-table-content-error" : "custom-table-content-strong"}>
            {value.toLocaleString()}
          </span>
        );
      },
    },
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      align: "left",
      render: (_, record) => {
        if (!record.order.trackingNumber) {
          return (
            <Tooltip title="Chưa có mã đơn hàng">
              <span className="text-muted">
                Chưa có mã
              </span>
            </Tooltip>
          );
        }

        return (
          <Tooltip title="Click để xem chi tiết đơn hàng">
            <span
              className="navigate-link-default"
              onClick={() => navigate(`/orders/tracking/${record.order.trackingNumber}`)}
            >
              {record.order.trackingNumber}
            </span>
          </Tooltip>
        );
      },
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
            <div>
              {record.checkedBy.lastName} {record.checkedBy.firstName}
            </div>
            <span className="text-muted">{record.checkedBy.phoneNumber}</span>
          </>
        );
      },
    },
    {
      title: 'Thời gian',
      key: 'time',
      align: 'left',
      render: (_, record) => {
        const paidAt = record.paidAt
          ? dayjs(record.paidAt).format('HH:mm:ss DD/MM/YYYY')
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
              {paidAt ? paidAt : <span className="text-muted">N/A</span>}
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
      key: "action",
      align: "left",
      render: (_, record) => {
        const canProcess = canProcessManagerPaymetSubmission(record.status);
        
        return (
          <Space>
            <Button
              type="link"
              icon={<CheckCircleOutlined />}
              size="small"
              disabled={!canProcess}
              onClick={() => onProcess(record)}
              className="action-button-link"
            >
              Đối soát
            </Button>
          </Space>
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
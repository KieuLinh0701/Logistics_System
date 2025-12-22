import React from "react";
import dayjs from 'dayjs';
import { Table, Button, Dropdown, Tooltip } from "antd";
import { DownOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import type { SettlementBatch } from "../../../../types/settlementBatch";
import { canPayUserSettlementBatch, translateSettlementBatchStatus } from "../../../../utils/settlementBatchUtils";

interface Props {
  datas: SettlementBatch[];
  onProcess: (item: SettlementBatch) => void;
  onDetail: (id: number) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, pageSize?: number) => void;
}

const DataTable: React.FC<Props> = ({
  datas,
  onProcess,
  onDetail,
  currentPage,
  pageSize,
  total,
  onPageChange,
  loading,
}) => {
  const tableData = datas.map((o) => ({ ...o, key: String(o.id) }));

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
      title: "Tổng tiền",
      key: "total",
      align: "center",
      render: (_, record) => (
        <span>
          {Math.abs(record.balanceAmount).toLocaleString()}
        </span>
      ),
    },
    {
      title: "Còn nợ",
      dataIndex: "remainAmount",
      key: "remainAmount",
      align: "center",
      render: (value) => (
        <span className={value > 0 ? "custom-table-content-error" : "custom-table-content-strong"}>
          {value.toLocaleString()}
        </span>
      ),
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
    {
      key: "action",
      align: "left",
      render: (_, record: SettlementBatch) => {
        const canPay = canPayUserSettlementBatch(record);
        const items = [
          {
            key: "detail",
            label: "Xem chi tiết",
            onClick: () => onDetail(record.id),
          },
          {
            key: "payment",
            label: "Thanh toán",
            disabled: !canPay,
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

export default DataTable;
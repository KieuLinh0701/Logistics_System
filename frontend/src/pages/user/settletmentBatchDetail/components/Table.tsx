import React from "react";
import dayjs from 'dayjs';
import { Table, Tooltip } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { Order } from "../../../../types/order";
import { translateOrderPayerType, translateOrderStatus } from "../../../../utils/orderUtils";
import { InfoCircleOutlined } from "@ant-design/icons";

interface Props {
  datas: Order[];
  onDetail: (trackingNumber: string) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, pageSize?: number) => void;
}

const DataTable: React.FC<Props> = ({
  datas,
  onDetail,
  currentPage,
  pageSize,
  total,
  onPageChange,
  loading,
}) => {
  const tableData = datas.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<Order> = [
    {
      title: "Mã đơn hàng",
      key: "trackingNumber",
      align: "left",
      render: (_, record) => {
        return (
          <Tooltip title="Nhấn để xem chi tiết đơn hàng">
            <span
              className="navigate-link"
              onClick={() => onDetail(record.trackingNumber)}
            >
              {record.trackingNumber}
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
          <div>{translateOrderStatus(record.status)}</div>
        </>
      ),
    },
    {
      title: "Ngày giao / hoàn",
      dataIndex: "deliveriedAt",
      key: "deliveriedAt",
      align: "center",
      render: (value) =>
        value ? dayjs(value).format("DD/MM/YYYY") : <span className="text-muted">N/A</span>,
    },
    {
      title: "Người thanh toán",
      dataIndex: "payer",
      key: "payer",
      align: "center",
      render: (_, record) => (
        <>
          <div>{translateOrderPayerType(record.payer)}</div>
        </>
      ),
    },
    {
      title: "COD (chưa phí)",
      key: "cod",
      dataIndex: "cod",
      align: "center",
      render: (cod) => cod?.toLocaleString('vi-VN')
    },
    {
      title: "Phí dịch vụ",
      key: "totalFee",
      dataIndex: "totalFee",
      align: "center",
      render: (totalFee) => totalFee?.toLocaleString('vi-VN')
    },
    {
      title: (
        <span>
          Cần trả / nhận&nbsp;
          <Tooltip title="Màu đỏ: bạn còn nợ, màu xanh: bạn sẽ nhận">
            <InfoCircleOutlined />
          </Tooltip>
        </span>
      ),
      key: "codAmount",
      align: "center",
      render: (_, record) => {
        let codAmount = record.cod ?? 0;
        const paymentStatus = record.paymentStatus;
        const orderStatus = record.status;
        const payer = record.payer;
        const totalFee = record.totalFee ?? 0;
        let displayAmount = codAmount;

        if (orderStatus === "RETURNED" && paymentStatus === "UNPAID") {
          displayAmount = -totalFee;
        } else if (orderStatus === "RETURNED" && paymentStatus === "PAID" && payer === "CUSTOMER") {
          displayAmount = 0;
        } else if (orderStatus === "DELIVERED" && paymentStatus === "PAID" && payer === "CUSTOMER") {
          displayAmount = codAmount;
        } else if (orderStatus === "DELIVERED" && paymentStatus === "UNPAID" && payer === "SHOP") {
          displayAmount = codAmount - totalFee;
        } else { 
          displayAmount = 0;
        }

        return (
          <span className={displayAmount >= 0 ? 'custom-table-content-strong' : 'custom-table-content-error'}>
            {displayAmount.toLocaleString('vi-VN')}
          </span>
        );
      }
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
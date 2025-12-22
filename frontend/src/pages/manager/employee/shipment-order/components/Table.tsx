import React from "react";
import { Table, Button, Space, Tooltip } from "antd";
import { useNavigate } from "react-router-dom";
import type { Order } from "../../../../../types/order";
import type { ColumnsType } from "antd/es/table";
import { translateOrderPayerType } from "../../../../../utils/orderUtils";

interface Props {
  orders: Order[];
  role?: string;
  onDetail: (trackingNumber: string) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, pageSize?: number) => void;
}

const OrderTable: React.FC<Props> = ({
  orders,
  role,
  onDetail,
  currentPage,
  pageSize,
  total,
  onPageChange,
}) => {
  const navigate = useNavigate();

  const tableData = orders.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<any> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      align: "center",
      render: (text, record) => (
        <Tooltip title="Click để xem chi tiết đơn hàng">
          <span
            onClick={() => navigate(`/${role}/orders/detail/${record.trackingNumber}`)}
            style={{
              color: '#1890ff',
              cursor: 'pointer',
              userSelect: 'text',
              whiteSpace: 'nowrap',
            }}
          >
            {record.trackingNumber || 'N/A'}
          </span>
        </Tooltip>
      ),
    },
    { title: "Giá trị đơn (VNĐ)", dataIndex: "orderValue", key: "orderValue", align: "center" },
    { title: "Phí dịch vụ (VNĐ)", dataIndex: "totalFee", key: "totalFee", align: "center" },
    { title: "COD (VNĐ)", dataIndex: "cod", key: "cod", align: "center" },
    { title: "Khối lượng (Kg)", dataIndex: "weight", key: "weight", align: "center" },
    {
      title: "Người thanh toán",
      dataIndex: "payer",
      key: "payer",
      align: "center",
      render: (payer) => (
        translateOrderPayerType(payer)
      )
    },
    {
      title: "",
      key: "action",
      align: "center",
      width: 100,
      render: (_, record: Order) => {

        return (
          <Space>
            {/* Nút Xem */}
            <Button
              type="link"
              size="small"
              onClick={() => onDetail(record.trackingNumber)}
            >
              Chi tiết ĐH
            </Button>
          </Space>
        );
      }
    }
  ];

  return <Table
    columns={columns}
    dataSource={tableData}
    rowKey="key"
    scroll={{ x: "max-content" }}
    pagination={{
      current: currentPage,
      pageSize,
      total,
      onChange: onPageChange,
    }}
  />;
};

export default OrderTable;
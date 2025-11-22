import React, { useEffect, useState } from "react";
import { Table, Button, Space, Tooltip, Dropdown } from "antd";
import { EditOutlined, CloseCircleOutlined, DownOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import type { Order } from "../../../../../types/order";
import locationApi from "../../../../../api/locationApi";
import { translateOrderPayerType, translateOrderPaymentStatus, translateOrderStatus } from "../../../../../utils/orderUtils";

interface Props {
  orders: Order[];
  onCancel: (id: number) => void;
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, limit?: number) => void;
}

const OrderTable: React.FC<Props> = ({
  orders,
  onCancel,
  page,
  limit,
  total,
  loading,
  onPageChange }) => {
  const [locationMap, setLocationMap] = useState<Record<number, { city: string, ward: string }>>({});
  const navigate = useNavigate();

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, { city: string; ward: string }> = {};
      for (const order of orders) {
        const cityName = (await locationApi.getCityNameByCode(order.recipientAddress.cityCode)) || "";
        const wardName = (await locationApi.getWardNameByCode(order.recipientAddress.cityCode, order.recipientAddress.wardCode)) || "";
        map[order.id] = { city: cityName, ward: wardName };
      }
      setLocationMap(map);
    };
    fetchLocations();
  }, [orders]);

  const tableData = orders.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<any> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      align: "left",
      render: (_, record) => (
        <Tooltip title="Click để xem chi tiết đơn hàng">
          <span className="navigate-link">
            {record.trackingNumber}
          </span>
        </Tooltip>
      ),
    },
    {
      title: "Người nhận",
      key: "recipient",
      align: "left",
      render: (_, record) => {
        const location = locationMap[record.id];
        const cityName = location?.city || "";
        const wardName = location?.ward || "";
        const address = `${record.recipientAddress.detail || ""}, ${wardName}, ${cityName}`;

        return (
          <><span className="long-column">
            {record.recipientName}<br />
            {record.recipientPhone}<br />
            {address}
          </span>
          </>
        );
      }
    },
    {
      title: "Tổng tiền (VNĐ)",
      key: "totalMoney",
      render: (_, record) => (
        <>Đơn: {record.orderValue.toLocaleString('vi-VN')}<br />
          COD: {record.cod.toLocaleString('vi-VN')}<br />
          Phí DV: {record.totalFee.toLocaleString('vi-VN')}</>
      )
    },
    {
      title: "Trạng thái", key: "status", render: (_, record) =>
        <>Đơn: {translateOrderStatus(record.status)}<br />
          Thanh toán: {translateOrderPaymentStatus(record.paymentStatus)}</>
    },
    { title: "Khối lượng (Kg)", dataIndex: "weight", key: "weight", align: "left" },
    {
      title: "Người thanh toán",
      dataIndex: "payer",
      key: "payer",
      align: "left",
      render: (payer) => (translateOrderPayerType(payer))
    },
    {
      title: "Dịch vụ giao hàng",
      dataIndex: "serviceType",
      key: "serviceType",
      align: "left",
      render: (serviceType) => (
        serviceType.name
      )
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => {
        const canCancel = ["DRAFT", "PENDING", "CONFIRMED"].includes(record.status);
        const canEdit = ["DRAFT", "PENDING", "CONFIRMED"].includes(record.status);

        const items = [
          {
            key: "edit",
            icon: <EditOutlined />,
            label: "Sửa",
            disabled: !canEdit,
            onClick: () => canEdit && navigate(`/orders/edit/${record.trackingNumber}`),
          },
          {
            key: "cancel",
            icon: <CloseCircleOutlined />,
            label: "Hủy",
            disabled: !canCancel,
            onClick: () => canCancel && record.id && onCancel(record.id),
          },
        ];

        return (
          <Space>
            <Button
              className="action-button-link"
              type="link"
              onClick={() => navigate(`/orders/detail/${record.trackingNumber}`)}
            >
              Xem
            </Button>

            <Dropdown menu={{ items }} trigger={['click']}>
              <Button className="dropdown-trigger-button">
                Thêm <DownOutlined />
              </Button>
            </Dropdown>
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
        loading={loading}
        rowKey="key"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={{
          current: page,
          pageSize: limit,
          total,
          onChange: (page, pageSize) => onPageChange(page, pageSize)
        }}
      />
    </div>
  );
}

export default OrderTable;
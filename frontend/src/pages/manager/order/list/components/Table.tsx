import React, { useEffect, useState } from "react";
import { Table, Button, Space, Tooltip, Dropdown } from "antd";
import { EditOutlined, CloseCircleOutlined, DownOutlined, PrinterOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import type { Order } from "../../../../../types/order";
import locationApi from "../../../../../api/locationApi";
import { canCancelManagerOrder, canEditManagerOrder, canPrintManagerOrder, translateOrderCreatorType, translateOrderPayerType, translateOrderPaymentStatus, translateOrderPickupType, translateOrderStatus } from "../../../../../utils/orderUtils";

interface Props {
  orders: Order[];
  onCancel: (id: number) => void;
  onPrint: (id: number) => void;
  onEdit: (id: number, trackingNumber: string) => void;
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, limit?: number) => void;
  selectedOrderIds: number[];
  setSelectedOrderIds: React.Dispatch<React.SetStateAction<number[]>>;
}

const OrderTable: React.FC<Props> = ({
  orders,
  onCancel,
  onPrint,
  onEdit,
  page,
  limit,
  total,
  loading,
  onPageChange,
  selectedOrderIds,
  setSelectedOrderIds }) => {

  const navigate = useNavigate();
  const [locationMap, setLocationMap] = useState<Record<number, {
    senderCity: string; senderWard: string;
    recipientCity: string; recipientWard: string;
  }>>({});

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, {
        senderCity: string; senderWard: string;
        recipientCity: string; recipientWard: string;
      }> = {};

      for (const order of orders) {
        // Sender
        const senderCityName = (await locationApi.getCityNameByCode(order.senderCityCode)) || "Unknown";
        const senderWardName = (await locationApi.getWardNameByCode(order.senderCityCode, order.senderWardCode)) || "Unknown";

        // Recipient
        const recipientCityName = (await locationApi.getCityNameByCode(order.recipientAddress.cityCode)) || "Unknown";
        const recipientWardName = (await locationApi.getWardNameByCode(order.recipientAddress.cityCode, order.recipientAddress.wardCode)) || "Unknown";

        map[order.id!] = {
          senderCity: senderCityName,
          senderWard: senderWardName,
          recipientCity: recipientCityName,
          recipientWard: recipientWardName
        };
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
      render: (trackingNumber, _) => {
        if (!trackingNumber) {
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
              className="navigate-link"
              onClick={() => navigate(`/orders/tracking/${trackingNumber}`)}
            >
              {trackingNumber}
            </span>
          </Tooltip>
        );
      },
    },
    {
      title: "Người gửi",
      key: "sender",
      align: "left",
      render: (_, record) => {
        const location = locationMap[record.id];
        const senderAddress = `${record.senderDetail || ""}, ${location?.senderWard || ""}, ${location?.senderCity || ""}`;

        return (
          <span className="long-column">
            <span className="custom-table-content-strong">{record.senderName}</span><br />
            {record.senderPhone}<br />
            {senderAddress}
          </span>
        );
      }
    },
    {
      title: "Người nhận",
      key: "recipient",
      align: "left",
      render: (_, record) => {
        const location = locationMap[record.id];
        const recipientAddress = `${record.recipientAddress.detail || ""}, ${location?.recipientWard || ""}, ${location?.recipientCity || ""}`;

        return (
          <span className="long-column">
            <span className="custom-table-content-strong">{record.recipientAddress.name}</span><br />
            {record.recipientAddress.phoneNumber}<br />
            {recipientAddress}
          </span>
        );
      }
    },
    {
      title: "Tổng tiền (VNĐ)",
      key: "totalMoney",
      render: (_, record) => (
        <><span className="custom-table-content-strong">Đơn:</span> {record.orderValue.toLocaleString('vi-VN')}<br />
          <span className="custom-table-content-strong">COD:</span> {record.cod.toLocaleString('vi-VN')}<br />
          <span className="custom-table-content-strong">Phí DV:</span> {record.totalFee.toLocaleString('vi-VN')}</>
      )
    },
    {
      title: "Trạng thái", key: "status", render: (_, record) =>
        <><span className="custom-table-content-strong">Đơn:</span><br />
          {translateOrderStatus(record.status)}<br />
          <span className="custom-table-content-strong">Thanh toán:</span><br />
          {translateOrderPaymentStatus(record.paymentStatus)}</>
    },
    {
      title: "Thông tin giao hàng",
      key: "shippingInfo",
      align: "left",
      render: (_, record) => (
        <><span className="custom-table-content-strong">Hình thức lấy hàng:</span><br />
          {translateOrderPickupType(record.pickupType)}<br />
          <span className="custom-table-content-strong">Dịch vụ giao hàng:</span><br />
          {record.serviceTypeName}</>
      )
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
      title: "Người tạo đơn hàng",
      dataIndex: "createdByType",
      key: "createdByType",
      align: "left",
      render: (_, record) => (
        <div className="order-creator-cell">
          <div>{translateOrderCreatorType(record.createdByType)}</div>
          {(record.employeeCode && record.createdByType !== "USER") ? (
            <div className="custom-table-content-strong">({record.employeeCode})</div>
          ) :
            (<div className="custom-table-content-strong">({record.userCode})</div>
            )}
        </div>
      ),
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => {
        const canCancel = canCancelManagerOrder(record.status);
        const canEdit = canEditManagerOrder(record.status); // này chưa sửa bên utils
        const canPrint = canPrintManagerOrder(record.status);

        const items = [
          ...(canPrint ? [{
            key: "print",
            icon: <PrinterOutlined />,
            label: "In phiếu",
            onClick: () => onPrint(record.id),
          }] : []),

          ...(canEdit ? [{
            key: "edit",
            icon: <EditOutlined />,
            label: "Sửa",
            onClick: () => onEdit(record.id, record.trackingNumber),
          }] : []),

          ...(canCancel ? [{
            key: "cancel",
            icon: <CloseCircleOutlined />,
            label: "Hủy",
            onClick: () => onCancel(record.id),
          }] : []),
        ];

        return (
          <Space>
            <Button
              className="action-button-link"
              type="link"
              onClick={() =>
                record.trackingNumber
                  ? navigate(`/orders/tracking/${record.trackingNumber}`)
                  : navigate(`/orders/id/${record.id}`)
              }
            >
              Xem
            </Button>

            <Dropdown
              menu={{ items }}
              trigger={["click"]}
              disabled={items.length === 0}
            >
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
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={{
          current: page,
          pageSize: limit,
          total,
          onChange: (page, pageSize) => onPageChange(page, pageSize)
        }}
        rowSelection={{
          type: 'checkbox',
          selectedRowKeys: selectedOrderIds,
          onChange: (keys) => setSelectedOrderIds(keys as number[]),
          getCheckboxProps: (record) => ({
            disabled: !record.trackingNumber,
          }),
        }}
        rowClassName={(record) =>
          selectedOrderIds.includes(record.id) ? "selectd-checkbox-table-row-selected" : ""
        }
      />
    </div>
  );
}

export default OrderTable;
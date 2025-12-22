import React, { useEffect, useState } from "react";
import { Table, Button, Space, Tooltip, Dropdown } from "antd";
import { EditOutlined, CloseCircleOutlined, DownOutlined, PrinterOutlined, CheckCircleOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import type { Order } from "../../../../../types/order";
import locationApi from "../../../../../api/locationApi";
import dayjs from 'dayjs';
import { canAtOriginOfficeManagerOrder, canCancelManagerOrder, canEditManagerOrder, canPrintManagerOrder, translateOrderCreatorType, translateOrderPayerType, translateOrderPaymentStatus, translateOrderPickupType, translateOrderStatus } from "../../../../../utils/orderUtils";

interface Props {
  orders: Order[];
  onCancel: (id: number) => void;
  onPrint: (id: number) => void;
  onAtOriginOffice: (id: number) => void;
  onEdit: (id: number, trackingNumber: string) => void;
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onPageChange: (page: number, limit?: number) => void;
  selectedOrderIds: number[];
  setSelectedOrderIds: React.Dispatch<React.SetStateAction<number[]>>;
  onSelectAllFiltered: (select: boolean) => void;
}

const OrderTable: React.FC<Props> = ({
  orders,
  onCancel,
  onPrint,
  onEdit,
  onAtOriginOffice,
  page,
  limit,
  total,
  loading,
  onPageChange,
  selectedOrderIds,
  setSelectedOrderIds,
  onSelectAllFiltered
}) => {

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
      title: "Trạng thái",
      key: "status",
      align: "center",
      render: (_, record) => translateOrderStatus(record.status)
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
            <span className="custom-table-content-limit">{senderAddress}</span>
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
            <span className="custom-table-content-limit">{recipientAddress}</span>
          </span>
        );
      }
    },
    {
      title: "Tổng quan tiền",
      key: "totalMoney",
      align: "left",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Giá trị đơn:</span> {record.orderValue.toLocaleString('vi-VN')}<br />
          <span className="custom-table-content-strong">COD (chưa phí):</span> {record.cod.toLocaleString('vi-VN')}<br />
          <span className="custom-table-content-strong">Phí dịch vụ:</span> {record.totalFee.toLocaleString('vi-VN')}
        </>
      )
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
    {
      title: "Thanh toán", key: "status", render: (_, record) =>
        <><span className="custom-table-content-strong">Người thanh toán:</span><br />
          {translateOrderPayerType(record.payer)}<br />
          <span className="custom-table-content-strong">Trạng thái:</span><br />
          {translateOrderPaymentStatus(record.paymentStatus)}</>
    },
    {
      title: "Khối lượng (Kg)",
      dataIndex: "weight",
      key: "weight",
      align: "center",
      render: (weight: number) => (weight || 0).toFixed(2)
    },
    {
      title: "Người tạo đơn hàng",
      dataIndex: "createdByType",
      key: "createdByType",
      align: "center",
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
      title: "Thời gian",
      key: "shippingInfo",
      align: "left",
      render: (_, record) => {
        const times = [
          { label: "Tạo đơn", value: record.createdAt },
          { label: record.status === "RETURNED" ? "Hoàn hàng" : "Giao hàng", value: record.deliveriedAt },
          { label: "Thanh toán", value: record.paidAt }
        ];

        return (
          <>
            {times.map((t, idx) => {
              const formatted = t.value ? dayjs(t.value).format('HH:mm:ss DD/MM/YYYY') : null;
              return (
                <div key={idx}>
                  <span className="custom-table-content-strong">{t.label}:{" "}</span>
                  {formatted || <span className="text-muted">N/A</span>}
                </div>
              );
            })}
          </>
        );
      }
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => {
        const canCancel = canCancelManagerOrder(record.status, record.createdByType);
        const canEdit = canEditManagerOrder(record.status);
        const canPrint = canPrintManagerOrder(record.status);
        const canAtOriginOffice = canAtOriginOfficeManagerOrder(record.status) && record.pickupType === 'AT_OFFICE';

        const items = [
          ...(canPrint ? [{
            key: "print",
            icon: <PrinterOutlined />,
            label: "In phiếu",
            onClick: () => onPrint(record.id),
          }] : []),

          ...(canAtOriginOffice ? [{
            key: "atOrginOffice",
            icon: <CheckCircleOutlined />,
            label: "Đã đến bưu cục",
            onClick: () => onAtOriginOffice(record.id),
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
        dataSource={orders}
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
          preserveSelectedRowKeys: false,
          selectedRowKeys: selectedOrderIds,
          onChange: (keys) => setSelectedOrderIds(keys as number[]),
          onSelectAll: (selected) => {
            onSelectAllFiltered(selected);
          },
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
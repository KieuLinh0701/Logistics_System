import React, { useEffect, useState } from "react";
import { Table, Button, Space, Tooltip, Dropdown } from "antd";
import { EditOutlined, CloseCircleOutlined, DownOutlined, PrinterOutlined, DeleteOutlined, PlayCircleOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import type { Order } from "../../../../../types/order";
import locationApi from "../../../../../api/locationApi";
import dayjs from 'dayjs';
import { canCancelUserOrder, canDeleteUserOrder, canEditUserOrder, canPrintUserOrder, canPublicUserOrder, canReadyUserOrder, translateOrderCodStatus, translateOrderPayerType, translateOrderPaymentStatus, translateOrderPickupType, translateOrderStatus } from "../../../../../utils/orderUtils";

interface Props {
  orders: Order[];
  onCancel: (id: number) => void;
  onPublic: (id: number) => void;
  onDelete: (id: number) => void;
  onPrint: (id: number) => void;
  onReady: (id: number) => void;
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
  onPublic,
  onDelete,
  onPrint,
  onEdit,
  onReady,
  page,
  limit,
  total,
  loading,
  onPageChange,
  selectedOrderIds,
  setSelectedOrderIds }) => {
  const [locationMap, setLocationMap] = useState<Record<number, { city: string, ward: string }>>({});
  const navigate = useNavigate();

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, { city: string; ward: string }> = {};
      for (const order of orders) {
        const cityName = (await locationApi.getCityNameByCode(order.recipientAddress.cityCode)) || "Unknown";
        const wardName = (await locationApi.getWardNameByCode(order.recipientAddress.cityCode, order.recipientAddress.wardCode)) || "Unknown";
        map[order.id!] = { city: cityName, ward: wardName };
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
            <span className="text-muted">
              Chưa có mã
            </span>
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
            <span className="custom-table-content-strong">{record.recipientAddress.name}</span><br />
            {record.recipientAddress.phoneNumber}<br />
            <span className="custom-table-content-limit">{address}</span>
          </span>
          </>
        );
      }
    },
    {
      title: "Trạng thái",
      key: "status",
      align: "center",
      render: (_, record) => translateOrderStatus(record.status)
    },
    {
      title: "Khối lượng (Kg)",
      dataIndex: "weight",
      key: "weight",
      align: "center",
      render: (weight: number) => (weight || 0).toFixed(2)
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
      title: "Thanh toán", key: "status", render: (_, record) =>
        <><span className="custom-table-content-strong">Người thanh toán:</span><br />
          {translateOrderPayerType(record.payer)}<br />
          <span className="custom-table-content-strong">Trạng thái:</span><br />
          {translateOrderPaymentStatus(record.paymentStatus)}</>
    },
    {
      title: "Tổng quan tiền",
      key: "totalMoney",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Giá trị đơn:</span> {record.orderValue.toLocaleString('vi-VN')}<br />
          <span className="custom-table-content-strong">COD (chưa phí):</span> {record.cod.toLocaleString('vi-VN')}<br />
          <span className="custom-table-content-strong">Phí dịch vụ:</span> {record.totalFee.toLocaleString('vi-VN')}
        </>
      )
    },
    {
      title: "Người nhận trả",
      key: "codFromRecipient",
      align: "center",
      render: (_, record) => {

        if (record.payer === 'CUSTOMER') {
          const codCollected = (record.cod || 0) + (record.totalFee || 0);
          return codCollected.toLocaleString('vi-VN');
        } else {
          return (record.cod || 0).toLocaleString('vi-VN');
        }
      }
    },
    {
      title: "Người gửi trả",
      key: "senderPaid",
      align: "center",
      render: (_, record) => {

        if (record.payer === 'SHOP') {
          return (record.totalFee || 0).toLocaleString('vi-VN');
        }

        return 0;
      }
    },
    {
      title: "Còn nợ",
      key: "debt",
      align: "center",
      render: (_, record) => {
        if (record.payer !== 'SHOP') return 0;

        const cod = record.cod || 0;
        const fee = record.totalFee || 0;
        const diff = cod - fee;

        const debt = diff > 0 ? 0 : Math.abs(diff);

        return (
          <span className={debt > 0 ? "custom-table-content-error" : ""}>
            {debt.toLocaleString('vi-VN')}
          </span>
        );
      }
    },
    {
      title: "COD thu về",
      key: "codCollected",
      align: "center",
      render: (_, record) => {
        if (record.payer !== 'SHOP') {
          return (record.cod || 0).toLocaleString('vi-VN');
        }

        const cod = record.cod || 0;
        const fee = record.totalFee || 0;
        const codCollected = Math.max(0, cod - fee);

        return (
          <span className={codCollected > 0 ? "custom-table-content-strong" : ""}>
            {codCollected.toLocaleString('vi-VN')}
          </span>
        );
      }
    },
    {
      title: "Trạng thái COD",
      key: "codStatus",
      align: "center",
      render: (_, record) => translateOrderCodStatus(record.codStatus)
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => {
        const canCancel = canCancelUserOrder(record.status);
        const canEdit = canEditUserOrder(record.status);
        const canDelete = canDeleteUserOrder(record.status);
        const canPrint = canPrintUserOrder(record.status);
        const canPublic = canPublicUserOrder(record.status);
        const canReady = canReadyUserOrder(record.status) && record.pickupType === "PICKUP_BY_COURIER"

        const items = [
          ...(canPrint ? [{
            key: "print",
            icon: <PrinterOutlined />,
            label: "In phiếu",
            onClick: () => onPrint(record.id),
          }] : []),

          ...(canReady ? [{
            key: "ready",
            icon: <PlayCircleOutlined />,
            label: "Sẵn sàng để lấy",
            onClick: () => onReady(record.id),
          }] : []),

          ...(canPublic ? [{
            key: "public",
            icon: <PlayCircleOutlined />,
            label: "Chuyển xử lý",
            onClick: () => onPublic(record.id),
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

          ...(canDelete ? [{
            key: "delete",
            icon: <DeleteOutlined />,
            label: "Xóa",
            danger: true,
            onClick: () => onDelete(record.id),
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
            disabled: !canPrintUserOrder(record.status),
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
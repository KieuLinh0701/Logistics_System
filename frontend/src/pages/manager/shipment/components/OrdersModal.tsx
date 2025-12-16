import React, { useEffect, useState } from "react";
import { Modal, Input, Table, Tooltip } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useNavigate } from "react-router-dom";
import type { ManagerOrderShipment } from "../../../../types/shipment";
import { SearchOutlined } from "@ant-design/icons";
import {
  translateOrderStatus,
  translateOrderPaymentStatus,
  translateOrderPayerType,
} from "../../../../utils/orderUtils";
import locationApi from "../../../../api/locationApi";

interface Props {
  open: boolean;
  orders: ManagerOrderShipment[];
  page: number;
  limit: number;
  total: number;
  searchText: string;
  loading?: boolean;
  onClose: () => void;
  onSearch: (value: string) => void;
  onPageChange: (page: number, limit?: number) => void;
}

const OrdersModal: React.FC<Props> = ({
  open,
  orders,
  page,
  limit,
  total,
  searchText,
  loading = false,
  onClose,
  onSearch,
  onPageChange,
}) => {
  const navigate = useNavigate();
  const [addressMap, setAddressMap] = useState<Record<string, string>>({});

  const formatAddress = async (
    key: string,
    detail?: string,
    wardCode?: number,
    cityCode?: number
  ) => {
    try {
      const cityName = cityCode
        ? await locationApi.getCityNameByCode(cityCode)
        : "";
      const wardName =
        cityCode && wardCode
          ? await locationApi.getWardNameByCode(cityCode, wardCode)
          : "";

      setAddressMap((prev) => ({
        ...prev,
        [key]: [detail, wardName, cityName].filter(Boolean).join(", "),
      }));
    } catch (error) {
      setAddressMap((prev) => ({
        ...prev,
        [key]: detail || "",
      }));
    }
  };

  useEffect(() => {
    orders.forEach((o) => {
      if (o.recipient) {
        formatAddress(
          `recipient-${o.id}`,
          o.recipient.detail,
          o.recipient.wardCode,
          o.recipient.cityCode
        );
      }
      if (o.toOffice) {
        formatAddress(
          `toOffice-${o.id}`,
          o.toOffice.detail,
          o.toOffice.wardCode,
          o.toOffice.cityCode
        );
      }
    });
  }, [orders]);

  const columns: ColumnsType<ManagerOrderShipment> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      align: "left",
      render: (trackingNumber) =>
        trackingNumber ? (
          <Tooltip title="Click để xem chi tiết đơn hàng">
            <span
              className="navigate-link"
              onClick={() =>
                navigate(`/orders/tracking/${trackingNumber}`)
              }
            >
              {trackingNumber}
            </span>
          </Tooltip>
        ) : (
          <Tooltip title="Chưa có mã đơn hàng">
            <span className="text-muted">Chưa có mã</span>
          </Tooltip>
        ),
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      align: "left",
      render: (status) => translateOrderStatus(status),
    },
    {
      title: "Người thanh toán",
      dataIndex: "payer",
      key: "payer",
      align: "left",
      render: (payer) => translateOrderPayerType(payer),
    },
    {
      title: "Trạng thái thanh toán",
      dataIndex: "paymentStatus",
      key: "paymentStatus",
      align: "left",
      render: (paymentStatus) => translateOrderPaymentStatus(paymentStatus),
    },
    {
      title: "Phí thu hộ (VNĐ)",
      dataIndex: "cod",
      key: "cod",
      align: "left",
      render: (_, record) => {
        const cod = record.cod || 0;
        const totalFee = record.totalFee || 0; 
        const payer = record.payer;

        const fee =
          payer === "CUSTOMER" ? cod + totalFee : cod;

        return fee.toLocaleString("vi-VN");
      },
    },
    {
      title: "Trọng lượng (Kg)",
      dataIndex: "weight",
      key: "weight",
      align: "left",
      render: (weight) => weight?.toFixed(2) || "0.00",
    },
    {
      title: "Người nhận",
      dataIndex: "recipient",
      key: "recipient",
      align: "left",
      render: (recipient, record) => {
        const address = addressMap[`recipient-${record.id}`];
        return (
          <div>
            <span className="custom-table-content-strong">{recipient?.name}</span> - {recipient?.phone}<br />
            <span className="custom-table-content-limit">{address || "Chưa có địa chỉ"}</span>
          </div>
        );
      },
    },
    {
      title: "Bưu cục nhận",
      dataIndex: "toOffice",
      key: "toOffice",
      align: "left",
      render: (toOffice, record) => {
        if (!toOffice) return <span className="text-muted">N/A</span>;

        const address = addressMap[`toOffice-${record.id}`];

        return (
          <div>
            <span>{toOffice.name}</span> - <span>{toOffice.postalCode}</span><br />
            {toOffice.latitude && toOffice.longitude ? (
              <Tooltip title="Nhấn để mở Google Maps">
                <span
                  className="navigate-link"
                  onClick={() =>
                    window.open(
                      `https://www.google.com/maps?q=${toOffice.latitude},${toOffice.longitude}`,
                      "_blank",
                      "noopener,noreferrer"
                    )
                  }
                >
                  {address || "Chưa có địa chỉ"}
                </span>
              </Tooltip>
            ) : (
              <span>{address || "Chưa có địa chỉ"}</span>
            )}
          </div>
        );
      },
    }
  ];

  const tableData = orders.map((o, index) => ({
    ...o,
    key: String(index + 1 + (page - 1) * limit),
  }));

  return (
    <Modal
      title={<span className='modal-title'>Danh sách đơn hàng</span>}
      open={open}
      onCancel={onClose}
      footer={null}
      width="90%"
      className="modal-hide-scrollbar"
    >
      <Input
        placeholder="Tìm theo mã đơn hàng..."
        prefix={<SearchOutlined />}
        allowClear
        className="search-input"
        value={searchText}
        onChange={(e) => {
          const value = e.target.value;
          onSearch(value);
        }}
      />

      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={{
          current: page,
          pageSize: limit,
          total: total,
          onChange: onPageChange,
        }}
        loading={loading}
      />
    </Modal>
  );
};

export default OrdersModal;
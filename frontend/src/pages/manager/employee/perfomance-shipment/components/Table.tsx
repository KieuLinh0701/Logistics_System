import React from "react";
import { Table, Button, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from 'dayjs';
import 'dayjs/locale/vi';
import type { ManagerShipment } from "../../../../../types/shipment";

interface Props {
  shipments: ManagerShipment[];
  onDetail: (shipmentCode: string) => void;
  currentPage: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, pageSize?: number) => void;
}

const ShipmentTable: React.FC<Props> = ({
  shipments,
  onDetail,
  currentPage,
  pageSize,
  total,
  onPageChange,
}) => {

  const tableData = shipments.map((o) => ({ ...o, key: String(o.id) }));

  const columns: ColumnsType<ManagerShipment> = [
    {
      title: "Mã chuyến",
      dataIndex: "code",
      key: "code",
      align: "left",
      render: (code) => {

        return (
          <span
            className="custom-table-content-strong"
          >
            {code}
          </span>
        );
      },
    },
    {
      title: "Trạng thái",
      key: "status",
      align: "left",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Trạng thái:</span><br />
          {record.status || "N/A"}
        </>
      ),
    },
    {
      title: "Thông tin xe",
      key: "vehicle",
      align: "left",
      render: (_, record) => {
        const vehicle = record.vehicle;
        const plate = vehicle?.licensePlate || "N/A";
        const capacity = vehicle?.capacity
          ? Number(vehicle.capacity).toLocaleString("vi-VN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })
          : "N/A";

        return (
          <span className="long-column">
            <span className="custom-table-content-strong">Biển số:</span> {plate}<br />
            <span className="custom-table-content-strong">Tải trọng:</span> {capacity} kg
          </span>
        );
      },
    },
    {
      title: "Thời gian",
      key: "time",
      align: "left",
      render: (_, record) => {
        const start = record.startTime
          ? dayjs(record.startTime).format("DD/MM/YYYY HH:mm")
          : "N/A";

        const end = record.endTime
          ? dayjs(record.endTime).format("DD/MM/YYYY HH:mm")
          : "N/A";

        return (
          <>
            <span className="custom-table-content-strong">Bắt đầu:</span><br />
            {start}<br />
            <span className="custom-table-content-strong">Kết thúc:</span><br />
            {end}
          </>
        );
      },
    },
    {
      title: "Thông tin chuyến đi",
      key: "tripInfo",
      align: "left",
      render: (_, record) => {
        const totalOrders = record.orderCount?.toLocaleString("vi-VN") || "0";
        const weight = record.totalWeight
          ? Number(record.totalWeight).toFixed(2)
          : "0.00";

        return (
          <>
            <span className="custom-table-content-strong">Tổng đơn:</span> {totalOrders}<br />
            <span className="custom-table-content-strong">Tổng trọng lượng:</span> {weight} kg
          </>
        );
      },
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => (
        <Button
          className="action-button-link"
          type="link"
          onClick={() => onDetail(record.code)}
        >
          Xem đơn hàng
        </Button>
      ),
    },
  ];

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        pagination={{
          current: currentPage,
          pageSize,
          total,
          onChange: onPageChange,
        }}
        />
    </div>
  );
}

export default ShipmentTable;
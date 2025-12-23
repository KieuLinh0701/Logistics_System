import React from "react";
import { Table, Button, Tooltip } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from 'dayjs';
import 'dayjs/locale/vi';
import type { ManagerShipment } from "../../../../../types/shipment";
import { translateShipmentStatus, translateShipmentType } from "../../../../../utils/shipmentUtils";

interface Props {
  shipments: ManagerShipment[];
  onDetail: (id: number) => void;
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
      render: (_, record) => {
        return (
          <Tooltip title="Click để xem danh sách đơn hàng của chuyến hàng">
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
      title: 'Loại chuyến',
      dataIndex: 'type',
      key: 'type',
      align: 'center',
      render: (type) => translateShipmentType(type)
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      align: 'center',
      render: (status) => translateShipmentStatus(status)
    },
    {
      title: 'Phương tiện',
      dataIndex: 'vehicle',
      key: 'vehicle',
      align: 'center',
      render: (_, record) => {
        const vehicle = record.vehicle;

        if (!vehicle) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            {vehicle.licensePlate}<br />
            <span className='text-muted'>
              ({vehicle.capacity} Kg)</span>
          </div>
        );
      }
    },
    {
      title: 'Thời gian',
      key: 'time',
      align: 'left',
      render: (_, record) => {
        const startTime = record.startTime
          ? dayjs(record.startTime).format('HH:mm:ss DD/MM/YYYY')
          : null;

        const endTime = record.endTime
          ? dayjs(record.endTime).format('HH:mm:ss DD/MM/YYYY')
          : null;

        if (!startTime && !endTime) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            {startTime && (
              <div>
                {startTime}
              </div>
            )}
            {endTime && (
              <div>
                - {endTime}
              </div>
            )}
          </div>
        );
      }
    },
    {
      title: "Tổng đơn",
      dataIndex: "orderCount",
      key: "orderCount",
      align: "center",
      render: (value) => value?.toLocaleString("vi-VN") || "0",
    },
    {
      title: "Tổng trọng lượng (kg)",
      dataIndex: "totalWeight",
      key: "totalWeight",
      align: "center",
      render: (value) =>
        value ? Number(value).toFixed(2) : "0.00",
    },
    {
      key: "action",
      align: "center",
      render: (_, record) => (
        <Button
          className="action-button-link"
          type="link"
          onClick={() => onDetail(record.id)}
        >
          DS Đơn hàng
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
import React from 'react';
import { Table, Button, Space, Tag } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { Vehicle } from '../../../../types/vehicle';
import { translateVehicleStatus, translateVehicleType } from '../../../../utils/vehicleUtils';

interface VehicleTableProps {
  data: Vehicle[];
  currentPage: number;
  pageSize: number;
  total: number;
  loading?: boolean;
  onEdit: (vehicle: Vehicle) => void;
  onPageChange: (page: number, pageSize?: number) => void;
}

const VehicleTable: React.FC<VehicleTableProps> = ({
  data,
  currentPage,
  pageSize,
  total,
  loading = false,
  onEdit,
  onPageChange,
}) => {

  const openGoogleMap = (lat?: number, lng?: number) => {
    if (!lat || !lng) return;

    const url = `https://www.google.com/maps?q=${lat},${lng}`;
    window.open(url, "_blank", "noopener,noreferrer");
  };


  const columns: ColumnsType<Vehicle> = [
    {
      title: 'Biển số xe',
      dataIndex: 'licensePlate',
      key: 'licensePlate',
      align: 'left',
      render: (licensePlate, _) => (
        <span className="custom-table-content-strong">
          {licensePlate}
        </span>
      )
    },
    {
      title: 'Thông tin xe',
      dataIndex: 'info',
      key: 'info',
      align: 'left',
      render: (_, record: Vehicle) => (
        <>
          <span className="custom-table-content-strong">Loại xe: </span>{translateVehicleType(record.type)}<br />
          <span className="custom-table-content-strong">Tải trọng: </span>{record.capacity} Kg<br />
          <span className="custom-table-content-strong">Trạng thái: </span>{translateVehicleStatus(record.status)}
        </>
      )
    },
    {
      title: 'Mô tả',
      dataIndex: 'description',
      key: 'description',
      align: 'left',
      render: (desc) => (
        desc
          ? desc
          : <span className="text-muted">N/A</span>
      )
    },
    {
      title: 'Thời gian bảo trì',
      dataIndex: 'createdAt',
      key: 'maintenanceTime',
      align: 'left',
      render: (_, record: Vehicle) => (
        <>
          <div>
            <span className="custom-table-content-strong">Gần nhất: </span>
            {record.lastMaintenanceAt
              ? dayjs(record.lastMaintenanceAt).format('HH:mm:ss DD-MM-YYYY')
              : <span className="text-muted">N/A</span>}
          </div>
          <div>
            <span className="custom-table-content-strong">Tiếp theo: </span>
            {record.nextMaintenanceDue
              ? dayjs(record.nextMaintenanceDue).format('HH:mm:ss DD-MM-YYYY')
              : <span className="text-muted">N/A</span>}
          </div>
        </>
      )
    },
    {
      title: 'Vị trí hiện tại',
      key: 'currentLocation',
      align: 'left',
      render: (_, record: Vehicle) => {
        const { latitude, longitude } = record;

        return (
          <>
            {latitude && longitude ? (
              <>
                <div>
                  {latitude}, {longitude}
                </div>

                <span
                  onClick={() => openGoogleMap(latitude, longitude)}
                  className="navigate-link"
                >
                  Xem trên Google Maps
                </span>
              </>
            ) : (
              <span className="text-muted">N/A</span>
            )}
          </>
        );
      }
    },
    {
      title: 'Mã thiết bị GPS',
      dataIndex: 'gpsDeviceId',
      key: 'gpsDeviceId',
      align: 'left',
    },
    {
      key: 'action',
      align: 'left',
      width: 120,
      render: (_, record: Vehicle) => (
        <Space>
          <Button
            className="action-button-link"
            type="link"
            icon={<EditOutlined />}
            onClick={() => onEdit(record)}
          >
            Sửa
          </Button>
        </Space>
      ),
    },
  ];

  const tableData = data.map((vehicle, index) => ({
    ...vehicle,
    key: String(index + 1 + (currentPage - 1) * pageSize),
  }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        loading={loading}
        pagination={{
          current: currentPage,
          pageSize,
          total,
          onChange: onPageChange,
        }}
        style={{
          borderRadius: 12,
          overflow: 'hidden',
          background: '#fff',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        }}
      />
    </div>
  );
};

export default VehicleTable;
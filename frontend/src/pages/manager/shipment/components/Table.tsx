import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Tooltip, Dropdown } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { translateShipmentStatus, translateShipmentType } from '../../../../utils/shipmentUtils';
import type { ManagerShipment } from '../../../../types/shipment';
import { CloseCircleOutlined, DeleteOutlined, DownOutlined, EditOutlined } from '@ant-design/icons';
import locationApi from '../../../../api/locationApi';

interface TableProps {
  data: ManagerShipment[];
  currentPage: number;
  pageSize: number;
  total: number;
  loading?: boolean;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
  onCancel: (item: ManagerShipment) => void;
  onDetail: (id: number) => void;
  onPageChange: (page: number, pageSize?: number) => void;
}

const RequestTable: React.FC<TableProps> = ({
  data,
  currentPage,
  pageSize,
  total,
  loading = false,
  onEdit,
  onDelete,
  onDetail,
  onCancel,
  onPageChange,
}) => {

  const [formattedAddressMap, setFormattedAddressMap] = useState<Record<string, string>>({});

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

      setFormattedAddressMap((prev) => ({
        ...prev,
        [key]: [detail, wardName, cityName].filter(Boolean).join(", "),
      }));
    } catch (error) {
      setFormattedAddressMap((prev) => ({
        ...prev,
        [key]: detail || "",
      }));
    }
  };

  useEffect(() => {
    data.forEach((item) => {
      if (item.fromOffice) {
        formatAddress(
          `from-${item.id}`,
          item.fromOffice.detail,
          item.fromOffice.wardCode,
          item.fromOffice.cityCode
        );
      }
      if (item.toOffice) {
        formatAddress(
          `to-${item.id}`,
          item.toOffice.detail,
          item.toOffice.wardCode,
          item.toOffice.cityCode
        );
      }
    });
  }, [data]);

  const columns: ColumnsType<ManagerShipment> = [
    {
      title: 'Mã chuyến',
      dataIndex: 'code',
      key: 'code',
      align: 'left',
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
      align: 'left',
      render: (type) => translateShipmentType(type)
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      align: 'left',
      render: (status) => translateShipmentStatus(status)
    },
    {
      title: 'Phương tiện',
      dataIndex: 'vehicle',
      key: 'vehicle',
      align: 'left',
      render: (_, record) => {
        const vehicle = record.vehicle;

        if (!vehicle) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            <span className="custom-table-content-strong">
              Biển số:{" "}
            </span>
            {vehicle.licensePlate}<br />
            <span className="custom-table-content-strong">
              Tải trọng:{" "}
            </span>
            {vehicle.capacity}<br />
          </div>
        );
      }
    },
    {
      title: 'Bưu cục đi',
      key: 'fromOffice',
      align: 'left',
      render: (_, record) => {
        const from = record.fromOffice;

        if (!from) {
          return <span className="text-muted">N/A</span>;
        }

        const fromAddress = formattedAddressMap[`from-${record.id}`];

        return (
          <div>
            <span>
              {from.name}
            </span><br />
            <span>{from.postalCode}</span><br/>

            {from.latitude && from.longitude ? (
              <Tooltip title="Nhấn để mở Google Maps">
                <span
                  className="navigate-link custom-table-content-limit"
                  onClick={() =>
                    window.open(
                      `https://www.google.com/maps?q=${from.latitude},${from.longitude}`,
                      "_blank",
                      "noopener,noreferrer"
                    )
                  }
                >
                  {fromAddress || 'Chưa có địa chỉ'}
                </span>
              </Tooltip>
            ) : (
              <Tooltip title="Địa chỉ không có tọa độ">
                <span>{fromAddress || 'Chưa có địa chỉ'}</span>
              </Tooltip>
            )}
          </div>
        );
      }
    },
    {
      title: 'Bưu cục đến',
      key: 'toOffice',
      align: 'left',
      render: (_, record) => {
        const to = record.toOffice;

        if (!to) {
          return <span className="text-muted">N/A</span>;
        }

        const toAddress = formattedAddressMap[`to-${record.id}`];

        return (
          <div>
            <span>
              {to.name}
            </span><br />
            <span>{to.postalCode}</span><br/>

            {to.latitude && to.longitude ? (
              <Tooltip title="Nhấn để mở Google Maps">
                <span
                  className="navigate-link custom-table-content-limit"
                  onClick={() =>
                    window.open(
                      `https://www.google.com/maps?q=${to.latitude},${to.longitude}`,
                      "_blank",
                      "noopener,noreferrer"
                    )
                  }
                >
                  {toAddress || 'Chưa có địa chỉ'}
                </span>
              </Tooltip>
            ) : (
              <Tooltip title="Địa chỉ không có tọa độ">
                <span>{toAddress || 'Chưa có địa chỉ'}</span>
              </Tooltip>
            )}
          </div>
        );
      }
    },
    {
      title: 'Nhân viên phụ trách',
      dataIndex: 'employee',
      key: 'employee',
      align: 'left',
      render: (_, record) => {
        const employee = record.employee;

        if (!employee) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            <span className="custom-table-content-strong">
              {employee.name} - {employee.code}
            </span><br />
            <span>
              {employee.phoneNumber}
            </span><br />
            <span>
              {employee.email}
            </span>
          </div>
        );
      }
    },
    {
      title: 'Nhân viên tạo chuyến',
      dataIndex: 'createdBy',
      key: 'createdBy',
      align: 'left',
      render: (_, record) => {
        const createdBy = record.createdBy;

        if (!createdBy) {
          return <span className="text-muted">N/A</span>;
        }

        return (
          <div>
            <span className="custom-table-content-strong">
              {createdBy.name} - {createdBy.code}
            </span><br />
            <span>
              {createdBy.phoneNumber}
            </span><br />
            <span>
              {createdBy.email}
            </span>
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
                <span className="custom-table-content-strong">
                  Bắt đầu:
                </span><br />
                {startTime}
              </div>
            )}
            {endTime && (
              <div className="text-extra-time">
                Kết thúc: {endTime}
              </div>
            )}
          </div>
        );
      }
    },
    {
      key: "action",
      align: "left",
      render: (_, record: ManagerShipment) => {
        // const canCancel = canCancelUserShippingRequest(record.status);
        // const canEdit = canEditUserShippingRequest(record.status);
        const canCancel = true;
        const canEdit = true;
        const canDelete = true;

        const items = [
          ...(canEdit
            ? [
              {
                key: "edit",
                icon: <EditOutlined />,
                label: "Sửa",
                onClick: () => onEdit(record.id),
              },
            ]
            : []),

          ...(canCancel
            ? [
              {
                key: "cancel",
                icon: <CloseCircleOutlined />,
                label: "Hủy",
                danger: true,
                onClick: () => onCancel(record),
              },
            ]
            : []),
          ...(canDelete
            ? [
              {
                key: "delete",
                icon: <DeleteOutlined />,
                label: "Xóa",
                onClick: () => onDelete(record.id),
              },
            ]
            : []),
        ];

        return (
          <Space>
            <Button
              className="action-button-link"
              type="link"
              onClick={() => onDetail(record.id)}
            >
              Xem
            </Button>

            <Dropdown menu={{ items }} trigger={["click"]} disabled={items.length === 0}>
              <Button className="dropdown-trigger-button">
                Thêm <DownOutlined />
              </Button>
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  const tableData = data.map((p, index) => ({
    ...p,
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
        pagination={{
          current: currentPage,
          pageSize,
          total,
          onChange: onPageChange,
        }}
        loading={loading}
      />
    </div>
  );
};

export default RequestTable;
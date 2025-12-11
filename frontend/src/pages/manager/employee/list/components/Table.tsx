import React, { useEffect, useState } from 'react';
import { Button, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';
import locationApi from '../../../../../api/locationApi';
import { translateRoleName } from '../../../../../utils/roleUtils';
import { EditOutlined } from '@ant-design/icons';
import type { ManagerEmployee } from '../../../../../types/employee';

interface EmployeeTableProps {
  data: ManagerEmployee[];
  page: number;
  limit: number;
  total: number;
  loading: boolean;
  onEdit: (employee: ManagerEmployee) => void;
  onPageChange: (page: number, limit?: number) => void;
}

const EmployeeTable: React.FC<EmployeeTableProps> = ({
  data,
  page,
  limit,
  total,
  loading,
  onEdit,
  onPageChange,
}) => {

  const [locationMap, setLocationMap] = useState<Record<number, { city: string, ward: string }>>({});

  useEffect(() => {
    const fetchLocations = async () => {
      const map: Record<number, { city: string; ward: string }> = {};
      for (const employee of data) {
        const cityName = (await locationApi.getCityNameByCode(employee.userCityCode)) || "Unknown";
        const wardName = (await locationApi.getWardNameByCode(employee.userCityCode, employee.userWardCode)) || "Unknown";
        map[employee.id!] = { city: cityName, ward: wardName };
      }
      setLocationMap(map);
    };
    fetchLocations();
  }, [data]);

  const columns: ColumnsType<ManagerEmployee> = [
    {
      title: "Mã NV",
      dataIndex: "code",
      key: "code",
      align: "center",
      render: (code,_) => <span className="custom-table-content-strong">{code}</span>,
    },
    {
      title: "Thông tin cá nhân",
      key: "personalInfo",
      align: "left",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Họ tên: </span>{record.userFullName}<br />
          <span className="custom-table-content-strong">Email: </span>{record.userEmail}<br />
          <span className="custom-table-content-strong">SĐT: </span>{record.userPhoneNumber}
        </>
      ),
    },
    {
      title: "Địa chỉ",
      key: "address",
      align: "left",
      render: (_, record) => {
        const location = locationMap[record.id];
        const hasAddress = record.userDetail && location?.city && location?.ward;
        return hasAddress ?
          <span className='custom-table-content-limit'>
            {record.userDetail}, {location.ward}, {location.city}
          </span>
          : <span className='text-muted'>
            N/A
          </span>;
      },
    },
    {
      title: "Thông tin công việc",
      key: "jobInfo",
      align: "left",
      render: (_, record) => (
        <>
          <span className="custom-table-content-strong">Chức vụ: </span>{translateRoleName(record.userRole) || "-"}<br />
          <span className="custom-table-content-strong">Ca làm: </span>{translateEmployeeShift(record.shift)}<br />
          <span className="custom-table-content-strong">Trạng thái: </span>{translateEmployeeStatus(record.status)}
        </>
      ),
    },
    {
      title: "Ngày vào làm",
      key: "hireDate",
      align: "center",
      render: (_, record) => dayjs(record.hireDate).format("YYYY-MM-DD"),
    },
    {
      title: "Hành động",
      key: "action",
      align: "center",
      render: (_, record) => (
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

  const tableData = data.map((p, index) => ({
    ...p,
    key: String(index + 1 + (page - 1) * limit),
  }));

  return (
    <div className="table-container">
      <Table
        columns={columns}
        dataSource={tableData}
        rowKey="key"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        loading={loading}
        pagination={{
          current: page,
          pageSize: limit,
          total,
          onChange: (page, pageSize) => onPageChange(page, pageSize)
        }}
      />
    </div>
  );
};

export default EmployeeTable;